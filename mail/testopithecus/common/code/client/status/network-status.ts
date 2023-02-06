import { Int32, Nullable } from '../../../ys/ys'
import { JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'

export const enum NetworkStatusCode {
  ok = 1,
  temporaryError = 2,
  permanentError = 3,
}
function networkStatusCodeFromInt32(value: Int32): NetworkStatusCode {
  switch (value) {
    case 1: return NetworkStatusCode.ok
    case 2: return NetworkStatusCode.temporaryError
    case 3: return NetworkStatusCode.permanentError
    default: return NetworkStatusCode.temporaryError
  }
}

export class NetworkStatus {
  public constructor(
    public readonly code: NetworkStatusCode,
    public readonly trace: Nullable<string> = null,
    public readonly phrase: Nullable<string> = null,
  ) { }
}

export function networkStatusFromJSONItem(item: JSONItem): Nullable<NetworkStatus> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const statusCode = networkStatusCodeFromInt32(map.getInt32('status')!)
  const trace = map.getString('trace')
  const phrase = map.getString('phrase')
  return new NetworkStatus(statusCode, trace, phrase)
}
