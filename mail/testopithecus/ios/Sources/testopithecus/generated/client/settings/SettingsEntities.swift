// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/settings/settings-entities.ts >>>

import Foundation

open class SettingsResponse {
  public let status: NetworkStatus
  public let payload: StatusResponsePayload!
  public init(_ status: NetworkStatus, _ payload: StatusResponsePayload!) {
    self.status = status
    self.payload = payload
  }
}

open class StatusResponsePayload {
  public let accountInformation: AccountInformation
  public let userParameters: UserParameters
  public let settingsSetup: SettingsSetup
  public init(_ accountInformation: AccountInformation, _ userParameters: UserParameters, _ settingsSetup: SettingsSetup) {
    self.accountInformation = accountInformation
    self.userParameters = userParameters
    self.settingsSetup = settingsSetup
  }
}

@discardableResult
public func settingsResponseFromJSONItem(_ item: JSONItem) -> SettingsResponse! {
  if item.kind != JSONItemKind.map {
    return nil
  }
  let map = item as! MapJSONItem
  let status = networkStatusFromJSONItem(map.get("status")! as! MapJSONItem)!
  if status.code != NetworkStatusCode.ok {
    return SettingsResponse(status, nil)
  }
  let accountInformation = accountInformationFromJSONItem(map.getMap("account_information")!.get("account-information")! as! MapJSONItem)
  let userParameters = userParametersFromJSONItem(map.getMap("get_user_parameters")!.get("body")! as! MapJSONItem)
  let settingsSetup = settingsSetupFromJSONItem(map.getMap("settings_setup")!.get("body")! as! MapJSONItem)
  return SettingsResponse(status, StatusResponsePayload(accountInformation, userParameters, settingsSetup))
}

open class Email {
  public let login: String
  public let domain: String
  public init(_ login: String, _ domain: String) {
    self.login = login
    self.domain = domain
  }
}

@discardableResult
public func stringToEmail(_ email: String) -> Email! {
  let parts = email.split("@")
  if parts.length != 2 {
    return nil
  }
  return Email(parts[0], parts[1])
}

open class AccountInformation {
  public let uid: String
  public let suid: String
  public let emails: YSArray<Email>
  public let composeCheck: String
  public init(_ uid: String, _ suid: String, _ emails: YSArray<Email>, _ composeCheck: String) {
    self.uid = uid
    self.suid = suid
    self.emails = emails
    self.composeCheck = composeCheck
  }
}

@discardableResult
private func accountInformationFromJSONItem(_ item: MapJSONItem) -> AccountInformation {
  let uid = item.getString("uid")!
  let suid = item.getString("suid")!
  let composeCheck = item.getString("compose-check")!
  let emails: YSArray<Email> = YSArray()
  let emailsArray = item.getMap("emails")!.get("email")! as! ArrayJSONItem
  for element in emailsArray.asArray() {
    let mapElement = element as! MapJSONItem
    let login = mapElement.getString("login")!
    let domain = mapElement.getString("domain")!
    emails.push(Email(login, domain))
  }
  return AccountInformation(uid, suid, emails, composeCheck)
}

open class UserParameters {
  public let seasonsModifier: String!
  public let keyValues: YSMap<String, String>
  public init(_ seasonsModifier: String!, _ keyValues: YSMap<String, String>) {
    self.seasonsModifier = seasonsModifier
    self.keyValues = keyValues
  }
}

@discardableResult
private func userParametersFromJSONItem(_ item: MapJSONItem) -> UserParameters {
  let keyValues = YSMap<String, String>()
  for key in item.asMap().keys() {
    keyValues.set(key, item.getString(key)!)
  }
  return UserParameters(item.getString("seasons-modifier"), keyValues)
}

public enum SignaturePlace: Int32, Codable {
  case none = 0
  case atEnd = 1
  case afterReply = 2
  public func toInt() -> Int32 {
    return rawValue
  }
}

@discardableResult
public func signaturePlaceToInt32(_ value: SignaturePlace) -> Int32 {
  switch value {
  case SignaturePlace.none:
    return 0
  case SignaturePlace.atEnd:
    return 1
  case SignaturePlace.afterReply:
    return 2
  }
}

@discardableResult
public func int32ToSignaturePlace(_ value: Int32) -> SignaturePlace {
  switch value {
  case 0:
    return SignaturePlace.none
  case 1:
    return SignaturePlace.atEnd
  case 2:
    return SignaturePlace.afterReply
  default:
    return SignaturePlace.none
  }
}

open class SettingsSetup {
  public let colorScheme: String
  public let fromName: String
  public let defaultEmail: String
  public let folderThreadView: Bool
  public let quotationChar: String
  public let mobileSign: String
  public let signatureTop: SignaturePlace
  public let replyTo: YSArray<String>
  public init(_ colorScheme: String, _ fromName: String, _ defaultEmail: String, _ folderThreadView: Bool, _ quotationChar: String, _ mobileSign: String, _ signatureTop: SignaturePlace, _ replyTo: YSArray<String>) {
    self.colorScheme = colorScheme
    self.fromName = fromName
    self.defaultEmail = defaultEmail
    self.folderThreadView = folderThreadView
    self.quotationChar = quotationChar
    self.mobileSign = mobileSign
    self.signatureTop = signatureTop
    self.replyTo = replyTo
  }
}

@discardableResult
private func settingsSetupFromJSONItem(_ item: MapJSONItem) -> SettingsSetup {
  let colorScheme = item.getString("color_scheme")!
  let fromName = item.getString("from_name")!
  let defaultEmail = item.getString("default_email")!
  let folderThreadView = item.getString("folder_thread_view") == "on"
  let mobileSignature = item.getString("mobile_sign")!
  let quotationChar = item.getString("quotation_char")!
  let signatureOnTop = item.hasKey("signature_top") ? (item.getString("signature_top") == "on" ? SignaturePlace.afterReply : SignaturePlace.atEnd) : SignaturePlace.none
  let replyTo: YSArray<String> = YSArray()
  return SettingsSetup(colorScheme, fromName, defaultEmail, folderThreadView, quotationChar, mobileSignature, signatureOnTop, replyTo)
}
