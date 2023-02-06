// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/value-map-builder.ts >>>

import Foundation

open class ValueMapBuilder {
  private let map: YSMap<String, JSONItem> = YSMap<String, JSONItem>()
  private init(_ map: YSMap<String, JSONItem> = YSMap<String, JSONItem>()) {
    let `self` = self
    map.__forEach {
      v, k in
      `self`.map.set(k, v)
    }
  }

  @discardableResult
  open class func userEvent() -> ValueMapBuilder {
    return ValueMapBuilder().setString("event_type", "user")
  }

  @discardableResult
  open class func systemEvent() -> ValueMapBuilder {
    return ValueMapBuilder().setString("event_type", "system")
  }

  @discardableResult
  open class func modelSyncEvent() -> ValueMapBuilder {
    return ValueMapBuilder().setString("event_type", "model_sync")
  }

  @discardableResult
  open class func customEvent(_ source: String, _ map: YSMap<String, JSONItem> = YSMap<String, JSONItem>()) -> ValueMapBuilder {
    return ValueMapBuilder(map).setString("event_type", "other").setString("event_source", source)
  }

  @discardableResult
  open class func __parse(_ map: YSMap<String, JSONItem>) -> ValueMapBuilder {
    return ValueMapBuilder(map)
  }

  @discardableResult
  open func setEventName(_ name: String) -> ValueMapBuilder {
    return setString("event_name", name)
  }

  @discardableResult
  open func addStartEvent() -> ValueMapBuilder {
    return setBoolean("start_event", true)
  }

  @discardableResult
  open func addOrder(_ order: Int32) -> ValueMapBuilder {
    return setInt32("order", order)
  }

  @discardableResult
  open func addCount(_ count: Int32) -> ValueMapBuilder {
    return setInt32("count", count)
  }

  @discardableResult
  open func addRepliesNumber(_ repliesNumber: Int32!) -> ValueMapBuilder {
    if repliesNumber != nil {
      return setInt32("repliesNumber", repliesNumber)
    }
    return self
  }

  @discardableResult
  open func addRepliesNumbers(_ repliesNumbers: YSArray<Int32>!) -> ValueMapBuilder {
    if repliesNumbers != nil {
      return setInt32Array("repliesNumbers", repliesNumbers)
    }
    return self
  }

  @discardableResult
  open func addLength(_ length: Int32!) -> ValueMapBuilder {
    if length != nil {
      return setInt32("length", length)
    }
    return self
  }

  @discardableResult
  open func addUid(_ uid: Int64!) -> ValueMapBuilder {
    if uid != nil {
      return setInt64("uid", uid)
    }
    return self
  }

  @discardableResult
  open func addMid(_ mid: Int64!) -> ValueMapBuilder {
    if mid != nil {
      return setInt64("mid", mid)
    }
    return self
  }

  @discardableResult
  open func addMids(_ mids: YSArray<Int64>!) -> ValueMapBuilder {
    if mids != nil {
      return setInt64Array("mids", mids)
    }
    return self
  }

  @discardableResult
  open func addFid(_ fid: Int64) -> ValueMapBuilder {
    return setInt64("fid", fid)
  }

  @discardableResult
  open func addTid(_ tid: Int64) -> ValueMapBuilder {
    return setInt64("tid", tid)
  }

  @discardableResult
  open func addMessages(_ messages: YSArray<MessageDTO>!) -> ValueMapBuilder {
    if messages != nil {
      let array = ArrayJSONItem()
      for message in messages {
        array.add(message.toJson())
      }
      map.set("messages", array)
    }
    return self
  }

  @discardableResult
  open func addDebug() -> ValueMapBuilder {
    return setBoolean("debug", true)
  }

  @discardableResult
  open func addError() -> ValueMapBuilder {
    return setBoolean("error", true)
  }

  @discardableResult
  open func addReason(_ reason: String) -> ValueMapBuilder {
    return setString("reason", reason)
  }

  @discardableResult
  open func addEvent(_ event: String) -> ValueMapBuilder {
    return setString("event", event)
  }

  @discardableResult
  open func addSaveDraft(_ saveDraft: Bool) -> ValueMapBuilder {
    return setBoolean("saveDraft", saveDraft)
  }

  @discardableResult
  open func build() -> YSMap<String, JSONItem> {
    return map
  }

  @discardableResult
  private func setBoolean(_ name: String, _ value: Bool) -> ValueMapBuilder {
    map.set(name, BooleanJSONItem(value))
    return self
  }

  @discardableResult
  private func setInt32(_ name: String, _ value: Int32) -> ValueMapBuilder {
    map.set(name, IntegerJSONItem.fromInt32(value))
    return self
  }

  @discardableResult
  private func setInt64(_ name: String, _ value: Int64) -> ValueMapBuilder {
    map.set(name, IntegerJSONItem.fromInt64(value))
    return self
  }

  @discardableResult
  private func setString(_ name: String, _ value: String) -> ValueMapBuilder {
    map.set(name, StringJSONItem(value))
    return self
  }

  @discardableResult
  private func setInt32Array(_ name: String, _ value: YSArray<Int32>) -> ValueMapBuilder {
    map.set(name, ArrayJSONItem(value.map {
      n in
      IntegerJSONItem.fromInt32(n)
    }))
    return self
  }

  @discardableResult
  private func setInt64Array(_ name: String, _ value: YSArray<Int64>) -> ValueMapBuilder {
    map.set(name, ArrayJSONItem(value.map {
      n in
      IntegerJSONItem.fromInt64(n)
    }))
    return self
  }
}