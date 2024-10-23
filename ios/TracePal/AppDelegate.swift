import UIKit
import CoreLocation
import Firebase
import FirebaseMessaging

@UIApplicationMain
class AppDelegate: UIResponder, UIApplicationDelegate, CLLocationManagerDelegate {

	// Firebase
	var window: UIWindow?
	var firebaseData: [AnyHashable: Any]?
	var isAnswer: Bool?
	var locationManager: CLLocationManager!
	var locationToken: String = ""
	var locationTimestamp: Int = 0

	/*
	 Initilise Firebase
	 */
	func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
		// Use Firebase library to configure APIs
		FirebaseApp.configure()

		// Delegate firebase messaging
		Messaging.messaging().delegate = self

		// Register for remote notifications. This shows a permission dialog on first run, to
		// show the dialog at a more appropriate time move this registration accordingly.
		if #available(iOS 10.0, *) {
			// For iOS 10 display notification (sent via APNS)
			UNUserNotificationCenter.current().delegate = self

			let authOptions: UNAuthorizationOptions = [.alert, .badge, .sound]
			UNUserNotificationCenter.current().requestAuthorization(
				options: authOptions,
				completionHandler: { _, _ in }
			)
		} else {
			let settings: UIUserNotificationSettings =
				UIUserNotificationSettings(types: [.alert, .badge, .sound], categories: nil)
			application.registerUserNotificationSettings(settings)
		}

		application.registerForRemoteNotifications()

		// Override point for customization after application launch.
		return true
	}
	
	// Start location tracking in background/terminated
	func startLocation(token: String) {
		locationToken = token
		
		// Init location
		locationManager = CLLocationManager()
		locationManager.delegate = self
		locationManager.desiredAccuracy = kCLLocationAccuracyBest
		locationManager.distanceFilter = CLLocationDistance(Constant.locationDistance)
		locationManager.allowsBackgroundLocationUpdates = true
		locationManager.pausesLocationUpdatesAutomatically = false
		// Check for significant location changes, including when app is terminated
		locationManager.startMonitoringSignificantLocationChanges()
	}

	// Stop location tracking in background/terminated
	func stopLocation() {
		// Stop checking for significant location changes
		locationToken = ""
		if locationManager != nil {
			locationManager.stopMonitoringSignificantLocationChanges()
		}
	}
	
	// Handle location update in background or terminated state
	func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
		if let location = locations.last {
			// Get the latitude, longitude, and accuracy
			let latitude = (String)(location.coordinate.latitude)
			let longitude = (String)(location.coordinate.longitude)
			let accuracy = (String)(location.horizontalAccuracy)

			// Send the location to the server
			sendLocation(latitude: latitude, longitude: longitude, accuracy: accuracy)
		}
	}

	// Send location data to server
	func sendLocation(latitude: String, longitude: String, accuracy: String) {
		if locationToken.isEmpty {
			return
		}
		
		// Avoid HTTP flooding
		let now = Int(Date().timeIntervalSince1970)
		// Accessing AppDelegate
		let appDelegate = UIApplication.shared.delegate as! AppDelegate
		if (appDelegate.locationTimestamp > now - Constant.locationInterval) {
			return
		}
		// Update location timestamp
		appDelegate.locationTimestamp = now
		
		let url = URL(string: Constant.AppApi)!
		var request = URLRequest(url: url)
		request.httpMethod = "POST"
		// Create URLComponents to handle the URL-encoded parameters
		var urlComponents = URLComponents()
		urlComponents.queryItems = [
			URLQueryItem(name: "webhooks", value: "locate"),
			URLQueryItem(name: "token", value: locationToken),
			URLQueryItem(name: "lat", value: latitude),
			URLQueryItem(name: "long", value: longitude),
			URLQueryItem(name: "accuracy", value: accuracy)
		]

		// Convert the query items to a URL-encoded data string
		let postData = urlComponents.query?.data(using: .utf8)
		// Set the request body with the URL-encoded data
		request.httpBody = postData
		// Set content type to URL-encoded form
		request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")

		let task = URLSession.shared.dataTask(with: request) { data, response, error in
			if let error = error {
				print("Error sending location: \(error.localizedDescription)")
				return
			}
		}

		task.resume()
	}

	/*
	 Firebase notifications
	 This method is not called when swizzling is enabled (default) becasue notifications are intercepted by the AppDelegate
	 If you are receiving a notification message while your app is in the background
	 This callback will not be fired till the user taps on the notification launching the application
	 https://firebase.google.com/docs/cloud-messaging/ios/receive
	 */
	func application(_ application: UIApplication,
		didReceiveRemoteNotification userInfo: [AnyHashable: Any]) {
		// Log data
		if(Constant.Log) {
			print("[didReceiveRemoteNotification]")
			print(userInfo)
		}
	}

	/*
	 Firebase data notifications
	 This method is called when "content-available" = 1 is passed while the app is either in background or foreground
	 */
	func application(_ application: UIApplication,
		didReceiveRemoteNotification userInfo: [AnyHashable: Any],
		fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void) {
		// With swizzling disabled you must let Messaging know about the message, for Analytics
		// Messaging.messaging().appDidReceiveMessage(userInfo)

		// Set data
		firebaseData = userInfo

		// Log data
		if(Constant.Log) {
			print("[didReceiveRemoteNotification][fetchCompletionHandler]")
			print(firebaseData as Any)
		}
		// Show local notification for a call
		// N.B. local notifications show up only when the app is in background
		// N.B. manually unwrap jsonString to avoid runtime nil issues
		if(userInfo["type"] != nil && userInfo["type"] as! String == "webRtc"
				&& userInfo["title"] != nil && userInfo["body"] != nil) {
			callNotification(title: userInfo["title"] as! String, body: userInfo["body"] as! String)
		} else {
			// Post notification either if the app is in background or foreground
			NotificationCenter.default.post(name: NSNotification.Name(rawValue: Constant.notificationForeground), object: nil, userInfo: userInfo)

			// Bring the app in foreground
			completionHandler(UIBackgroundFetchResult.newData)
		}
	}

	/*
	 Handle Firebase registration error
	 */
	func application(_ application: UIApplication,
		didFailToRegisterForRemoteNotificationsWithError error: Error) {
		// Log error
		print("[didFailToRegisterForRemoteNotificationsWithError] Unable to register for remote notifications: \(error.localizedDescription)")
	}

	// MARK: UISceneSession Lifecycle
	func application(_ application: UIApplication, configurationForConnecting connectingSceneSession: UISceneSession, options: UIScene.ConnectionOptions) -> UISceneConfiguration {
		// Called when a new scene session is being created.
		// Use this method to select a configuration to create the new scene with.
		return UISceneConfiguration(name: "Default Configuration", sessionRole: connectingSceneSession.role)
	}

	func application(_ application: UIApplication, didDiscardSceneSessions sceneSessions: Set<UISceneSession>) {
		// Called when the user discards a scene session.
		// If any sessions were discarded while the application was not running, this will be called shortly after application:didFinishLaunchingWithOptions.
		// Use this method to release any resources that were specific to the discarded scenes, as they will not return.
	}

	/*
	 Handle the URLscheme
	 Must be implemented in both SceneDelegate and AppDelegate
	 different iOS devices/simulator could access either
	 */
	func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey: Any] = [:]) -> Bool {

		if let scheme = url.scheme,
			scheme.caseInsensitiveCompare(Constant.redirectUrlScheme) == .orderedSame {
			URLComponents(url: url, resolvingAgainstBaseURL: false)?.queryItems?.forEach {
				if($0.name == Constant.redirectUrlParam) {
					UserDefaults(suiteName: Constant.shareGroup)?.set($0.value, forKey: Constant.redirectUrl)
				}
			}
		}

		return true
	}

	/*
	 Create and display a call notification
	 */
	func callNotification(title: String, body: String, subtitle: String = "", delayInterval: Double = 0.1) -> Void {
		// Clear all notifications
		UNUserNotificationCenter.current().removeAllPendingNotificationRequests()

		let callReject = UNNotificationAction(identifier: Constant.callReject, title: Lang.get(key: "reject"), options: [.destructive])
		let callAnswer = UNNotificationAction(identifier: Constant.callAnswer, title: Lang.get(key: "answer"), options: [.foreground])
		// Define the notification type
		let notificationCategory =
			UNNotificationCategory(identifier: Constant.callCategory, actions: [callReject, callAnswer], intentIdentifiers: [], hiddenPreviewsBodyPlaceholder: "")
		// Register the notification type.
		let notificationCenter = UNUserNotificationCenter.current()
		notificationCenter.setNotificationCategories([notificationCategory])

		// Create Notification Content
		let notificationContent = UNMutableNotificationContent()

		// Configure Notification Content
		notificationContent.title = title
		notificationContent.body = body
		notificationContent.sound = UNNotificationSound(named: UNNotificationSoundName(rawValue: "ring.receiver.mp3"));
		notificationContent.categoryIdentifier = Constant.callCategory;
		if(subtitle != "") {
			// optional
			notificationContent.subtitle = subtitle
		}

		// Add Trigger, cannot be triggered immediatelly NOW + 0 but a minimum time must elaps NOW + 0.1
		let notificationTrigger = UNTimeIntervalNotificationTrigger(timeInterval: delayInterval, repeats: false)

		// Create Notification Request
		let notificationRequest = UNNotificationRequest(identifier: UUID().uuidString, content: notificationContent, trigger: notificationTrigger)

		// Add Request to User Notification Center
		UNUserNotificationCenter.current().add(notificationRequest) { (error) in
			if let error = error {
				print("[callNotification] \(error): \(error.localizedDescription)")
			}
		}
	}
}

@available(iOS 10, *)
extension AppDelegate: UNUserNotificationCenterDelegate {

	/*
	 Handle Firebase notification in Foreground
	 Silent the notification, handle the data via the javascript call
	 For iOS 10 devices.
	 */
	func userNotificationCenter(_ center: UNUserNotificationCenter,
		willPresent notification: UNNotification,
		withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions)
			-> Void) {
		// Get data
		let userInfo = notification.request.content.userInfo

		// Set data
		firebaseData = userInfo.isEmpty ? firebaseData : userInfo

		// Log data
		if(Constant.Log) {
			print("[userNotificationCenter][willPresent]")
			print(firebaseData as Any)
		}

		// Post data to the app that is already in foreground
		NotificationCenter.default.post(name: NSNotification.Name(rawValue: Constant.notificationForeground), object: nil, userInfo: userInfo)
		// Do not show the notification on screen becasue the app is already in foreground
		//completionHandler([[.alert, .sound]])
	}

	/*
	 Handle Firebase notification on click
	 The click is registered despite the app being in foreground or background
	 N.B. This is never called by destructive actions, such as Constant.callReject
	 */
	func userNotificationCenter(_ center: UNUserNotificationCenter,
		didReceive response: UNNotificationResponse,
		withCompletionHandler completionHandler: @escaping () -> Void) {
		// Get data
		let userInfo = response.notification.request.content.userInfo

		// Set data
		firebaseData = userInfo.isEmpty ? firebaseData : userInfo

		// Log data
		if(Constant.Log) {
			print("[userNotificationCenter][didReceive]")
			print(firebaseData as Any)
		}

		// Handle callback action
		switch response.actionIdentifier {
		case Constant.callAnswer:
			// Add answer fallthrough
			if (!firebaseData!.isEmpty) {
				firebaseData!["isAnswer"] = 1
			}
			break
		default:
			// Do nothing
			break
		}

		// Bring the app in foreground
		completionHandler()
	}
}

/*
 Firebase delegate for registering token
 */
extension AppDelegate: MessagingDelegate {
	func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
		// Log data
		if(Constant.Log) {
			print("[AppDelegate] Notification token: \(String(describing: fcmToken))")
		}

		let dataDict: [String: String] = ["token": fcmToken ?? ""]
		NotificationCenter.default.post(
			name: Notification.Name("FCMToken"),
			object: nil,
			userInfo: dataDict
		)

		// N.B. Token is fetched by the app rather than pushed to it, via a javascript hook
	}
}

