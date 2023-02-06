import { ArrayJSONItem, MapJSONItem } from '../../mail/logging/json-types'
import { NetworkExtra } from '../network/network-extra'
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../network/network-request'
import { Platform } from '../../utils/platform'
import { MessageRequestItem } from './message-request-item'

export class MessagesRequestPack extends BaseNetworkRequest {
  public constructor(
    public readonly requests: readonly MessageRequestItem[],
    platform: Platform,
    extra: NetworkExtra,
  ) {
    super(platform, extra)
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }
  public method(): NetworkMethod {
    return NetworkMethod.post
  }
  public path(): string {
    return 'messages'
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
  public params(): NetworkParams {
    return new MapJSONItem().put(
      'requests',
      new ArrayJSONItem(this.requests.map((item) => item.params())),
    )
  }
}
