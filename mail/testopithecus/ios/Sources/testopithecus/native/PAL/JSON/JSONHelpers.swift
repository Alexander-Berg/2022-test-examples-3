//
//  JSONHelpers.swift
//  XMail
//
//  Created by Dmitry Zakharov on 22/05/2019.
//

import Foundation

public func JSONItemFromAny(_ value: Any?) -> JSONItem? {
  switch value {
  case nil, is NSNull:
    return NullJSONItem()
  case let value as NSNumber where isBoolean(value):
    return BooleanJSONItem(value.boolValue)
  case let value as NSNumber where isInteger(value):
    return IntegerJSONItem.fromInt64(value.int64Value)
  case let value as NSNumber where !isInteger(value):
    return DoubleJSONItem(value.doubleValue)
  case let value as String:
    return StringJSONItem(value)
  case let value as [Any?]:
    return value.compactMap(JSONItemFromAny).reduce(into: ArrayJSONItem()) { $0.add($1) }
  case let value as [String: Any?]:
    return value.compactMapValues(JSONItemFromAny).reduce(into: MapJSONItem()) { $0.put($1.key, $1.value) }
  default:
    // SimpleLogger.instance().error("Unsupported type for converting to JSONItem: \(type(of: value))")
    return nil
  }
}

extension JSONItem {
  public func toAny() -> Any? {
    switch kind {
      case .integer: return (self as! IntegerJSONItem).asInt64()
      case .double: return (self as! DoubleJSONItem).value
      case .string: return (self as! StringJSONItem).value
      case .boolean: return (self as! BooleanJSONItem).value
      case .nullItem: return NSNull()
      case .array: return (self as! ArrayJSONItem).asArray().items.map { $0.toAny() }
      case .map: return (self as! MapJSONItem).asMap().reduce(into: [String: Any?]()) { $0[$1.key] = $01.value.toAny() }
    }
  }
}

public func isBoolean(_ value: NSNumber) -> Bool {
  return CFBooleanGetTypeID() == CFGetTypeID(value)
}

public func isInteger(_ value: NSNumber) -> Bool {
  return Double(value.int64Value) == value.doubleValue
}
