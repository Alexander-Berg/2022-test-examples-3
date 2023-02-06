import { int64ToString, Nullable } from '../../../ys/ys';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { ID } from '../common/id';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest, JsonRequestEncoding, NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding, UrlRequestEncoding,
} from '../network/network-request';

export class DeleteRequest extends BaseNetworkRequest {
  public constructor(
    public readonly mid: Nullable<ID>,
    public readonly tid: Nullable<ID>,
    public readonly fid: ID,
    platform: Platform,
    networkExtra: NetworkExtra,
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
    return 'delete_items';
  }
  public params(): NetworkParams {
    const params = new MapJSONItem();
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    params.put('current_folder', new StringJSONItem(int64ToString(this.fid)));
    return params;
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
}
