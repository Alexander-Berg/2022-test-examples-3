import { Int32 } from '../../../ys/ys';
import { IntegerJSONItem, MapJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../network/network-request';

export class ABookTopRequest extends BaseNetworkRequest {
  public constructor(
    private readonly n: Int32,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(platform, networkExtra)
  }

  public path(): string {
    return 'abook_top';
  }

  public params(): NetworkParams {
    const params = new MapJSONItem();
    params.put('n', IntegerJSONItem.fromInt32(this.n))
    return params;
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }
}
