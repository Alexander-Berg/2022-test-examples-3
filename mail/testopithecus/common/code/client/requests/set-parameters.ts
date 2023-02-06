import { MapJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  NetworkAPIVersions,
  NetworkMethod,
  RequestEncoding,
  UrlRequestEncoding,
} from '../network/network-request';

export class SetParametersRequest extends BaseNetworkRequest {
  constructor(private key: string, private value: string, platform: Platform, networkExtra: NetworkExtra) {
    super(platform, networkExtra)
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding();
  }

  public method(): NetworkMethod {
    return NetworkMethod.get;
  }

  public params(): MapJSONItem {
    return new MapJSONItem().putString('params', `${this.key}=${this.value}`);
  }

  public path(): string {
    return 'set_parameters';
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1;
  }
}
