import Foundation

public final class YSMap<K: Hashable, V>: Sequence {
  public typealias Iterator = DictionaryIterator<K, V>

  public func makeIterator() -> Iterator {
    return items.makeIterator()
  }

  internal var items: [K: V] = [:]

  public init() {}

  public init(items: [K: V]) {
    self.items = items
  }

  @discardableResult
  public func set(_ key: K, _ value: V) -> Self {
    items[key] = value
    return self
  }
  public func get(_ key: K) -> V? {
    return items[key]
  }
  @discardableResult
  public func delete(_ key: K) -> Bool {
    return items.removeValue(forKey: key) != nil
  }

  public var size: Int32 {
    return numericCast(items.count)
  }

  public func keys() -> Dictionary<K, V>.Keys {
    return items.keys
  }

  public func values() -> Dictionary<K, V>.Values {
    return items.values
  }

  public func clear() {
    items.removeAll()
  }

  public func has(_ key: K) -> Bool {
    return items[key] != nil
  }

  public func __forEach(_ callback: (_ value: V, _ key: K) -> Void) {
    items.forEach { entry in callback(entry.value, entry.key) }
  }
}

extension YSMap: Decodable where K: Decodable, V:Decodable {
  public convenience init(from decoder: Decoder) throws {
    self.init()
    self.items = try Dictionary.init(from: decoder)
  }
}

extension YSMap: Encodable where K: Encodable, V: Encodable {
  public func encode(to encoder: Encoder) throws {
    try self.items.encode(to: encoder)
  }
}

extension YSMap: CustomStringConvertible, CustomDebugStringConvertible {
  public var description: String {
    return self.items.description
  }
  public var debugDescription: String {
    return self.items.debugDescription
  }
}
