import { MapJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform'
import { NetworkExtra } from '../network/network-extra'
import {
  BaseNetworkRequest,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
  UrlRequestEncoding,
} from '../network/network-request'

export class ContainersRequest extends BaseNetworkRequest {
  public constructor(
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(platform, networkExtra)
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }
  public method(): NetworkMethod {
    return NetworkMethod.get
  }
  public path(): string {
    return 'xlist'
  }
  public params(): NetworkParams {
    return new MapJSONItem()
  }
  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }
}
