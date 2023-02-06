// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/recipient/recipient.ts >>>

import Foundation

open class Recipient {
  public let email: String
  public let name: String!
  public let type: RecipientType
  public init(_ email: String, _ name: String!, _ type: RecipientType) {
    self.email = email
    self.name = name
    self.type = type
  }

  @discardableResult
  open func asString() -> String {
    let result = StringBuilder()
    let hasName = name != nil && name.length > 0
    if hasName {
      result.add(name!)
    }
    if email.length > 0 {
      result.add(hasName ? " " : "").add("<").add(email).add(">")
    }
    return result.build()
  }
}

public enum RecipientType: Int32, Codable {
  case to = 1
  case from = 2
  case cc = 3
  case bcc = 4
  case replyTo = 5
  public func toInt() -> Int32 {
    return rawValue
  }
}

@discardableResult
public func int32ToRecipientType(_ value: Int32) -> RecipientType! {
  switch value {
  case 1:
    return RecipientType.to
  case 2:
    return RecipientType.from
  case 3:
    return RecipientType.cc
  case 4:
    return RecipientType.bcc
  case 5:
    return RecipientType.replyTo
  default:
    return nil
  }
}

@discardableResult
public func recipientTypeToInt32(_ value: RecipientType) -> Int32 {
  switch value {
  case RecipientType.to:
    return 1
  case RecipientType.from:
    return 2
  case RecipientType.cc:
    return 3
  case RecipientType.bcc:
    return 4
  case RecipientType.replyTo:
    return 5
  }
}

@discardableResult
public func recipientFromJSONItem(_ item: JSONItem) -> Recipient! {
  if item.kind != JSONItemKind.map {
    return nil
  }
  let map = item as! MapJSONItem
  let email = map.getString("email")!
  let name = map.getString("name")!
  let type: RecipientType! = int32ToRecipientType(map.getInt32("type")!)
  if type == nil {
    return nil
  }
  return Recipient(email, name, type)
}

@discardableResult
public func recipientToJSONItem(_ recipient: Recipient) -> JSONItem {
  let item = MapJSONItem()
  item.putString("email", recipient.email)
  if recipient.name != nil {
    item.putString("name", recipient.name)
  }
  item.putInt32("type", recipientTypeToInt32(recipient.type))
  return item
}