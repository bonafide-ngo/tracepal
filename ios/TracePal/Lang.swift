import Foundation

class Lang {
	
	// ISO languages
	static let iso_it = "it"
	static let iso_en = "en" // default
	
	/*
	 * Get a keyword by language iso code
	 */
	static func get(key: String, iso: String = (NSLocale.current.language.languageCode?.identifier ?? Lang.iso_en)) -> String {
		switch iso {
		case Lang.iso_it:
			return Lang.it[key] ?? "[" + key + "]"
		default:
			return Lang.en[key] ?? "[" + key + "]"
		}
	}

	// Language dictionary
	static let it = [
		"answer": "Rispondi",
		"error_ios": "Errore iOS",
		"error_email": "Il tuo dispositivo non è configurato per inviare email",
		"reject": "Rifiuta",
	]
	static let en = [
		"answer": "Answer",
		"error_ios": "iOS error",
		"error_email": "Your device is not configured to send emails",
		"reject": "Reject",
	]
}
