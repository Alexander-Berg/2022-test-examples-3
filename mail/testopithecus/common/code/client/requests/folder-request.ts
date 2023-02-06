import { int64ToString, Nullable } from '../../../ys/ys';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { Platform } from '../../utils/platform';
import { ID } from '../common/id';
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest,
  JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  RequestEncoding,
} from '../network/network-request';

export abstract class FolderRequest extends BaseNetworkRequest {
  protected constructor(platform: Platform, networkExtra: NetworkExtra) {
    super(platform, networkExtra);
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.v1;
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
}

export class MoveToFolderRequest extends FolderRequest {
  public constructor(
    public readonly mid: Nullable<ID>,
    public readonly tid: Nullable<ID>,
    public readonly fid: ID,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(platform, networkExtra)
  }

  public params(): MapJSONItem {
    const params = new MapJSONItem();
    if (this.mid !== null) {
      params.put('mids', new StringJSONItem(int64ToString(this.mid)))
    }
    if (this.tid !== null) {
      params.put('tids', new StringJSONItem(int64ToString(this.tid)))
    }
    params.put('fid', new StringJSONItem(int64ToString(this.fid)));
    return params;
  }

  public path(): string {
    return 'move_to_folder';
  }

}

export class CreateFolderRequest extends FolderRequest {
  public constructor(
    public readonly name: string,
    public readonly parentFid: Nullable<ID>,
    public readonly symbol: Nullable<string>,
    platform: Platform,
    networkExtra: NetworkExtra,
  ) {
    super(platform, networkExtra)
  }

  public params(): MapJSONItem {
    const params = new MapJSONItem();
    if (this.parentFid !== null) {
      params.put('parent_fid', new StringJSONItem(int64ToString(this.parentFid)))
    }
    if (this.symbol !== null) {
      params.put('symbol', new StringJSONItem(this.symbol))
    }
    params.put('name', new StringJSONItem(this.name))
    return params;
  }

  public path(): string {
    return 'create_folder';
  }
}
