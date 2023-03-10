// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/message/message-meta.ts >>>

import Foundation

open class MessageMeta {
  public let mid: ID
  public let fid: ID
  public let tid: ID!
  public let lid: YSArray<LabelID>
  public let subjectEmpty: Bool
  public let subjectPrefix: String!
  public let subjectText: String
  public let firstLine: String
  public let sender: String
  public let unread: Bool
  public let searchOnly: Bool
  public let showFor: String!
  public let timestamp: Int64
  public let hasAttach: Bool
  public let attachments: Attachments!
  public let typeMask: Int32
  public let threadCount: String!
  public init(_ mid: ID, _ fid: ID, _ tid: ID!, _ lid: YSArray<LabelID>, _ subjectEmpty: Bool, _ subjectPrefix: String!, _ subjectText: String, _ firstLine: String, _ sender: String, _ unread: Bool, _ searchOnly: Bool, _ showFor: String!, _ timestamp: Int64, _ hasAttach: Bool, _ attachments: Attachments!, _ typeMask: Int32, _ threadCount: String!) {
    self.mid = mid
    self.fid = fid
    self.tid = tid
    self.lid = lid
    self.subjectEmpty = subjectEmpty
    self.subjectPrefix = subjectPrefix
    self.subjectText = subjectText
    self.firstLine = firstLine
    self.sender = sender
    self.unread = unread
    self.searchOnly = searchOnly
    self.showFor = showFor
    self.timestamp = timestamp
    self.hasAttach = hasAttach
    self.attachments = attachments
    self.typeMask = typeMask
    self.threadCount = threadCount
  }
}

open class Attachments {
  public let attachments: YSArray<Attachment>
  public init(_ attachments: YSArray<Attachment>) {
    self.attachments = attachments
  }
}

open class Attachment {
  public let hid: String
  public let displayName: String
  public let fileClass: String
  public let isDisk: Bool
  public let size: Int64
  public let mimeType: String
  public let previewSupported: Bool
  public let previewUrl: String!
  public let downloadUrl: String
  public let isInline: Bool
  public let contentID: String!
  public init(_ hid: String, _ displayName: String, _ fileClass: String, _ isDisk: Bool, _ size: Int64, _ mimeType: String, _ previewSupported: Bool, _ previewUrl: String!, _ downloadUrl: String, _ isInline: Bool, _ contentID: String!) {
    self.hid = hid
    self.displayName = displayName
    self.fileClass = fileClass
    self.isDisk = isDisk
    self.size = size
    self.mimeType = mimeType
    self.previewSupported = previewSupported
    self.previewUrl = previewUrl
    self.downloadUrl = downloadUrl
    self.isInline = isInline
    self.contentID = contentID
  }
}

@discardableResult
public func attachmentsFromJSONItem(_ json: JSONItem) -> Attachments! {
  if json.kind != JSONItemKind.map {
    return nil
  }
  let attachments: YSArray<Attachment> = YSArray()
  for item in (json as! MapJSONItem).getArrayOrDefault("attachments", YSArray()) {
    if item.kind == JSONItemKind.map {
      let map = item as! MapJSONItem
      attachments.push(Attachment(map.getString("hid")!, map.getString("display_name")!, map.getString("class")!, map.getBooleanOrDefault("narod", false), map.getInt64("size")!, map.getString("mime_type")!, map.getBooleanOrDefault("preview_supported", false), map.getString("preview_url"), map.getString("download_url")!, map.getBooleanOrDefault("is_inline", false), map.getString("content_id")))
    }
  }
  return Attachments(attachments)
}

@discardableResult
public func messageMetaFromJSONItem(_ json: JSONItem) -> MessageMeta! {
  if json.kind != JSONItemKind.map {
    return nil
  }
  let map = json as! MapJSONItem
  let mid = idFromString(map.getString("mid"))!
  let fid = idFromString(map.getString("fid"))!
  let tid: Int64! = idFromString(map.getString("tid"))
  let lids = map.getArrayOrDefault("lid", YSArray()).map {
    item in
    (item as! StringJSONItem).value
  }
  let subjectEmpty = map.getBoolean("subjEmpty")!
  let subjectPrefix = map.getString("subjPrefix")!
  let subjectText = map.getString("subjText")!
  let firstLine = map.getStringOrDefault("firstLine", "")
  let sender = recipientFromJSONItem(map.get("from")!)!.asString()
  let isUnread = map.getArray("status")!.map {
    (statusItem) -> Int32 in
    let value: Int32! = JSONItemToInt32(statusItem)
    return (value != nil) ? value : 0
  }.includes(1)
  let timestamp = stringToInt64(map.getString("utc_timestamp")!)! * int64(1000)
  let hasAttach = map.getBoolean("hasAttach")!
  let types = messageTypeMaskFromServerMessageTypes(map.getArray("types")!.map {
    item in
    stringToInt32((item as! StringJSONItem).value)!
  })
  var attachments: Attachments!
  if map.hasKey("attachments") {
    attachments = attachmentsFromJSONItem(map.get("attachments")!)
  }
  let threadCount: String! = map.getString("threadCount")
  return MessageMeta(mid, fid, tid, lids, subjectEmpty, subjectPrefix, subjectText, firstLine, sender, isUnread, false, nil, timestamp, hasAttach, attachments, types, threadCount)
}

@discardableResult
public func getMidToTimestampMap(_ metas: YSArray<MessageMeta>) -> YSMap<ID, Int64> {
  let midToTimestamp = YSMap<ID, Int64>()
  for meta in metas {
    midToTimestamp.set(meta.mid, meta.timestamp)
  }
  return midToTimestamp
}
