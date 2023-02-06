import { MapJSONItem } from '../../mail/logging/json-types'
import { NetworkExtra } from '../network/network-extra'
import {
  BaseNetworkRequest,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
  UrlRequestEncoding,
} from '../network/network-request'
import { Platform } from '../../utils/platform';

export class SettingsRequest extends BaseNetworkRequest {
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
    return 'settings'
  }
  public params(): NetworkParams {
    return new MapJSONItem()
  }
  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }
}
