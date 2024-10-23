import Social
import CoreServices
import UniformTypeIdentifiers

class ShareViewController: UIViewController {
	
	private let typeText = UTType.plainText.identifier
	private let typeURL = UTType.url.identifier
	
	/*
	Handle the call to share
	*/
	override func viewDidAppear(_ animated: Bool) {
		super.viewDidAppear(animated)
		
		// Get the all encompasing object that holds whatever was shared. If not, dismiss view.
		guard let extensionItem = extensionContext?.inputItems.first as? NSExtensionItem,
			  let itemProvider = extensionItem.attachments?.first else {
			self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
			return
		}
		
		// Check if object is of type text
		if itemProvider.hasItemConformingToTypeIdentifier(typeText) {
			handleIncomingText(itemProvider: itemProvider)
		}
		// Check if object is of type URL
		else if itemProvider.hasItemConformingToTypeIdentifier(typeURL) {
			handleIncomingURL(itemProvider: itemProvider)
		}
		// Check if object is any data
		else if itemProvider.hasItemConformingToTypeIdentifier(UTType.data.identifier) {
			// Check if sharing one or multiple items
			if extensionItem.attachments?.count == 1 {
				handleIncomingFile(itemProvider: itemProvider)
			}
			else {
				handleIncomingFiles(itemProviders: extensionItem.attachments!)
			}
		} else {
			print("Share error: content not supported")
			self.extensionContext?.completeRequest(returningItems: nil, completionHandler: nil)
		}
	}
	
	/*
	Handle text to share
	*/
	private func handleIncomingText(itemProvider: NSItemProvider) {
		itemProvider.loadItem(forTypeIdentifier: typeText, options: nil) { (item, error) in
			if let error = error {
				print("Share text error: \(error.localizedDescription)")
			}
			
			if let textString = item as? String {
				UserDefaults(suiteName: Constant.shareGroup)?.set(textString, forKey: Constant.shareIncomingText);
				self.openMainApp()
			}
		}
	}
	
	/*
	Handle URL to share, same as text but different validation
	*/
	private func handleIncomingURL(itemProvider: NSItemProvider) {
		itemProvider.loadItem(forTypeIdentifier: typeURL, options: nil) { (item, error) in
			if let error = error {
				print("Share URL error: \(error.localizedDescription)")
			}
			
			// Cast url to NSURL
			let url = item as? NSURL
			// Get url string
			let urlString = url?.absoluteString
			// Get scheme
			let scheme = url?.scheme
			// Check if the URL is pointing to a local resourse
			if(scheme?.caseInsensitiveCompare("File") == .orderedSame) {
				self.handleIncomingFile(itemProvider: itemProvider)
			} else if urlString != nil {
				UserDefaults(suiteName: Constant.shareGroup)?.set(urlString, forKey: Constant.shareIncomingText)
				self.openMainApp()
			}
		}
	}
	
	/*
	Handle file to share
	*/
	private func handleIncomingFile(itemProvider: NSItemProvider) {
		itemProvider.loadItem(forTypeIdentifier: UTType.data.identifier, options: nil) { (item, error) in
			if let error = error {
				print("Share file error: \(error.localizedDescription)")
			}
			// Convert to url
			let url = item as! NSURL
			// Get mimetype
			let fileType = self.mimeTypeForPath(url)
			// Get name
			let fileName = url.lastPathComponent
			// Get size
			var fileSize = 0;
			do {
				fileSize = try (url.resourceValues(forKeys:[.fileSizeKey]).values.first as! Int)
			} catch {
				print("Share file error: \(error)")
			}
			
			// Get data and convert it to base64 string
			let fileBase64 = NSData(contentsOf: url as URL)!.base64EncodedString() as String
			
			// Set data to share as json
			let fileShare = [
				Constant.IntentFileType: String(fileType),
				Constant.IntentFileName: String(fileName!),
				Constant.IntentFileSize: String(fileSize),
				Constant.IntentFileBase64: String("data:" + fileType + ";base64," + fileBase64)
			] as [String : String]
			
			// Convert to json string
			let jsonData = try! JSONSerialization.data(withJSONObject: fileShare as [String : String], options: [])
			let jsonString = String(data: jsonData, encoding: .utf8)!
			
			UserDefaults(suiteName: Constant.shareGroup)?.set(jsonString, forKey: Constant.shareIncomingFile)
			self.openMainApp()
		}
	}
	
	/*
	Handle files to share
	*/
	private func handleIncomingFiles(itemProviders: [NSItemProvider]) {
		// Init files
		var filesShare = [[String : String]]()
		
		// Loop over the list of items
		for itemProvider in itemProviders {
			// N.B. loadItem is asynchronous
			itemProvider.loadItem(forTypeIdentifier: UTType.data.identifier, options: nil) { (item, error) in
				if let error = error {
					print("Share file error: \(error.localizedDescription)")
				}
				// Convert to url
				let url = item as! NSURL
				// Get mimetype
				let fileType = self.mimeTypeForPath(url)
				// Get name
				let fileName = url.lastPathComponent
				// Get size
				var fileSize = 0;
				do {
					fileSize = try (url.resourceValues(forKeys:[.fileSizeKey]).values.first as! Int)
				} catch {
					print("Share file error: \(error)")
				}
				
				// Get data and convert it to base64 string
				let fileBase64 = NSData(contentsOf: url as URL)!.base64EncodedString() as String
				
				// Set data to share as json
				filesShare.append([
					Constant.IntentFileType: String(fileType),
					Constant.IntentFileName: String(fileName!),
					Constant.IntentFileSize: String(fileSize),
					Constant.IntentFileBase64: String("data:" + fileType + ";base64," + fileBase64)
				] as [String : String])
				
				// Wait for all items to be loaded before opening the main app
				if filesShare.count == itemProviders.count {
					// Convert to json string
					 let jsonData = try! JSONSerialization.data(withJSONObject: filesShare as [[String : String]], options: [])
					 let jsonString = String(data: jsonData, encoding: .utf8)!
					 
					 UserDefaults(suiteName: Constant.shareGroup)?.set(jsonString, forKey: Constant.shareIncomingFiles)
					 self.openMainApp()
				}
			}
		}	
	}
	
	/*
	Get mimetype from NSURL resource
	*/
	private func mimeTypeForPath(_ url: NSURL) -> String {
		let pathExtension = url.pathExtension
		if let uti = UTTypeCreatePreferredIdentifierForTag(kUTTagClassFilenameExtension, pathExtension! as NSString, nil)?.takeRetainedValue() {
			if let mimetype = UTTypeCopyPreferredTagWithClass(uti, kUTTagClassMIMEType)?.takeRetainedValue() {
				return mimetype as String
			}
		}
		return "application/octet-stream"
	}
	
	/*
	Call the main app to open and handle the content to share via AppDelegate
	*/
	private func openMainApp() {
		self.extensionContext?.completeRequest(returningItems: nil, completionHandler: { _ in
			guard let url = URL(string: Constant.shareURL) else { return }
			_ = self.openURL(url)
		})
	}
	
	// Courtesy: https://stackoverflow.com/a/44499222/13363449 👇🏾
	// Function must be named exactly like this so a selector can be found by the compiler!
	// Anyway - it's another selector in another instance that would be "performed" instead.
	@objc func openURL(_ url: URL) -> Bool {
		var responder: UIResponder? = self
		while responder != nil {
			if let application = responder as? UIApplication {
				return application.perform(#selector(openURL(_:)), with: url) != nil
			}
			responder = responder?.next
		}
		return false
	}
	
}
