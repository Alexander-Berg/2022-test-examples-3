//
//  RequestEncoding.swift
//  XMail
//
//  Created by Aleksandr A. Dvornikov on 116//19.
//

import Foundation

private enum Constants {
  static let methodsEncodedInUrl: Set = ["GET", "HEAD", "DELETE"]
}

protocol RequestEncoder {
  func encode(request: URLRequest, with params: NetworkParams) -> URLRequest?
}

class UrlRequestEncoder: RequestEncoder {
  func encode(request: URLRequest, with params: NetworkParams) -> URLRequest? {
    var request = request
    guard let httpMethod = request.httpMethod, let url = request.url else {
      return nil
    }

    let encodesParametersInUrl = Constants.methodsEncodedInUrl.contains(httpMethod)
    if encodesParametersInUrl {
      guard var urlComponents = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
        return nil
      }
      var items = urlComponents.queryItems ?? []
      items.append(contentsOf: queryItems(fromParams: params))
      urlComponents.queryItems = items
      request.url = urlComponents.url
    } else {
      request.httpBody = Data(formEncodedQuery(with: params).utf8)
      request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
    }

    return request
  }

  private func formEncodedQuery(with params: NetworkParams) -> String {
    return queryItems(fromParams: params)
      .map { "\(escape($0.name))=\(escape($0.value!))" }
      .joined(separator: "&")
  }

  private func escape(_ string: String) -> String {
    return string.addingPercentEncoding(withAllowedCharacters: .urlQueryItemAllowed) ?? string
  }
}

class JsonRequestEncoder: RequestEncoder {
  func encode(request: URLRequest, with params: NetworkParams) -> URLRequest? {
    var request = request
    request.httpBody = jsonBody(with: params)
    request.setValue("application/json", forHTTPHeaderField: "Content-Type")
    return request
  }

  private func jsonBody(with params: NetworkParams) -> Data? {
    guard params.asMap().size > 0 else { return nil }
    #if DEBUG
    let jsonWritingOptions: JSONSerialization.WritingOptions = [.prettyPrinted]
    #else
    let jsonWritingOptions: JSONSerialization.WritingOptions = []
    #endif
    do {
      return try JSONSerialization.data(withJSONObject: params.toAny()!, options: jsonWritingOptions)
    } catch let error as NSError {
      // SimpleLogger.instance().error("Request body creation failed with error \(error)")
    }
    return nil
  }
}

func encodeRequest(_ request: URLRequest, with encoding: RequestEncoding, params: NetworkParams) -> URLRequest? {
  let encoder: RequestEncoder
  switch encoding.kind {
  case .url:
    encoder = UrlRequestEncoder()
  case .json:
    encoder = JsonRequestEncoder()
  }
  return encoder.encode(request: request, with: params)
}

extension CharacterSet {
  /// Via https://github.com/Alamofire/Alamofire/blob/5.0.0-beta.6/Source/ParameterEncoder.swift#L795-L813
  public static let urlQueryItemAllowed: CharacterSet = {
    let generalDelimitersToEncode = ":#[]@"
    let subDelimitersToEncode = "!$&'()*+,;="
    let encodableDelimiters = CharacterSet(charactersIn: "\(generalDelimitersToEncode)\(subDelimitersToEncode)")

    return CharacterSet.urlQueryAllowed.subtracting(encodableDelimiters)
  }()
}
