// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM users/user-pool.ts >>>

import Foundation

open class UserAccount {
  public var login: String
  public var password: String
  public init(_ login: String, _ password: String) {
    self.login = login
    self.password = password
  }
}

open class OAuthUserAccount {
  public var account: UserAccount
  public var oauthToken: String
  public init(_ account: UserAccount, _ oauthToken: String) {
    self.account = account
    self.oauthToken = oauthToken
  }
}

public protocol UserLock {
  @discardableResult
  func lockedAccount() -> UserAccount
  func ping(_ newLockTtlMs: Int64) -> Void
  func release() -> Void
}

public protocol UserPool {
  @discardableResult
  func tryAcquire(_ tryAcquireTimeoutMs: Int64, _ lockTtlMs: Int64) -> UserLock!
  func reset() -> Void
}