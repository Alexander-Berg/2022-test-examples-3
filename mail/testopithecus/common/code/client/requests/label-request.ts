import { int32ToString, int64ToString, Nullable } from '../../../ys/ys';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { ID, LabelID } from '../common/id';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding,
} from '../network/network-request';

export class LabelRequest extends BaseNetworkRequest {
  public constructor(
    public readonly mark: boolean,
    public readonly mid: Nullable<ID>,
    public readonly tid: Nullable<ID>,
    public readonly lid: LabelID,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(platform, networkExtra)
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1;
  }
  public method(): NetworkMethod {
    return NetworkMethod.post
  }
  public path(): string {
    return 'mark_with_label';
  }
  public params(): NetworkParams {
    const params = new MapJSONItem();
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    params.put('lid', new StringJSONItem(this.lid))
    params.put('mark', new StringJSONItem(int32ToString(this.mark ? 1 : 0)))
    return params;
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
}

export class LabelMarkRequest extends LabelRequest {
  public constructor(
    mid: Nullable<ID>,
    tid: Nullable<ID>,
    lid: LabelID,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(true, mid, tid, lid, platform, networkExtra)
  }
}

export class LabelUnmarkRequest extends LabelRequest {
  public constructor(
    mid: Nullable<ID>,
    tid: Nullable<ID>,
    lid: LabelID,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(false, mid, tid, lid, platform, networkExtra)
  }
}
