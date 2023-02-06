import { Nullable, nullthrows, undefinedToNull } from '../../../ys/ys'
import { ArrayJSONItem, JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'
import { NetworkStatus, NetworkStatusCode, networkStatusFromJSONItem } from '../status/network-status'
import { MessageMeta, messageMetaFromJSONItem } from './message-meta'
import { messageResponseHeaderFromJSONItem, MessagesResponseHeader } from './message-response-header'

export class MessageResponse {
  public constructor(
    public readonly status: NetworkStatus,
    public readonly payload: Nullable<readonly MessageResponsePayload[]>,
  ) { }
}

export class MessageResponsePayload {
  public constructor(
    public readonly header: MessagesResponseHeader,
    public readonly items: readonly MessageMeta[],
  ) { }
}

export function messageResponseFromJSONItem(item: JSONItem): Nullable<MessageResponse> {
  if (item.kind === JSONItemKind.array) {
    const array = item as ArrayJSONItem
    return new MessageResponse(
      new NetworkStatus(NetworkStatusCode.ok),
      messageResponsePayloadFromJSONItems(array),
    )
  } else if (item.kind === JSONItemKind.map) {
    const map = item as MapJSONItem
    if (map.hasKey('status')) {
      return new MessageResponse(
        networkStatusFromJSONItem(map.get('status')!)!,
        null,
      )
    }
  }
  return null
}

function messageResponsePayloadFromJSONItems(items: ArrayJSONItem): readonly MessageResponsePayload[] {
  return items.asArray().filter((item) => item.kind === JSONItemKind.map).map((item) => {
    const map = item as MapJSONItem
    const header = messageResponseHeaderFromJSONItem(map.get('header')!)
    const messages = undefinedToNull(nullthrows(map.getMap('messageBatch')).get('messages'))
    return new MessageResponsePayload(
      header !== null ? header : MessagesResponseHeader.withError(-1),
      messages !== null ? messageMetasFromJSONItem(messages as ArrayJSONItem) : [],
    )
  })
}

function messageMetasFromJSONItem(items: ArrayJSONItem): readonly MessageMeta[] {
  const result: MessageMeta[] = []
  for (const item of items.asArray()) {
    const message = messageMetaFromJSONItem(item as MapJSONItem)
    if (message !== null) {
      result.push(message)
    }
  }
  return result
}
