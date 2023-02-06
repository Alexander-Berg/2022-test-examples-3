import { MapJSONItem } from '../../mail/logging/json-types'
import { Platform, platformToClient } from '../../utils/platform'
import { NetworkExtra } from './network-extra'

export type NetworkParams = MapJSONItem
export type NetworkUrlExtra = MapJSONItem

export const enum NetworkAPIVersions {
  v1 = 'v1',
  v2 = 'v2',
  v3 = 'v3',
  unspecified = '',
}

export const enum NetworkMethod {
  get = 'get',
  post = 'post',
}

export enum RequestEncodingKind {
  url,
  json,
}

/** A type that configures how `params` should be encoded when executing `NetworkRequest` */
export interface RequestEncoding {
  readonly kind: RequestEncodingKind
}

/**
 * A `RequestEncoding` that encodes types as:
 *  * URL-encoded query strings to be set on the URL - for "GET", "HEAD" & "DELETE" requests
 *  * As form-encoded body data for all the remaining methods (ex. "POST")
 */
export class UrlRequestEncoding implements RequestEncoding {
  public readonly kind: RequestEncodingKind = RequestEncodingKind.url
}

/** A `RequestEncoding` that encodes types as JSON body data. */
export class JsonRequestEncoding implements RequestEncoding {
  public readonly kind: RequestEncodingKind = RequestEncodingKind.json
}

export interface NetworkRequest {
  version(): NetworkAPIVersions
  method(): NetworkMethod
  path(): string
  params(): NetworkParams
  urlExtra(): NetworkUrlExtra
  encoding(): RequestEncoding
}

export abstract class BaseNetworkRequest implements NetworkRequest {
  public constructor(
    public readonly platform: Platform,
    public readonly networkExtra: NetworkExtra,
  ) { }

  public abstract version(): NetworkAPIVersions
  public abstract method(): NetworkMethod
  public abstract path(): string
  public abstract params(): NetworkParams

  public urlExtra(): NetworkUrlExtra {
    const result = new MapJSONItem();
    result
      .putString('client', platformToClient(this.platform))
      .putString('app_state', this.networkExtra.foreground ? 'foreground' : 'background')
      .putString('uuid', this.networkExtra.uuid);
    return result
  }

  public abstract encoding(): RequestEncoding
}
