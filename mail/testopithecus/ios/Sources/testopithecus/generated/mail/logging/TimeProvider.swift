// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/time-provider.ts >>>

import Foundation

public protocol TimeProvider {
  @discardableResult
  func getCurrentTimeMs() -> Int64
}

open class NativeTimeProvider: TimeProvider {
  @discardableResult
  open func getCurrentTimeMs() -> Int64 {
    return currentTimeMs()
  }
}
