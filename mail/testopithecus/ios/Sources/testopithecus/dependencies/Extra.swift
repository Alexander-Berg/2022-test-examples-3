//
//  Extra.swift
//  XMail
//
//  Created by Dmitry Zakharov.
//  Copyright © 2018 Yandex. All rights reserved.
//

public func cast<T, U>(_ array: YSArray<T>) -> YSArray<U> {
  return YSArray<U>(array.map { item in item as! U })
}

public func castToAny<T>(_ value: T) -> Any {
  return value
}

public func int64<T: SignedInteger>(_ value: T) -> Int64 {
  return numericCast(value)
}

public func int32ToInt64(_ value: Int32) -> Int64 {
  return numericCast(value)
}

public func int64ToInt32(_ value: Int64) -> Int32 {
  return Int32(truncatingIfNeeded: value)
}

public func int64ToDouble(_ value: Int64) -> Double {
  return Double(value)
}

public func stringToInt32(_ value: String, _ radix: Int32 = 10) -> Int32? {
  return Int32(value, radix: numericCast(radix))
}

public func stringToInt64(_ value: String, _ radix: Int32 = 10) -> Int64? {
  return Int64(value, radix: numericCast(radix))
}

public func stringToDouble(_ value: String) -> Double? {
  return Double(value)
}

public func int64ToString(_ value: Int64) -> String {
    return value.description
}

public func int32ToString(_ value: Int32) -> String {
    return value.description
}

public func doubleToString(_ value: Double) -> String {
    return value.description
}

public func doubleToInt32(_ value: Double) -> Int32 {
  return Int32(value)
}

public func doubleToInt64(_ value: Double) -> Int64 {
  return Int64(value)
}

public func booleanToInt32(_ value: Bool) -> Int32 {
  return value ? 1 : 0
}

public func int32ToBoolean(_ value: Int32) -> Bool {
  return value != 0
}

public func defaultValue() {
  return
}

public func undefinedToNull<T>(_ value: T?) -> T? {
  return value
}

public func nullthrows<T>(_ value: T?) -> T {
  guard let value = value else {
    fatalError("Got unexpected nil")
  }
  return value
}

public func setToArray<T: Hashable>(_ value: YSSet<T>) -> YSArray<T> {
  return YSArray(array: [T](value.items))
}

public func arrayToSet<T: Hashable>(_ value: YSArray<T>) -> YSSet<T> {
  return YSSet(value.items)
}

public func iterableToArray<T: Sequence>(_ value: T) -> YSArray<T.Element> {
  return YSArray(array: Array(value))
}

public func iterableToSet<T: Sequence>(_ value: T) -> YSSet<T.Element> {
  return YSSet(Set(value))
}

public extension Int64 {
  func toString() -> String {
    return description
  }
}

public extension String {
  func startsWith(_ other: String) -> Bool {
    return hasPrefix(other)
  }
  func endsWith(_ other: String) -> Bool {
    return hasSuffix(other)
  }
  func split(_ separator: String) -> YSArray<String> {
    return YSArray(array: separator.isEmpty
      ? map(String.init)
      : components(separatedBy: separator))
  }
  func trim() -> String {
    return trimmingCharacters(in: .whitespacesAndNewlines)
  }
  var length: Int32 {
    return numericCast(count)
  }
  func slice(_ start: Int32 = 0, _ end: Int32? = nil) -> String {
    let length = count
    let realStart = start >= 0 ? Int(start) : Swift.max(0, length + Int(start))
    var realEnd = length
    if let end = end.map(Int.init) {
      realEnd = end >= 0 ? Swift.min(end, length) : length + end
    }
    guard realStart < realEnd else {
      return ""
    }
    let fromIndex = self.index(self.startIndex, offsetBy: realStart)
    let toIndex = self.index(self.startIndex, offsetBy: realEnd)
    return String(self[fromIndex..<toIndex])
  }
  func substring(_ start: Int32 = 0, _ end: Int32? = nil) -> String {
    let len = self.count
    let intStart = Int(start)
    let intEnd = end.map(Int.init) ?? len
    let finalStart = Swift.min(Swift.max(intStart, 0), len)
    let finalEnd = Swift.min(Swift.max(intEnd, 0), len)
    let from = Swift.min(finalStart, finalEnd)
    let to = Swift.max(finalStart, finalEnd)

    let fromIndex = self.index(self.startIndex, offsetBy: from)
    let toIndex = self.index(self.startIndex, offsetBy: to)
    return String(self[fromIndex..<toIndex])
  }
  func substr(_ start: Int32 = 0, _ length: Int32? = nil) -> String {
    let size = self.count
    var intStart = Int(start)
    if (intStart < 0) {
      intStart = Swift.max(size + intStart, 0)
    }
    let end = length.map(Int.init) ?? Int.max
    let resultLength = Swift.min(Swift.max(end, 0), size - intStart)
    if (resultLength <= 0) {
      return ""
    }

    let fromIndex = self.index(self.startIndex, offsetBy: intStart)
    let toIndex = self.index(fromIndex, offsetBy: resultLength)
    return String(self[fromIndex..<toIndex])
  }
  func search(_ regex: String) -> Int {
    guard let index = range(of: regex, options: .regularExpression)?.lowerBound else {
      return -1
    }
    return distance(from: startIndex, to: index)
  }
  func includes(_ s: String) -> Bool {
    return self.contains(s)
  }
  func charCodeAt(_ i: Int32) -> Int32 {
    return self[self.index(self.startIndex, offsetBy: Int(i))].unicodeScalarCodePoint()
  }
  func lastIndexOf(_ symbol: String) -> Int32 {
    guard let i = self.lastIndex(of: Array(symbol)[0]) else {
        return -1
    }
    return Int32(i.encodedOffset)
  }
}

extension Character {
  func unicodeScalarCodePoint() -> Int32 {
    let characterString = String(self)
    let scalars = characterString.unicodeScalars

    return Int32(scalars[scalars.startIndex].value)
  }
}

public class TypeSupport {
  public static func isString(_ value: Any) -> Bool {
    return value is String
  }
  public static func asString(_ value: Any) -> String! {
    return value as? String
  }
  public static func isBoolean(_ value: Any) -> Bool {
    return value is Bool
  }
  public static func asBoolean(_ value: Any) -> Bool! {
    return value as? Bool
  }
  public static func isInt32(_ value: Any) -> Bool {
    return value is Int32
  }
  public static func asInt32(_ value: Any) -> Int32! {
    switch value {
      case let v as Int32: return v
      case let v as Int: return Int32(v)
      default: return value as? Int32
    }
  }
  public static func isInt64(_ value: Any) -> Bool {
    return value is Int64
  }
  public static func asInt64(_ value: Any) -> Int64! {
    return value as? Int64
  }
  public static func isDouble(_ value: Any) -> Bool {
    return value is Double
  }
  public static func asDouble(_ value: Any) -> Double! {
    switch value {
      case let v as Int32: return Double(v)
      case let v as Int: return Double(v)
      default: return value as? Double
    }
  }
}
