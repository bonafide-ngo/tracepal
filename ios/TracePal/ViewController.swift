import UIKit
import WebKit
import Foundation
import FirebaseMessaging
import MessageUI
import CallKit
import CoreLocation

class ViewController: UIViewController, WKUIDelegate, WKNavigationDelegate, WKScriptMessageHandler, MFMailComposeViewControllerDelegate, CXCallObserverDelegate, CLLocationManagerDelegate {

	var webView: WKWebView!

	var firebaseToken: String!
	var firebaseObserver: NSObjectProtocol?
	var firebaseData: [AnyHashable: Any]?
	var firebaseNotification_LoopCount = 0
	var appDomain = URL(string: Constant.AppUrl)!.host!
	var locationManager: CLLocationManager!
	var locationToken: String = ""
	let callObserver = CXCallObserver()

	deinit {
		NotificationCenter.default.removeObserver(self)
	}

	/*
	 * Detecting a telephone call with CallKit
	 * https://developer.onepagecrm.com/blog/2018/05/24/ios-detect-call/
	 * https://medium.com/@adi.mizrahi/swift-callkit-and-how-we-use-it-2df1735f9c6d
	 */
	func callObserver(_ callObserver: CXCallObserver, callChanged call: CXCall) {
		func killSwitch() {
			// Handle notification
			self.webView.evaluateJavaScript(Constant.killSwitchHandler + "();") { (any, error) in
				if error != nil {
					// Toast error
					self.toastError(message: "[" + Constant.killSwitchHandler + "]: \(error.debugDescription)")
				}
			}
		}

		if call.isOutgoing == true && call.hasConnected == false && call.hasEnded == false {
			//.. 1. detect a dialing outgoing call
		} else if call.isOutgoing == true && call.hasConnected == true && call.hasEnded == false {
			//.. 2. outgoing call in process
			killSwitch()
		} else if call.isOutgoing == false && call.hasConnected == false && call.hasEnded == false {
			//.. 3. incoming call ringing (not answered)
		} else if call.isOutgoing == false && call.hasConnected == true && call.hasEnded == false {
			//.. 4. incoming call in process
			killSwitch()
		} else if call.isOutgoing == true && call.hasEnded == true {
			//.. 5. outgoing call ended.
		} else if call.isOutgoing == false && call.hasEnded == true {
			//.. 6. incoming call ended.
		} else if call.hasConnected == true && call.hasEnded == false && call.isOnHold == false {
			//.. 7. call connected (either outgoing or incoming)
			killSwitch()
		} else if call.isOutgoing == true && call.isOnHold == true {
			//.. 8. outgoing call is on hold
		} else if call.isOutgoing == false && call.isOnHold == true {
			//.. 9. incoming call is on hold
		}
	}

	/*
	 Call the firebase notification via the javascript handler
	 */
	@objc func runFirebaseNotification() {
		if(Constant.Log) {
			print("runFirebaseNotification")
		}

		//Get the notification data
		let appDelegate = UIApplication.shared.delegate as! AppDelegate
		firebaseData = appDelegate.firebaseData

		if(firebaseData != nil) {
			// Convert to json string
			let jsonData = try! JSONSerialization.data(withJSONObject: firebaseData as Any, options: [])
			let jsonString = String(data: jsonData, encoding: .utf8)!

			if(Constant.Log) {
				print("jsonString:" + jsonString)
			}

			// Handle notification
			self.webView.evaluateJavaScript(Constant.firebaseHandler + "('\(jsonString)');") { (any, error) in
				if error != nil {
					if self.firebaseNotification_LoopCount < Constant.notificationMaxLoops {
						// Increase loop count
						self.firebaseNotification_LoopCount += 1
						// Wait for the JS namespace to load (app was probably closed)
						self.perform(#selector(self.runFirebaseNotification), with: nil, afterDelay: 2.0)
					} else {
						// Reset loop count
						self.firebaseNotification_LoopCount = 0
						// Toast error
						self.toastError(message: "[" + Constant.firebaseHandler + "]: \(error.debugDescription)")
					}
				} else {
					// Reset loop count
					self.firebaseNotification_LoopCount = 0
				}
			}
		}
	}

	/*
	 Set up the notification listener
	 */
	func setupNotification() {
		NotificationCenter.default.addObserver(
			self,
			selector: #selector(runNotification),
			name: UIApplication.didBecomeActiveNotification,
			object: nil
		)

		// Notification has moved the app from background to foreground
		NotificationCenter.default.addObserver(
			self,
			selector: #selector(runFirebaseNotification),
			name: UIApplication.willEnterForegroundNotification,
			object: nil)

		// Notification triggered when the app is already in foreground
		NotificationCenter.default.addObserver(
			self,
			selector: #selector(runFirebaseNotification),
			name: NSNotification.Name(rawValue: Constant.notificationForeground),
			object: nil)
	}

	override func viewDidAppear(_ animated: Bool) {
		super.viewDidAppear(animated)
	}

	/*
	 Run a notification either for sharing or redirecting
	 */
	@objc func runNotification() {
		let shareText = UserDefaults(suiteName: Constant.shareGroup)?.value(forKey: Constant.shareIncomingText) as? String
		let shareFile = UserDefaults(suiteName: Constant.shareGroup)?.value(forKey: Constant.shareIncomingFile) as? String
		let shareFiles = UserDefaults(suiteName: Constant.shareGroup)?.value(forKey: Constant.shareIncomingFiles) as? String
		let redirectUrl = UserDefaults(suiteName: Constant.shareGroup)?.value(forKey: Constant.redirectUrl) as? String

		if(redirectUrl != nil) {
			// Load url in app
			let myURL = URL(string: redirectUrl!)
			let myRequest = URLRequest(url: myURL!)
			self.webView.load(myRequest)

			UserDefaults(suiteName: Constant.shareGroup)?.removeObject(forKey: Constant.redirectUrl)
		}
		else if(shareText != nil) {

			// Handle notification
			self.webView.evaluateJavaScript(Constant.shareTextHandler + "('\(shareText ?? "")');") { (any, error) in
				if error != nil {
					// Toast error
					self.toastError(message: "[" + Constant.shareTextHandler + "]: \(error.debugDescription)")
				}
			}

			UserDefaults(suiteName: Constant.shareGroup)?.removeObject(forKey: Constant.shareIncomingText)
		}
		else if(shareFile != nil) {

			// Handle notification
			self.webView.evaluateJavaScript(Constant.shareFileHandler + "('\(shareFile ?? "")');") { (any, error) in
				if error != nil {
					// Toast error
					self.toastError(message: "[" + Constant.shareFileHandler + "]: \(error.debugDescription)")
				}
			}

			UserDefaults(suiteName: Constant.shareGroup)?.removeObject(forKey: Constant.shareIncomingFile)
		}
		else if(shareFiles != nil) {

			// Handle notification
			self.webView.evaluateJavaScript(Constant.shareFilesHandler + "('\(shareFiles ?? "")');") { (any, error) in
				if error != nil {
					// Toast error
					self.toastError(message: "[" + Constant.shareFilesHandler + "]: \(error.debugDescription)")
				}
			}

			UserDefaults(suiteName: Constant.shareGroup)?.removeObject(forKey: Constant.shareIncomingFiles)
		}
	}

	// Update location
	func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
		if let location = locations.last {
			// Get the latitude, longitude, and accuracy
			let latitude = (String)(location.coordinate.latitude)
			let longitude = (String)(location.coordinate.longitude)
			let accuracy = (String)(location.horizontalAccuracy)

			// Send location via HTTP request
			sendLocation(latitude: latitude, longitude: longitude, accuracy: accuracy)
		}
	}

	// Handle location errors
	func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
		print("Location Manager Error: \(error.localizedDescription)")
	}

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

	override func loadView() {
		// Clear chache on debug
		if Constant.Debug {
			WebCacheCleaner.clear()
		}

		// Configure javascript hooks
		let userContentController = WKUserContentController();
		userContentController.add(self, name: Constant.hookDownload)
		userContentController.add(self, name: Constant.hookSyncCookies)
		userContentController.add(self, name: Constant.hookFirebaseToken)
		userContentController.add(self, name: Constant.hookStartTracker)
		userContentController.add(self, name: Constant.hookStopTracker)

		// Configure preferences
		let preferences = WKPreferences()
		preferences.javaScriptCanOpenWindowsAutomatically = true
		// Disable Google Safe Browsing
		preferences.isFraudulentWebsiteWarningEnabled = false

		// Configure webpage preferences
		let webPagePreferences = WKWebpagePreferences()
		webPagePreferences.allowsContentJavaScript = true

		// Set configuration
		let webConfiguration = WKWebViewConfiguration()
		webConfiguration.preferences = preferences
		webConfiguration.userContentController = userContentController
		webConfiguration.defaultWebpagePreferences = webPagePreferences
		webConfiguration.mediaTypesRequiringUserActionForPlayback = []
		webConfiguration.allowsInlineMediaPlayback = true
		webConfiguration.websiteDataStore = WKWebsiteDataStore.default()
		webConfiguration.processPool = WKProcessPool()
		// Set userAgent
		webConfiguration.applicationNameForUserAgent = "iOS/" + Constant.AppName + "/" + (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as! String)

		// Init webview
		webView = WKWebView(frame: .zero, configuration: webConfiguration)
		webView.uiDelegate = self
		webView.navigationDelegate = self

		// Allow gesture control
		webView.allowsBackForwardNavigationGestures = true

		// Set matching webview background colour
		webView.isOpaque = false;
		webView.backgroundColor = UIColor(red: 108/255, green: 195/255, blue: 213/255, alpha: 1.0)

		// Apply webview to view
		view = webView
	}

	override func viewDidLoad() {
		super.viewDidLoad()

		// Handle notifications
		setupNotification()

		// Unarchive and load cookies
		webView.unarchiveCookies(for: appDomain) {
			// Parse app url
			let myURL = URL(string: Constant.AppUrl)!
			let myRequest = URLRequest(url: myURL)
			// Load app url
			self.webView.load(myRequest)
		}

		// Detect telephone call
		callObserver.setDelegate(self, queue: nil)
	}

	func webView(_ webView: WKWebView, didFinish navigation: WKNavigation!) {
		// Do something when the page has finished loading, like evaluating javascript
	}

	// Handle javascript hooks
	func userContentController(_ userContentController: WKUserContentController, didReceive message: WKScriptMessage) {
		switch message.name {
		case Constant.hookStartTracker:
			// Sync with JS parameters
			let body = message.body as! NSDictionary
			let token = body["token"] as! String

			if !token.isEmpty {
				locationToken = token;

				// Init location
				locationManager = CLLocationManager()
				locationManager.delegate = self
				locationManager.desiredAccuracy = kCLLocationAccuracyBest
				locationManager.distanceFilter = CLLocationDistance(Constant.locationDistance)
				locationManager.requestAlwaysAuthorization()
				locationManager.allowsBackgroundLocationUpdates = true
				locationManager.pausesLocationUpdatesAutomatically = false
				// Start updating location if permission is granted
				if CLLocationManager.locationServicesEnabled() {
					// Track location when in foreground or background
					locationManager.startUpdatingLocation()
					// Accessing AppDelegate
					let appDelegate = UIApplication.shared.delegate as! AppDelegate
					appDelegate.startLocation(token: locationToken)
				}

			}

			// Debug token
			if(Constant.Log) {
				print(token)
			}
			break
		case Constant.hookStopTracker:
			locationToken = "";

			// Stop location tracking
			if locationManager != nil {
				locationManager.stopUpdatingLocation()
			}
			// Accessing AppDelegate
			let appDelegate = UIApplication.shared.delegate as! AppDelegate
			appDelegate.stopLocation()
			break
		case Constant.hookFirebaseToken:
			firebaseToken = Messaging.messaging().fcmToken

			// Log data
			if(Constant.Log) {
				print("FCM token: \(firebaseToken ?? "")")
				print("FCM update token with remote server")
			}

			// Refresh token
			Messaging.messaging().token { token, error in
				if let error = error {
					// Toast error
					self.toastError(message: "Cannot fetch remote registration token: \(error)")
				} else if let firebaseToken = token {
					// Sync token with remote server
					self.webView.evaluateJavaScript(Constant.hookFirebaseToken_callback + "('\(firebaseToken)');") { (any, error) in
						if error != nil {
							// Toast error
							self.toastError(message: "[" + Constant.hookFirebaseToken_callback + "]: \(error.debugDescription)")
						}
					}
				}
			}
			break
		case Constant.hookSyncCookies:
			// Archive cookies
			webView.archiveCookies(for: appDomain) {
				// Silent callback
			}
			break
		case Constant.hookDownload:
			// Sync with JS parameters
			let body = message.body as! NSDictionary
			let filename = body["filename"] as! String
			let fileBase64 = body["fileBase64"] as! String
			let message = body["message"] as! String

			// Extract pure base64 body part
			let base64Parts = fileBase64.components(separatedBy: ";base64,")
			let base64 = base64Parts[1]

			// Convert base64 to data
			guard
			var documentsURL = (FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)).last,
				let convertedData = Data(base64Encoded: base64)
				else {
				// Toast error
				self.toastError(message: "[hookDownload]: base64 conversion failed")
				return
			}

			// Set target filename
			documentsURL.appendPathComponent(filename)

			do {
				// Save into documents
				try convertedData.write(to: documentsURL)
			} catch {
				// Toast error
				self.toastError(message: "[hookDownload]: cannot save into documents")
			}

			// Debug location
			if(Constant.Log) {
				print(documentsURL)
			}

			// Toast success message
			let toast = UIAlertController(title: nil, message: message, preferredStyle: .alert)
			self.present(toast, animated: true, completion: nil)
			Timer.scheduledTimer(withTimeInterval: 5.0, repeats: false, block: { _ in toast.dismiss(animated: true, completion: nil) })
			break
		default:
			// Toast error
			self.toastError(message: "[userContentController]: undefined")
			break
		}
	}

	// Handle mail on completion
	func mailComposeController(_ controller: MFMailComposeViewController, didFinishWith result: MFMailComposeResult, error: Error?) {
		// Dismiss the mail compose view controller.
		controller.dismiss(animated: true, completion: nil)
	}

	// Avoid multiple media permission prompts
	// https://stackoverflow.com/questions/66363074/ios-wkwebview-always-allow-camera-permission
	func webView(_ webView: WKWebView, requestMediaCapturePermissionFor origin: WKSecurityOrigin, initiatedByFrame frame: WKFrameInfo, type: WKMediaCaptureType, decisionHandler: @escaping (WKPermissionDecision) -> Void) {
		decisionHandler(.grant)
	}

	// Delegate navigation for in-app domains or open with safari externally
	func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {

		let url = navigationAction.request.url
		guard url != nil else {
			decisionHandler(.allow)
			return
		}

		let isTargetBlank = !(navigationAction.targetFrame?.isMainFrame ?? false)
		let domain = url?.host
		let scheme = url?.scheme

		// Handle send email
		if scheme == "mailto" {
			decisionHandler(.cancel)

			let email = String((url!.absoluteString).dropFirst(7))
			let composeVC = MFMailComposeViewController()
			composeVC.mailComposeDelegate = self
			composeVC.setToRecipients([email])

			if MFMailComposeViewController.canSendMail() {
				// Present the view controller modally.
				self.present(composeVC, animated: true, completion: nil)
			} else {
				// Toast error
				self.toastError(message: Lang.get(key: "error_email"))
			}
		} else if (domain == nil || domain == appDomain || !isTargetBlank) {
			// Handle in-app domains or when target in not _blank
			decisionHandler(.allow)
		} else {
			// Handle other domains
			decisionHandler(.cancel)
			if(UIApplication.shared.canOpenURL(url!)) {
				UIApplication.shared.open(url!)
			}
		}
	}

	// Enforce light theme for status bar
	override var preferredStatusBarStyle: UIStatusBarStyle { .lightContent }

	/*
	 * Toast an error to the user
	 */
	func toastError(message: String, title: String = Lang.get(key: "error_ios")) -> Void {
		print(title + ": " + message)

		// Init toast error message
		let toast = UIAlertController(title: title, message: message, preferredStyle: .alert)
		self.present(toast, animated: true, completion: nil)
		Timer.scheduledTimer(withTimeInterval: 5.0, repeats: false, block: { _ in toast.dismiss(animated: true, completion: nil) })
	}
}

/*
 * Extension to handle cookies
 */
extension WKWebView {

	enum PrefKey {
		// Prefix for storing cookies in USerDefaults
		static let cookie = "cookies."
	}

	// Archive cookies in UserDefaults to be retrieved when app is killed
	func archiveCookies(for domain: String, completion: @escaping () -> ()) {
		getCookies(for: domain) { data in
			// Store for later
			UserDefaults.standard.setValue(data, forKey: PrefKey.cookie + domain)
			print("[archiveCookies]", data)
			completion();
		}
	}

	// Unarchive and restore cookies on app load
	func unarchiveCookies(for domain: String, completion: @escaping () -> ()) {
		// Get the cookies stored in the UserDefaults
		if let unarchivedCookie = UserDefaults.standard.dictionary(forKey: (PrefKey.cookie + domain)) {
			// Get present cookies if any
			getCookies(for: domain) { freshCookie in
				// Merge cookies for consistency
				let mergedCookie = unarchivedCookie.merging(freshCookie) { (_, new) in new }

				// Restore each cookie individually_
				for (_, cookieConfig) in mergedCookie {
					let cookie = cookieConfig as! Dictionary<String, Any>

					var expire: Any? = nil
					if let expireTime = cookie["Expires"] as? Double {
						expire = Date(timeIntervalSinceNow: expireTime)
					}

					let newCookie = HTTPCookie(properties: [
							.name: cookie["Name"] as Any,
							.value: cookie["Value"] as Any,
							.domain: cookie["Domain"] as Any,
							.path: cookie["Path"] as Any,
							.secure: cookie["Secure"] as Any,
							.expires: expire as Any
						])

					// Set or override cookie
					self.configuration.websiteDataStore.httpCookieStore.setCookie(newCookie!)
				}
				completion()
			}
		} else {
			completion()
		}
	}

	// Get all present cookies from the app
	func getCookies(for domain: String, completion: @escaping ([String: Any]) -> ()) {
		var cookieDict = [String: AnyObject]()
		WKWebsiteDataStore.default().httpCookieStore.getAllCookies { (cookies) in
			for cookie in cookies {
				if cookie.domain.contains(domain) {
					cookieDict[cookie.name] = cookie.properties as AnyObject?
				}
			}
			completion(cookieDict)
		}
	}
}

/*
 * Clean cache, cookies, datastore
 */
final class WebCacheCleaner {

	class func clear() {
		print("[WebCacheCleaner] All cached requests deleted")
		URLCache.shared.removeAllCachedResponses()

		HTTPCookieStorage.shared.removeCookies(since: Date.distantPast)
		print("[WebCacheCleaner] All cookies deleted")

		WKWebsiteDataStore.default().fetchDataRecords(ofTypes: WKWebsiteDataStore.allWebsiteDataTypes()) { records in
			records.forEach { record in
				WKWebsiteDataStore.default().removeData(ofTypes: record.dataTypes, for: [record], completionHandler: { })
				print("[WebCacheCleaner] \(record) DataStore deleted")
			}
		}
	}

}
