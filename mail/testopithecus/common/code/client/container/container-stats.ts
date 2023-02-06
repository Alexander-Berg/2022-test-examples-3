import { Int64, Nullable } from '../../../ys/ys'
import { JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'
import { NetworkStatus, NetworkStatusCode, networkStatusFromJSONItem } from '../status/network-status'

export class ContainerStatsPayload {
  public constructor(
    public readonly md5: string,
    public readonly mailboxRevision: Int64,
  ) { }
}

export class ContainerStats {
  public constructor(
    public readonly status: NetworkStatus,
    public readonly payload: Nullable<ContainerStatsPayload>,
  ) { }
}
export function containerStatsFromJSONItem(item: JSONItem): Nullable<ContainerStats> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const status = networkStatusFromJSONItem(map.get('status')!)!
  if (status.code !== NetworkStatusCode.ok) {
    return new ContainerStats(status, null)
  }
  const md5 = map.getString('md5')!
  const mailboxRevision = map.getInt64('mailbox_revision')!
  return new ContainerStats(
    status,
    new ContainerStatsPayload(md5, mailboxRevision),
  )
}
