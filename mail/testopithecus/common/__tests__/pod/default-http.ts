import * as syncRequest from 'sync-request';
import { HttpVerb, Options } from 'sync-request';
import { JSONSerializer } from '../../code/client/json/json-serializer';
import { MapJSONItem, StringJSONItem } from '../../code/mail/logging/json-types'
import {
  NetworkAPIVersions,
  NetworkMethod,
  NetworkRequest,
  RequestEncodingKind,
} from '../../code/client/network/network-request';
import { SyncNetwork } from '../../code/client/network/sync-network';
import { Logger } from '../../code/utils/logger';
import { Nullable } from '../../ys/ys';

export class DefaultSyncNetwork implements SyncNetwork {

  constructor(private jsonSerializer: JSONSerializer, private logger: Logger) {
  }

  public syncExecute(baseUrl: string, networkRequest: NetworkRequest, oauthToken: Nullable<string>): string {
    const opts = this.encodeRequest(networkRequest, oauthToken);
    const fullUrl = this.buildUrl(baseUrl, networkRequest);
    this.logger.log(`${DefaultSyncNetwork.requestToString(networkRequest.method(), fullUrl, opts)}`);
    const response = syncRequest.default(DefaultSyncNetwork.toHttpVerb(networkRequest.method()), fullUrl, opts);
    const body = response.body.toString();
    if (body.length === 0) { // TODO: get json response from Imap Sync
      return ''
    }
    let parsed: any
    try {
      parsed = JSON.parse(body);
    } catch (e) {
      throw new Error(`Bad response body ${body}`)
    }
    if (!(parsed instanceof Array)) {
      if (parsed.status && parsed.status !== 1 && parsed.status !== 'ok' && parsed.status.status && parsed.status.status !== 1) {
        throw new Error(`Bad response status ${body}`)
      }
    } else {
      const status = parsed[0].status;
      if (status && status.status !== 1) {
        throw new Error(`Bad response status ${status.phrase}`)
      }
    }
    return response.body.toString();
  }

  private buildUrl(baseUrl: string, req: NetworkRequest): string {
    const version = req.version() !== NetworkAPIVersions.unspecified ? `/${req.version().valueOf()}` : ''
    const url = `${baseUrl}${version}/${req.path()}`
    return `${url}?${DefaultSyncNetwork.buildUrlParams(req)}`
  }

  private encodeRequest(request: NetworkRequest, oauthToken: Nullable<string>) {
    const opts: Options = {
      retry: true,
    }
    opts.headers = {
      'Content-type': 'application/json',
      'User-Agent': 'testopithecus',
    };
    if (oauthToken) {
      opts.headers.Authorization = `OAuth ${oauthToken}`
    }
    if (request.encoding().kind === RequestEncodingKind.url) {
      if (request.path() === 'token') { // TODO
        opts.body = DefaultSyncNetwork.buildUrlParams(request)
        return opts
      }
      return opts
    }
    const json = this.jsonSerializer.serialize(request.params()).getValue();
    opts.json = JSON.parse(json);
    return opts;
  }

  private static buildUrlParams(request: NetworkRequest): string {
    const args: string[] = [];
    DefaultSyncNetwork.addUrlParams(args, request.urlExtra());
    if (request.encoding().kind === RequestEncodingKind.url) {
      DefaultSyncNetwork.addUrlParams(args, request.params());
    }
    if (args.length === 0) {
      return '';
    }
    return args.join('&');
  }

  private static addUrlParams(args: string[], params: MapJSONItem) {
    const map = params.asMap();
    for (const key of map.keys()) {
      args.push(key + '=' + (map.get(key) as StringJSONItem).value)
    }
  }

  private static requestToString(method: NetworkMethod, fullUrl: string, opts: Options): string {
    const data = opts.json ? JSON.stringify(opts.json) : opts.body
    const dataString = data ? `-d '${data}'` : ''
    const headers = opts.headers || {};
    let result = `curl -X ${DefaultSyncNetwork.toHttpVerb(method)} '${fullUrl}' ${dataString}`;
    for (const key of Object.keys(headers)) {
      result += ` -H '${key}: ${headers[key]}'`
    }
    return result;
  }

  private static toHttpVerb(method: NetworkMethod): HttpVerb {
    switch (method) {
      case NetworkMethod.get:
        return 'GET';
      case NetworkMethod.post:
        return 'POST';
    }
  }

}
