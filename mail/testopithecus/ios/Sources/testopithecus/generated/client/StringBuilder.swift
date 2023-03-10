// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/string-builder.ts >>>

import Foundation

open class StringBuilder {
  private let strings: YSArray<String> = YSArray()
  private var result: String!
  public init() {}

  @discardableResult
  open func add(_ value: String) -> StringBuilder {
    strings.push(value)
    return self
  }

  @discardableResult
  open func addLine(_ value: String) -> StringBuilder {
    return add(value).add("\n")
  }

  @discardableResult
  open func build() -> String {
    if result != nil {
      return result!
    }
    result = strings.join("")
    return result!
  }
}
