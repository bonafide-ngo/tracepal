import Foundation

struct Constant {
	// System flags
	static let Debug = false
	static let Log = false
	// App constants
	static let AppName = "TracePal"
	static let AppUrl = "https://tracepal.app/app/"
	static let AppApi = "https://tracepal.app/api/"

	/*
	 * Javascript integration *************************************************
	 */
	 
	// Hooks
	static let hookDownload = "hookDownload"
	static let hookSyncCookies = "hookSyncCookies"
	static let hookFirebaseToken = "hookFirebaseToken"
	static let hookFirebaseToken_callback = "frm.firebase.ios.hookFirebaseToken_Callback"
	static let hookStartTracker = "hookStartTracker"
	static let hookStopTracker = "hookStopTracker"
	// Handlers
	static let firebaseHandler = "frm.firebase.ios.handler"
	static let shareTextHandler = "frm.share.ios.textHandler"
	static let shareFileHandler = "frm.share.ios.fileHandler"
	static let shareFilesHandler = "frm.share.ios.filesHandler"
	static let killSwitchHandler = "frm.webrtc.ios.killSwitch"
	// Notification center
	static let notificationForeground = "notificationForeground"
	static let notificationMaxLoops = 3
	// Share group
	static let shareGroup = "group.tracepal"
	// Share incoming
	static let shareIncomingText = "shareIncomingText"
	static let shareIncomingFile = "shareIncomingFile"
	static let shareIncomingFiles = "shareIncomingFiles"
	// Redirect URLscheme
	static let redirectUrl = "redirectUrl"
	static let redirectUrlScheme = "TracePal"
	static let redirectUrlParam = "url"
	// Call controls
	static let callReject = "callReject"
	static let callAnswer = "callAnswer"
	static let callCategory = "callCategory"
	// Location
	static let locationDistance = 5
	static let locationInterval = 60
}
