import { Int32, Nullable } from '../../ys/ys';
import { JSONSerializer } from '../client/json/json-serializer';
import {
  NetworkAPIVersions,
  NetworkMethod,
  NetworkRequest,
  RequestEncoding,
  UrlRequestEncoding,
} from '../client/network/network-request';
import { SyncNetwork } from '../client/network/sync-network';
import { PublicBackendConfig } from '../client/public-backend-config';
import { Result } from '../client/result';
import { MapJSONItem } from '../mail/logging/json-types'
import { Logger } from '../utils/logger';

export class UserServiceAccount {
  constructor(public login: string, public password: string, public uid: string) {
  }
}

/**
 * Дока https://wiki.yandex-team.ru/test-user-service/
 */
export class UserService {
  public constructor(private network: SyncNetwork, private jsonSerializer: JSONSerializer, private logger: Logger) {
  }

  public getAccount(tag: Nullable<string>, lockDuration: Int32, ignoreLocks: boolean, uidd: Nullable<string>): Nullable<UserServiceAccount> {
    const response = this.syncRequest(new GetAccountRequest(tag, lockDuration, ignoreLocks, uidd))
    const account = response.get('account') as MapJSONItem
    if (account === null) {
      return null
    }
    const login = account.getString('login')
    const password = account.getString('password')
    const uid = account.getString('uid')
    if (login === null || password === null || uid === null) {
      return null
    }
    this.logger.log(`Got account login=${login!} password=${password!} uid=${uid!}`)
    return new UserServiceAccount(login!, password!, uid!)
  }

  public unlockAccount(uid: string): void {
    this.syncRequest(new UnlockAccountRequest(uid))
  }

  private syncRequest(networkRequest: NetworkRequest): MapJSONItem {
    const response = this.network.syncExecute(PublicBackendConfig.userServiceUrl, networkRequest, null)
    const json = this.jsonSerializer.deserialize(response, (item) => new Result(item, null)).getValue()
    return json as MapJSONItem
  }
}

class GetAccountRequest implements NetworkRequest {
  constructor(private tag: Nullable<string>, private lockDuration: Int32, private ignoreLocks: boolean, private uid: Nullable<string>) {
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.get
  }

  public params(): MapJSONItem {
    const params = new MapJSONItem()
      .putString('tus_consumer', 'testopithecus')
      .putInt32('lock_duration', this.lockDuration)
      .putBoolean('ignore_locks', this.ignoreLocks)
    if (this.tag !== null) {
      params.putString('tags', this.tag!)
    }
    if (this.uid !== null) {
      params.putString('uid', this.uid!)
    }
    return params
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.unspecified
  }

  public path(): string {
    return `1/get_account/`
  }
}

class UnlockAccountRequest implements NetworkRequest {
  constructor(private uid: string) {
  }

  public encoding(): RequestEncoding {
    return new UrlRequestEncoding()
  }

  public method(): NetworkMethod {
    return NetworkMethod.post
  }

  public params(): MapJSONItem {
    return new MapJSONItem()
      .putString('uid', this.uid)
  }

  public urlExtra(): MapJSONItem {
    return new MapJSONItem();
  }

  public version(): NetworkAPIVersions {
    return NetworkAPIVersions.unspecified
  }

  public path(): string {
    return `1/unlock_account/`
  }
}
