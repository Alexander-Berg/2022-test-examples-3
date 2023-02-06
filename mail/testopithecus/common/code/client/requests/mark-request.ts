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

export class MarkRequest extends BaseNetworkRequest {
  public constructor(
    private readonly action: string,
    public readonly mid: Nullable<ID>,
    public readonly tid: Nullable<ID>,
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
    return this.action;
  }
  public params(): NetworkParams {
    const params = new MapJSONItem();
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    return params;
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
}

export class MarkReadRequest extends MarkRequest {
  public constructor(
    mid: Nullable<ID>,
    tid: Nullable<ID>,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super('mark_read', mid, tid, platform, networkExtra)
  }
}

export class MarkUnreadRequest extends MarkRequest {
  public constructor(
    mid: Nullable<ID>,
    tid: Nullable<ID>,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super('mark_unread', mid, tid, platform, networkExtra)
  }
}
