// <<< AUTOGENERATED BY YANDEX.SCRIPT FROM client/requests/message-body-request.ts >>>

import Foundation

open class MessageBodyRequest: BaseNetworkRequest {
  private let mids: YSArray<ID>
  public init(_ platform: Platform, _ networkExtra: NetworkExtra, _ mids: YSArray<ID>) {
    self.mids = mids
    super.init(platform, networkExtra)
  }

  @discardableResult
  open override func version() -> NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }

  @discardableResult
  open override func method() -> NetworkMethod {
    return NetworkMethod.post
  }

  @discardableResult
  open override func path() -> String {
    return "message_body"
  }

  @discardableResult
  open override func encoding() -> RequestEncoding {
    return JsonRequestEncoding()
  }

  @discardableResult
  open override func params() -> NetworkParams {
    return MapJSONItem().putBoolean("novdirect", true).putString("mids", mids.map {
      mid in
      idToString(mid)
    }.join(","))
  }
}