//
//  Extra.swift
//  XMail
//
//  Created by Dmitry Zakharov.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation

public final class YSArray<T>: Sequence, ExpressibleByArrayLiteral {
  internal var items: [T] = []

  public typealias Iterator = Array<T>.Iterator
  public func makeIterator() -> YSArray<T>.Iterator {
    return items.makeIterator()
  }

  public init() {}

  public init(_ array: YSArray<T>) {
    self.items = array.items
  }
  public init(_ items: T...) {
    self.items = items
  }
  public init(arrayLiteral elements: T...) {
    self.items = elements
  }

  public init(array: [T]) {
    items = array
  }

  @discardableResult
  public func pop() -> T? {
    return items.popLast()
  }

  public func push(_ item: T) {
    items.append(item)
  }

  public var length: Int32 {
    return numericCast(items.count)
  }

  public subscript(index: Int) -> T {
    return items[index]
  }

  public subscript(index: Int32) -> T {
    get {
      return items[numericCast(index)]
    }
    set (newValue) {
      items[numericCast(index)] = newValue
    }
  }

  public subscript(index: Int64) -> T {
    return items[numericCast(index)]
  }

  public func map<U>(_ f: (T) -> U) -> YSArray<U> {
    return YSArray<U>(array: items.map(f))
  }

  public func filter(_ f: (T) -> Bool) -> YSArray<T> {
    return YSArray(array: items.filter(f))
  }

  public func slice(_ start: Int32 = 0, _ end: Int32? = nil) -> YSArray<T> {
    let length = items.count
    let realStart = start >= 0 ? Int(start) : Swift.max(0, length + Int(start))
    var realEnd = length
    if let end = end.map(Int.init) {
      realEnd = end >= 0 ? Swift.min(end, length) : length + end
    }
    guard realStart < realEnd else {
      return []
    }
    return YSArray(array: [T](items[realStart..<realEnd]))
  }

  public func concat(_ items: YSArray<T>) -> YSArray<T> {
    var copy = [T](self.items)
    copy.append(contentsOf: items.items)
    return YSArray<T>(array: copy)
  }

  internal var _items: [T] {
    return items
  }

  public func reduce<R>(_ f: (R, T) -> R, _ seed: R) -> R {
    return self.reduce(seed, f)
  }

  public func sort(_ comparator: (T, T) -> Int32) -> YSArray {
    self.items.sort { (a, b) -> Bool in
      return comparator(a, b) < 0
    }
    return self
  }

  @discardableResult
  public func reverse() -> YSArray {
    return YSArray(array: self.reversed())
  }
}

extension YSArray: Decodable where T: Decodable {
  public convenience init(from decoder: Decoder) throws {
    let container = try decoder.singleValueContainer()
    self.init(array: try container.decode([T].self))
  }
}

extension YSArray: Encodable where T: Encodable {
  public func encode(to encoder: Encoder) throws {
    var container = encoder.singleValueContainer()
    try container.encode(items)
  }
}

extension YSArray where T == String {
  public func join(_ separator: String) -> String {
    return items.joined(separator: separator)
  }
}

extension YSArray where T: Equatable {
  public func lastIndexOf(_ item: T) -> Int32 {
    return numericCast(items.lastIndex(of: item) ?? -1)
  }
  public func includes(_ item: T) -> Bool {
    return self.contains(item)
  }
}

extension YSArray: CustomStringConvertible, CustomDebugStringConvertible {
  public var description: String {
    return self.items.description
  }
  public var debugDescription: String {
    return self.items.debugDescription
  }
}
