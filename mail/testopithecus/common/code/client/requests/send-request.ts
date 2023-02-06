import { Platform } from '../../utils/platform';
import { MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { NetworkExtra } from '../network/network-extra';
import {
  BaseNetworkRequest, JsonRequestEncoding,
  NetworkAPIVersions,
  NetworkMethod,
  NetworkParams,
  RequestEncoding, } from '../network/network-request';

export class SendRequest extends BaseNetworkRequest {
  public constructor(
    public readonly networkParams: NetworkParams,
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
    return 'send'
  }
  public params(): NetworkParams {
    return this.networkParams;
  }
  public encoding(): RequestEncoding {
    return new JsonRequestEncoding()
  }
}

export class SendRequestBuilder {

  constructor(private platform: Platform, private extra: NetworkExtra) { }

  private params: NetworkParams = new MapJSONItem();

  public subject(value: string): SendRequestBuilder {
    return this.addStringParam('subj', value);
  }

  public to(value: string): SendRequestBuilder {
    return this.addStringParam('to', value);
  }

  public composeCheck(value: string): SendRequestBuilder {
    return this.addStringParam('compose_check', value);
  }

  public send(value: string): SendRequestBuilder {
    return this.addStringParam('send', value);
  }

  public inReplyTo(value: string): SendRequestBuilder {
    return this.addStringParam('inreplyto', value);
  }

  public references(value: string): SendRequestBuilder {
    return this.addStringParam('references', value);
  }

  private addStringParam(name: string, value: string): SendRequestBuilder {
    this.params.put(name, new StringJSONItem(value));
    return this
  }

  public build(): SendRequest {
    return new SendRequest(this.params, this.platform, this.extra)
  }

}
