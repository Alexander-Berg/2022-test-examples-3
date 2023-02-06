// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM users/oauth-service.ts >>>

import Foundation

open class OauthService {
  private var network: SyncNetwork
  private var jsonSerializer: JSONSerializer
  public init(_ network: SyncNetwork, _ jsonSerializer: JSONSerializer) {
    self.network = network
    self.jsonSerializer = jsonSerializer
  }

  @discardableResult
  open func getToken(_ account: UserAccount) -> String {
    let response = network.syncExecute(PublicBackendConfig.oauthUrl, TokenRequest(account), nil)
    let json = jsonSerializer.deserialize(response) {
      item in
      Result(item, nil)
    }.getValue()
    return requireNonNull((json as! MapJSONItem).getString("access_token"), "No access_token!")
  }
}

private class TokenRequest: NetworkRequest {
  private var account: UserAccount
  public init(_ account: UserAccount) {
    self.account = account
  }

  @discardableResult
  public func encoding() -> RequestEncoding {
    return UrlRequestEncoding()
  }

  @discardableResult
  public func method() -> NetworkMethod {
    return NetworkMethod.post
  }

  @discardableResult
  public func params() -> MapJSONItem {
    return MapJSONItem().putString("grant_type", "password").putString("username", account.login).putString("password", account.password).putString("client_id", "e7618c5efed842be839cc9a580be94aa").putString("client_secret", "81a97a4e05094a4c96e9f5fa0b21f794")
  }

  @discardableResult
  public func path() -> String {
    return "token"
  }

  @discardableResult
  public func urlExtra() -> MapJSONItem {
    return MapJSONItem()
  }

  @discardableResult
  public func version() -> NetworkAPIVersions {
    return NetworkAPIVersions.unspecified
  }
}