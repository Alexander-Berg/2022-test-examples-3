// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/message/message-request-item.ts >>>

import Foundation

open class MessageRequestItem {
  public let fid: ID!
  private let tid: ID!
  private let lid: LabelID!
  private let first: Int32
  private let last: Int32
  private let threaded: Bool
  private init(_ fid: ID!, _ tid: ID!, _ lid: LabelID!, _ first: Int32, _ last: Int32, _ threaded: Bool) {
    self.fid = fid
    self.tid = tid
    self.lid = lid
    self.first = first
    self.last = last
    self.threaded = threaded
  }

  @discardableResult
  open class func threads(_ fid: ID, _ first: Int32, _ last: Int32) -> MessageRequestItem {
    return MessageRequestItem(fid, nil, nil, first, last, true)
  }

  @discardableResult
  open class func messagesInThread(_ tid: ID, _ first: Int32, _ last: Int32) -> MessageRequestItem {
    return MessageRequestItem(nil, tid, nil, first, last, false)
  }

  @discardableResult
  open class func messagesInFolder(_ fid: ID, _ first: Int32, _ last: Int32) -> MessageRequestItem {
    return MessageRequestItem(fid, nil, nil, first, last, false)
  }

  @discardableResult
  open class func messagesWithLabel(_ lid: LabelID, _ first: Int32, _ last: Int32) -> MessageRequestItem {
    return MessageRequestItem(nil, nil, lid, first, last, false)
  }

  @discardableResult
  open func params() -> NetworkParams {
    let result = MapJSONItem()
    if fid != nil {
      result.putString("fid", idToString(fid)!)
    }
    if tid != nil {
      result.putString("tid", idToString(tid)!)
    }
    if lid != nil {
      result.putString("lid", lid)
    }
    return result.putInt32("first", first).putInt32("last", last).putBoolean("threaded", threaded).putString("md5", "").putBoolean("returnIfModified", true)
  }
}
