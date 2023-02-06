//
// Created by Dmitry Zakharov on 30/10/2018.
//

import Foundation

public struct JSONSerializerError: BaseError, Error {
  public let message: String!
  public let inner: Failure!
  private init(message: String, inner: Failure! = nil) {
    self.message = message
    self.inner = inner
  }

  private init(message: String, inner: Error? = nil) {
    self.init(message: message, inner: inner as? Failure)
  }

  public static func unableToSerialize(inner: Error?) -> BaseError {
    return JSONSerializerError(message: "Unable to JSON-serialize object", inner: inner)
  }

  public static func dataToStringConversionFailed() -> BaseError {
    return JSONSerializerError(message: "Unable to convert Data to String", inner: nil as Error?)
  }

  public static func stringToDataConversionFailed() -> BaseError {
    return JSONSerializerError(message: "Unable to convert String to Data", inner: nil as Error?)
  }

  public static func unableToDeserialize(inner: Error) -> BaseError {
    return JSONSerializerError(message: "Unable to JSON-deserialize object", inner: inner)
  }

  public static func badTopLevelObject(kind: JSONItemKind) -> BaseError {
    return JSONSerializerError(message: "Incorrect top level object: \(kind). Only arrays and maps are supported.", inner: nil as Error?)
  }

  public static func materializerFailed(inner: Failure) -> BaseError {
    return JSONSerializerError(message: "Failed materializing object", inner: inner)
  }
}

extension JSONItemKind: CustomStringConvertible {
  public var description: String {
    switch self {
    case .integer: return "integer"
    case .double: return "double"
    case .string: return "string"
    case .boolean: return "boolean"
    case .nullItem: return "null"
    case .map: return "map"
    case .array: return "array"
    }
  }
}
