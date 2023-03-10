// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM mail/logging/agregation/aggregator.ts >>>

import Foundation

public protocol Aggregator {
  @discardableResult
  func accepts(_ event: TestopithecusEvent) -> Bool
  @discardableResult
  func accept(_ event: TestopithecusEvent) -> TestopithecusEvent!
  @discardableResult
  func finalize() -> TestopithecusEvent!
}

open class EmptyAggregator: Aggregator {
  @discardableResult
  open func accept(_ event: TestopithecusEvent) -> TestopithecusEvent! {
    return event
  }

  @discardableResult
  open func accepts(_: TestopithecusEvent) -> Bool {
    return true
  }

  @discardableResult
  open func finalize() -> TestopithecusEvent! {
    return nil
  }
}
