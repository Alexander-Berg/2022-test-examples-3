//
//  DefaultJSONSerializer.swift
//  XMail
//
//  Created by Dmitry Zakharov on 30/10/2018.
//

import Foundation

public final class DefaultJSONSerializer: JSONSerializer {
  private lazy var encoder: JSONEncoder = JSONEncoder()
  private lazy var decoder: JSONDecoder = JSONDecoder()
  
  public init() {}

  public func serialize(_ item: JSONItem) -> Result<String> {
    let kind = item.kind
    guard kind == .array || kind == .map else { return Result<String>(nil, JSONSerializerError.badTopLevelObject(kind: kind)) }
    let transformed = transform(item: item)
    #if DEBUG
      let options: JSONSerialization.WritingOptions = [.prettyPrinted]
    #else
      let options: JSONSerialization.WritingOptions = []
    #endif
    do {
      return try Result(String(data: JSONSerialization.data(withJSONObject: transformed, options: options), encoding: .utf8), nil)
    } catch {
      return Result(nil, JSONSerializerError.unableToSerialize(inner: error))
    }
  }

  public func deserialize<T>(_ item: String, _ materializer: @escaping (JSONItem) -> Result<T>) -> Result<T> {
    let data = item.data(using: .utf8)!
    do {
      let object = try JSONSerialization.jsonObject(with: data, options: .allowFragments)
      let item = reverse(item: object)
      if item.isError() {
        return Result(nil, item.getError())
      }
      let result = item.withValue(materializer)!
      if result.isError() {
        return Result(nil, JSONSerializerError.materializerFailed(inner: result.getError()))
      }
      return result
    } catch {
        return Result(nil, JSONSerializerError.unableToDeserialize(inner: error))
    }
  }

  public func serializeEncodable<T: Encodable>(_ item: T) -> Result<String> {
    do {
      guard let string = try String(data: encoder.encode(item), encoding: .utf8) else {
        return Result(nil, JSONSerializerError.dataToStringConversionFailed())
      }
      return Result(string, nil)
    } catch {
        return Result(nil, JSONSerializerError.unableToSerialize(inner: error))
    }
  }

  public func deserializeDecodable<T: Decodable>(_ runtimeClassInfo: RuntimeClassInfo, _ item: String) -> Result<T> {
    guard let data = item.data(using: .utf8) else { return Result(nil, JSONSerializerError.stringToDataConversionFailed()) }
    do {
      return try Result(decoder.decode(T.self, from: data), nil)
    } catch {
        return Result(nil, JSONSerializerError.unableToDeserialize(inner: error))
    }
  }

  private func transform(item: JSONItem) -> Any {
    switch item.kind {
    case .integer: return transform(int: item as! IntegerJSONItem)
    case .double: return transform(double: item as! DoubleJSONItem)
    case .string: return transform(string: item as! StringJSONItem)
    case .boolean: return transform(bool: item as! BooleanJSONItem)
    case .nullItem: return transform(nullValue: item as! NullJSONItem)
    case .array: return transform(array: item as! ArrayJSONItem)
    case .map: return transform(map: item as! MapJSONItem)
    }
  }

  private func transform(map: MapJSONItem) -> NSDictionary {
    let result = NSMutableDictionary()
    for (key, value) in map.asMap() {
      result[key] = transform(item: value)
    }
    return result
  }

  private func transform(array: ArrayJSONItem) -> NSArray {
    let result = NSMutableArray()
    for item in array.asArray() {
      result.add(transform(item: item))
    }
    return result
  }

  private func transform(int: IntegerJSONItem) -> NSNumber {
    return NSNumber(value: int.asInt64())
  }

  private func transform(double: DoubleJSONItem) -> NSNumber {
    return NSNumber(value: double.value)
  }

  private func transform(string: StringJSONItem) -> NSString {
    return NSString(string: string.value)
  }

  private func transform(bool: BooleanJSONItem) -> NSNumber {
    return NSNumber(value: bool.value)
  }

  private func transform(nullValue _: NullJSONItem) -> NSNull {
    return NSNull()
  }

  private func reverse(item: Any) -> Result<JSONItem> {
    guard let result = JSONItemFromAny(item) else { return Result(nil, JSONSerializerError.unableToSerialize(inner: nil)) }
    return Result(result, nil)
  }
}
