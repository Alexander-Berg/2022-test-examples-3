//
//  DefaultNetworkFetch.swift
//  XMail
//
//  Created by Dmitry Zakharov.
//  Copyright © 2018 Yandex. All rights reserved.
//

import Foundation

public final class DefaultSyncNetwork: NSObject, SyncNetwork {
    private let logger: Logger

    private lazy var decoder: JSONDecoder = JSONDecoder()

    public init(logger: Logger) {
        self.logger = logger
    }

    public func syncExecute(_ baseUrl: String, _ request: NetworkRequest, _ oauthToken: String!) -> String {
        guard let httpRequest = self.buildRequest(baseUrl: baseUrl, request: request, token: oauthToken) else {
            fatalError("Can't build request!")
        }
        logger.log("Performing: \(httpRequest.url?.absoluteString ?? "")")
        let semaphore = DispatchSemaphore(value: 0)
        var result: String? = nil
        let task = URLSession.shared.dataTask(with: httpRequest) { (data, _, _) in
            if let data = data {
                result = String(data: data, encoding: .utf8)
            }
            semaphore.signal()
        }
        task.resume()
        semaphore.wait()
        guard let nonNilResult = result else {
            fatalError("Http request failure!")
        }
        return nonNilResult
    }

    private func buildRequest(baseUrl: String, request: NetworkRequest, token: String?) -> URLRequest? {
        guard let url = url(for: request, baseUrl: baseUrl) else {
            return nil
        }
        var httpRequest = URLRequest(url: url, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: TimeInterval(60))
        if let token = token {
            httpRequest.setValue("OAuth \(token)", forHTTPHeaderField: "Authorization")
        }
        httpRequest.httpMethod = request.method().toString()
        return encodeRequest(httpRequest, with: request.encoding(), params: request.params())
    }

    private func url(for request: NetworkRequest, baseUrl: String) -> URL? {
        let url = fullPath(for: request, baseUrl: baseUrl)
        guard var urlComponents = URLComponents(url: url, resolvingAgainstBaseURL: true) else {
            NSLog("Unable to build URL for \(request.method()) request; V: \(request.version()); P: \(request.path())")
            return nil
        }
        let items = queryItems(fromUrlExtra: request.urlExtra())
        if !items.isEmpty {
            urlComponents.queryItems = items
        }
        return urlComponents.url
    }

    private func fullPath(for request: NetworkRequest, baseUrl: String) -> URL {
        let baseURL = URL(string: baseUrl)!
        return baseURL.appendingPathComponent(request.version().toString()).appendingPathComponent(request.path())
    }
}

func queryItems(fromParams params: NetworkParams) -> [URLQueryItem] {
    return queryItems(fromDictionary: params.asMap().items)
}

func queryItems(fromUrlExtra extra: NetworkUrlExtra) -> [URLQueryItem] {
    return queryItems(fromDictionary: extra.asMap().items)
}

private func queryItems(fromDictionary dictionary: [String: JSONItem]) -> [URLQueryItem] {
    return dictionary.sorted { $0.key < $1.key }.compactMap(queryItem(fromKey:value:))
}

private func queryItem(fromKey key: String, value: JSONItem) -> URLQueryItem? {
    return stringifyQueryParam(value).map { URLQueryItem(name: key, value: $0) }
}

private func stringifyQueryParam(_ value: JSONItem) -> String? {
    switch value.kind {
    case .double:
        return doubleToString((value as! DoubleJSONItem).value)
    case .integer:
        return int64ToString((value as! IntegerJSONItem).asInt64())
    case .boolean:
        return (value as! BooleanJSONItem).value ? "yes" : "no"
    case .string:
        return (value as! StringJSONItem).value
    case .nullItem:
        return "null"
    case .map:
        NSLog("Maps are not supported as URL query request parameters: \(String(describing: value))")
    case .array:
        NSLog("Arrays are not supported as URL query request parameters: \(String(describing: value))")
    }
    return nil
}
