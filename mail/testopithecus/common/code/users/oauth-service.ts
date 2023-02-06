import { JSONSerializer } from '../client/json/json-serializer';
import { MapJSONItem } from '../mail/logging/json-types'
import {
  NetworkAPIVersions,
  NetworkMethod, NetworkRequest,
  RequestEncoding,
  UrlRequestEncoding,
} from '../client/network/network-request';
import { SyncNetwork } from '../client/network/sync-network';
import { PublicBackendConfig } from '../client/public-backend-config';
import { Result } from '../client/result';
import { requireNonNull } from '../utils/utils';
import { UserAccount } from './user-pool';

export class OauthService {
  constructor(private network: SyncNetwork, private jsonSerializer: JSONSerializer) {
  }

  public getToken(account: UserAccount): string {
    const response = this.network.syncExecute(PublicBackendConfig.oauthUrl, new TokenRequest(account), null)
    const json = this.jsonSerializer.deserialize(response, (item) => new Result(item, null)).getValue()
    return requireNonNull((json as MapJSONItem).getString('access_token'), 'No access_token!')
  }
}

class TokenRequest implements NetworkRequest {
  constructor(private account: UserAccount) {
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('grant_type', 'password')
      .putString('username', this.account.login)
      .putString('password', this.account.password)
      .putString('client_id', 'e7618c5efed842be839cc9a580be94aa')
      .putString('client_secret', '81a97a4e05094a4c96e9f5fa0b21f794')
  }

  public path(): string {
    return 'token';
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.unspecified
  }
}
