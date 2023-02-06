import { Nullable } from '../../../ys/ys';
import { NetworkRequest } from './network-request'

export interface SyncNetwork {
  syncExecute(baseUrl: string, request: NetworkRequest, oauthToken: Nullable<string>): string
}
