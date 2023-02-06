// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/json-types.ts >>>

import Foundation

public enum JSONItemKind {
  case integer
  case double
  case string
  case boolean
  case nullItem
  case map
  case array
}

@discardableResult
public func JSONItemKindToString(_ kind: JSONItemKind) -> String {
  switch kind {
  case JSONItemKind.integer:
    return "integer"
  case JSONItemKind.double:
    return "double"
  case JSONItemKind.string:
    return "string"
  case JSONItemKind.boolean:
    return "boolean"
  case JSONItemKind.nullItem:
    return "nullItem"
  case JSONItemKind.map:
    return "map"
  case JSONItemKind.array:
    return "array"
  }
}

@discardableResult
public func JSONItemGetValueDebugDescription(_ item: JSONItem) -> String {
  switch item.kind {
  case JSONItemKind.integer:
    return int64ToString((item as! IntegerJSONItem).asInt64())
  case JSONItemKind.double:
    return doubleToString((item as! DoubleJSONItem).value)
  case JSONItemKind.string:
    return quote((item as! StringJSONItem).value)
  case JSONItemKind.boolean:
    return (item as! BooleanJSONItem).value ? "true" : "false"
  case JSONItemKind.nullItem:
    return "null"
  case JSONItemKind.map:
    let map = item as! MapJSONItem
    let mapValues: YSArray<String> = YSArray()
    map.asMap().__forEach {
      (value: JSONItem, key: String) in
      mapValues.push("\"\(key)\": \(JSONItemGetDebugDescription(value))")
    }
    return "{\(mapValues.join(", "))}"
  case JSONItemKind.array:
    let array = item as! ArrayJSONItem
    let arrayValues: YSArray<String> = array.asArray().map {
      value in
      JSONItemGetDebugDescription(value)
    }
    return "[\(arrayValues.join(", "))]"
  }
}

@discardableResult
public func JSONItemGetDebugDescription(_ item: JSONItem) -> String {
  let valueDescription = JSONItemGetValueDebugDescription(item)
  return "<JSONItem kind: \(JSONItemKindToString(item.kind)), value: \(valueDescription)>"
}

public protocol JSONItem {
  var kind: JSONItemKind { get }
}

open class IntegerJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.integer
  private let value: Int64
  public let isInt64: Bool
  private init(_ value: Int64, _ isInt64: Bool) {
    self.value = value
    self.isInt64 = isInt64
  }

  @discardableResult
  open class func fromInt32(_ value: Int32) -> IntegerJSONItem {
    return IntegerJSONItem(int32ToInt64(value), false)
  }

  @discardableResult
  open class func fromInt64(_ value: Int64) -> IntegerJSONItem {
    return IntegerJSONItem(value, true)
  }

  @discardableResult
  open func asInt32() -> Int32 {
    return int64ToInt32(value)
  }

  @discardableResult
  open func asInt64() -> Int64 {
    return value
  }
}

open class DoubleJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.double
  public let value: Double
  public init(_ value: Double) {
    self.value = value
  }
}

open class StringJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.string
  public let value: String
  public init(_ value: String) {
    self.value = value
  }
}

open class BooleanJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.boolean
  public let value: Bool
  public init(_ value: Bool) {
    self.value = value
  }
}

open class NullJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.nullItem
}

open class MapJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.map
  private let value: YSMap<String, JSONItem>
  public init(_ value: YSMap<String, JSONItem> = YSMap<String, JSONItem>()) {
    self.value = value
  }

  @discardableResult
  open func asMap() -> YSMap<String, JSONItem> {
    return value
  }

  @discardableResult
  open func put(_ key: String, _ value: JSONItem) -> MapJSONItem {
    self.value.set(key, value)
    return self
  }

  @discardableResult
  open func putInt32(_ key: String, _ value: Int32) -> MapJSONItem {
    self.value.set(key, IntegerJSONItem.fromInt32(value))
    return self
  }

  @discardableResult
  open func putInt64(_ key: String, _ value: Int64) -> MapJSONItem {
    self.value.set(key, IntegerJSONItem.fromInt64(value))
    return self
  }

  @discardableResult
  open func putDouble(_ key: String, _ value: Double) -> MapJSONItem {
    self.value.set(key, DoubleJSONItem(value))
    return self
  }

  @discardableResult
  open func putBoolean(_ key: String, _ value: Bool) -> MapJSONItem {
    self.value.set(key, BooleanJSONItem(value))
    return self
  }

  @discardableResult
  open func putString(_ key: String, _ value: String) -> MapJSONItem {
    self.value.set(key, StringJSONItem(value))
    return self
  }

  @discardableResult
  open func putStringIfPresent(_ key: String, _ value: String!) -> MapJSONItem {
    if value != nil {
      putString(key, value!)
    }
    return self
  }

  @discardableResult
  open func putNull(_ key: String) -> MapJSONItem {
    value.set(key, NullJSONItem())
    return self
  }

  @discardableResult
  open func get(_ key: String) -> JSONItem! {
    return undefinedToNull(value.get(key))
  }

  @discardableResult
  open func getArray(_ key: String) -> YSArray<JSONItem>! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil || result.kind != JSONItemKind.array {
      return nil
    }
    return (result as! ArrayJSONItem).asArray()
  }

  @discardableResult
  open func getArrayOrDefault(_ key: String, _ value: YSArray<JSONItem>) -> YSArray<JSONItem> {
    return getArray(key) ?? value
  }

  @discardableResult
  open func getMap(_ key: String) -> YSMap<String, JSONItem>! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil || result.kind != JSONItemKind.map {
      return nil
    }
    return (result as! MapJSONItem).asMap()
  }

  @discardableResult
  open func getMapOrDefault(_ key: String, _ value: YSMap<String, JSONItem>) -> YSMap<String, JSONItem> {
    return getMap(key) ?? value
  }

  @discardableResult
  open func getInt32(_ key: String) -> Int32! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil {
      return nil
    }
    return JSONItemToInt32(result)
  }

  @discardableResult
  open func getInt32OrDefault(_ key: String, _ value: Int32) -> Int32 {
    return getInt32(key) ?? value
  }

  @discardableResult
  open func getInt64(_ key: String) -> Int64! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil {
      return nil
    }
    return JSONItemToInt64(result)
  }

  @discardableResult
  open func getInt64OrDefault(_ key: String, _ value: Int64) -> Int64 {
    return getInt64(key) ?? value
  }

  @discardableResult
  open func getDouble(_ key: String) -> Double! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil {
      return nil
    }
    return JSONItemToDouble(result)
  }

  @discardableResult
  open func getDoubleOrDefault(_ key: String, _ value: Double) -> Double {
    return getDouble(key) ?? value
  }

  @discardableResult
  open func getBoolean(_ key: String) -> Bool! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil || result.kind != JSONItemKind.boolean {
      return nil
    }
    return (result as! BooleanJSONItem).value
  }

  @discardableResult
  open func getBooleanOrDefault(_ key: String, _ value: Bool) -> Bool {
    return getBoolean(key) ?? value
  }

  @discardableResult
  open func getString(_ key: String) -> String! {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil || result.kind != JSONItemKind.string {
      return nil
    }
    return (result as! StringJSONItem).value
  }

  @discardableResult
  open func getStringOrDefault(_ key: String, _ value: String) -> String {
    return getString(key) ?? value
  }

  @discardableResult
  open func isNull(_ key: String) -> Bool {
    let result: JSONItem! = undefinedToNull(value.get(key))
    if result == nil {
      return false
    }
    return result.kind == JSONItemKind.nullItem
  }

  @discardableResult
  open func hasKey(_ key: String) -> Bool {
    return undefinedToNull(value.get(key)) != nil
  }
}

open class ArrayJSONItem: JSONItem {
  public let kind: JSONItemKind = JSONItemKind.array
  private let value: YSArray<JSONItem>
  public init(_ value: YSArray<JSONItem> = YSArray()) {
    self.value = value
  }

  @discardableResult
  open func asArray() -> YSArray<JSONItem> {
    return value
  }

  @discardableResult
  open func getCount() -> Int32 {
    return value.length
  }

  @discardableResult
  open func add(_ value: JSONItem) -> ArrayJSONItem {
    self.value.push(value)
    return self
  }

  @discardableResult
  open func addInt32(_ value: Int32) -> ArrayJSONItem {
    self.value.push(IntegerJSONItem.fromInt32(value))
    return self
  }

  @discardableResult
  open func addInt64(_ value: Int64) -> ArrayJSONItem {
    self.value.push(IntegerJSONItem.fromInt64(value))
    return self
  }

  @discardableResult
  open func addDouble(_ value: Double) -> ArrayJSONItem {
    self.value.push(DoubleJSONItem(value))
    return self
  }

  @discardableResult
  open func addBoolean(_ value: Bool) -> ArrayJSONItem {
    self.value.push(BooleanJSONItem(value))
    return self
  }

  @discardableResult
  open func addString(_ value: String) -> ArrayJSONItem {
    self.value.push(StringJSONItem(value))
    return self
  }

  @discardableResult
  open func addNull() -> ArrayJSONItem {
    value.push(NullJSONItem())
    return self
  }

  @discardableResult
  open func get(_ index: Int32) -> JSONItem {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    return value[index]
  }

  @discardableResult
  open func getMap(_ index: Int32) -> YSMap<String, JSONItem> {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    let result = value[index]
    if result.kind != JSONItemKind.map {
      fatalError("Type is not \(JSONItemKind.map) at index \(index). It's \(result.kind)")
    }
    return (result as! MapJSONItem).asMap()
  }

  @discardableResult
  open func getArray(_ index: Int32) -> YSArray<JSONItem> {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    let result = value[index]
    if result.kind != JSONItemKind.array {
      fatalError("Type is not \(JSONItemKind.array) at index \(index). It's \(result.kind)")
    }
    return (result as! ArrayJSONItem).asArray()
  }

  @discardableResult
  open func getInt32(_ index: Int32) -> Int32 {
    if index < 0 || index >= self.value.length {
      fatalError("Index is out of bounds")
    }
    let value = self.value[index]
    let result: Int32! = JSONItemToInt32(value)
    if result != nil {
      return result
    }
    fatalError("Type is not Int32 at index \(index). It's \(value.kind)")
  }

  @discardableResult
  open func getInt64(_ index: Int32) -> Int64 {
    if index < 0 || index >= self.value.length {
      fatalError("Index is out of bounds")
    }
    let value = self.value[index]
    let result: Int64! = JSONItemToInt64(value)
    if result != nil {
      return result
    }
    fatalError("Type is not Int64 at index \(index). It's \(value.kind)")
  }

  @discardableResult
  open func getDouble(_ index: Int32) -> Double {
    if index < 0 || index >= self.value.length {
      fatalError("Index is out of bounds")
    }
    let value = self.value[index]
    let result: Double! = JSONItemToDouble(value)
    if result != nil {
      return result
    }
    fatalError("Type is not Double at index \(index). It's \(value.kind)")
  }

  @discardableResult
  open func getBoolean(_ index: Int32) -> Bool {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    let result = value[index]
    if result.kind != JSONItemKind.boolean {
      fatalError("Type is not \(JSONItemKind.boolean) at index \(index). It's \(result.kind)")
    }
    return (result as! BooleanJSONItem).value
  }

  @discardableResult
  open func getString(_ index: Int32) -> String {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    let result = value[index]
    if result.kind != JSONItemKind.string {
      fatalError("Type is not \(JSONItemKind.string) at index \(index). It's \(result.kind)")
    }
    return (result as! StringJSONItem).value
  }

  @discardableResult
  open func isNull(_ index: Int32) -> Bool {
    if index < 0 || index >= value.length {
      fatalError("Index is out of bounds")
    }
    return value[index].kind == JSONItemKind.nullItem
  }
}

@discardableResult
public func JSONItemToInt32(_ item: JSONItem) -> Int32! {
  switch item.kind {
  case JSONItemKind.double:
    return doubleToInt32((item as! DoubleJSONItem).value)
  case JSONItemKind.integer:
    return (item as! IntegerJSONItem).asInt32()
  case JSONItemKind.string:
    return stringToInt32((item as! StringJSONItem).value)
  default:
    return nil
  }
}

@discardableResult
public func JSONItemToInt64(_ item: JSONItem) -> Int64! {
  switch item.kind {
  case JSONItemKind.double:
    return doubleToInt64((item as! DoubleJSONItem).value)
  case JSONItemKind.integer:
    return (item as! IntegerJSONItem).asInt64()
  case JSONItemKind.string:
    return stringToInt64((item as! StringJSONItem).value)
  default:
    return nil
  }
}

@discardableResult
public func JSONItemToDouble(_ item: JSONItem) -> Double! {
  switch item.kind {
  case JSONItemKind.double:
    return (item as! DoubleJSONItem).value
  case JSONItemKind.integer:
    return int64ToDouble((item as! IntegerJSONItem).asInt64())
  case JSONItemKind.string:
    return stringToDouble((item as! StringJSONItem).value)
  default:
    return nil
  }
}

@discardableResult
public func JSONItemGetValue(_ item: JSONItem) -> Any! {
  switch item.kind {
  case JSONItemKind.string:
    return (item as! StringJSONItem).value
  case JSONItemKind.boolean:
    return (item as! BooleanJSONItem).value
  case JSONItemKind.integer:
    let i = item as! IntegerJSONItem
    return i.isInt64 ? i.asInt64() : i.asInt32()
  case JSONItemKind.double:
    return (item as! DoubleJSONItem).value
  case JSONItemKind.array:
    return (item as! ArrayJSONItem).asArray().map {
      it in
      JSONItemGetValue(it)
    }
  case JSONItemKind.map:
    let res = YSMap<String, Any>()
    (item as! MapJSONItem).asMap().__forEach {
      v, k in
      let val = JSONItemGetValue(v)
      if val != nil {
        res.set(k, val)
      }
    }
    return res
  default:
    return nil
  }
}