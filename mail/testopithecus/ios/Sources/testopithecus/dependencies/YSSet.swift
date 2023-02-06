import Foundation

public final class YSSet<T: Hashable>: Sequence {
  public typealias Iterator = Set<T>.Iterator

  public __consuming func makeIterator() -> Set<T>.Iterator {
    return items.makeIterator()
  }

  internal var items = Set<T>()

  public var size: Int32 {
    return numericCast(items.count)
  }

  public init() {}

  public init<K: Sequence>(_ items: K) where K.Element == T {
    self.items = Set<T>(items)
  }

  public func add(_ value: T) -> YSSet<T> {
    items.insert(value)
    return self
  }

  public func has(_ value: T) -> Bool {
    return items.contains(value)
  }

  public func values() -> AnySequence<T> {
    return AnySequence(items)
  }

  @discardableResult
  public func delete(_ item: T) -> Bool {
    return items.remove(item) != nil
  }
}

extension YSSet: CustomStringConvertible, CustomDebugStringConvertible {
  public var description: String {
    return self.items.description
  }
  public var debugDescription: String {
    return self.items.debugDescription
  }
}

func ==<T: Hashable>(lhs: YSSet<T>, rhs: YSSet<T>) -> Bool {
  return lhs.items == rhs.items
}
