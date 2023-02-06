import { int64ToString, Nullable } from '../../../ys/ys';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { ID } from '../common/id';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  NetworkAPIVersions,
  NetworkMethod,
  RequestEncoding,
  UrlRequestEncoding,
} from '../network/network-request';

export class ArchiveRequest extends BaseNetworkRequest {
  constructor(private readonly local: string,
              private readonly mid: Nullable<ID>,
              private readonly tid: Nullable<ID>,
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
    params.put('local', new StringJSONItem(this.local))
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    return params;
  }

  public path(): string {
    return 'archive'
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1
  }

}
