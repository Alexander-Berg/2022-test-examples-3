import { int64ToString, Nullable } from '../../../ys/ys';
import { Platform } from '../../utils/platform';
import { ID } from '../common/id';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  NetworkAPIVersions,
  NetworkMethod,
  RequestEncoding,
  UrlRequestEncoding,
} from '../network/network-request';

export class MoveToSpamRequest extends BaseNetworkRequest {
  constructor(private readonly mid: Nullable<ID>,
              private readonly tid: Nullable<ID>,
              private readonly currentFolder: ID,
              platform: Platform,
              networkExtra: NetworkExtra) {
    super(platform, networkExtra);
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post;
  }

  public params(): MapJSONItem {
    const params = new MapJSONItem();
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    params.put('current_folder', new StringJSONItem(int64ToString(this.currentFolder)))
    return params;
  }

  public path(): string {
    return 'foo'
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }

}
