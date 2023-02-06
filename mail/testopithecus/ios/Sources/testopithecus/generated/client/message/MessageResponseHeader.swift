// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/message/message-response-header.ts >>>

import Foundation

open class MessageResponseHeaderPayload {
  public let md5: String
  public let countTotal: Int32
  public let countUnread: Int32
  public let modified: Bool
  public let batchCount: Int32
  public init(_ md5: String, _ countTotal: Int32, _ countUnread: Int32, _ modified: Bool, _ batchCount: Int32) {
    self.md5 = md5
    self.countTotal = countTotal
    self.countUnread = countUnread
    self.modified = modified
    self.batchCount = batchCount
  }
}

open class MessagesResponseHeader {
  public let error: Int32
  public let payload: MessageResponseHeaderPayload!
  private init(_ error: Int32, _ payload: MessageResponseHeaderPayload!) {
    self.error = error
    self.payload = payload
  }

  @discardableResult
  open class func withError(_ error: Int32) -> MessagesResponseHeader {
    return MessagesResponseHeader(error, nil)
  }

  @discardableResult
  open class func withPayload(_ md5: String, _ countTotal: Int32, _ countUnread: Int32, _ modified: Bool, _ batchCount: Int32) -> MessagesResponseHeader {
    return MessagesResponseHeader(1, MessageResponseHeaderPayload(md5, countTotal, countUnread, modified, batchCount))
  }
}

@discardableResult
public func messageResponseHeaderFromJSONItem(_ item: JSONItem) -> MessagesResponseHeader! {
  if item.kind != JSONItemKind.map {
    return nil
  }
  let map = item as! MapJSONItem
  let error = map.getInt32("error")!
  if error != 1 {
    return MessagesResponseHeader.withError(error)
  }
  let md5 = map.getString("md5")!
  let countTotal = map.getInt32("countTotal")!
  let countUnread = map.getInt32("countUnread")!
  let modified = map.getBoolean("modified")!
  let batchCount = map.getInt32("batchCount")!
  return MessagesResponseHeader.withPayload(md5, countTotal, countUnread, modified, batchCount)
}