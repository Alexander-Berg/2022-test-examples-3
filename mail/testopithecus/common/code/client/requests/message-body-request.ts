import { MapJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform'
import { ID, idToString } from '../common/id'
import { NetworkExtra } from '../network/network-extra'
import {
  BaseNetworkRequest, JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../network/network-request'

export class MessageBodyRequest extends BaseNetworkRequest {
  constructor(
    platform: Platform,
    networkExtra: NetworkExtra,
    private readonly mids: readonly ID[],
  ) {
    super(platform, networkExtra)
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }
  public method(): NetworkMethod {
    return NetworkMethod.post
  }
  public path(): string {
    return 'message_body'
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
  public params(): NetworkParams {
    return new MapJSONItem()
      .putBoolean('novdirect', true)
      .putString('mids', this.mids.map((mid) => idToString(mid)).join(','))
  }

}
