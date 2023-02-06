import { Int32, Nullable } from '../../../ys/ys'
import { JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'

export class MessageResponseHeaderPayload {
  public constructor(
    public readonly md5: string,
    public readonly countTotal: Int32,
    public readonly countUnread: Int32,
    public readonly modified: boolean,
    public readonly batchCount: Int32,
  ) { }
}

export class MessagesResponseHeader {
  private constructor(
    // Error being 1 means no error
    public readonly error: Int32,
    public readonly payload: Nullable<MessageResponseHeaderPayload>,
  ) { }
  public static withError(error: Int32): MessagesResponseHeader {
    return new MessagesResponseHeader(error, null)
  }
  public static withPayload(
    md5: string,
    countTotal: Int32,
    countUnread: Int32,
    modified: boolean,
    batchCount: Int32,
  ): MessagesResponseHeader {
    return new MessagesResponseHeader(
      1,
      new MessageResponseHeaderPayload(md5, countTotal, countUnread, modified, batchCount),
    )
  }
}

export function messageResponseHeaderFromJSONItem(item: JSONItem): Nullable<MessagesResponseHeader> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const error = map.getInt32('error')!
  if (error !== 1) {
    return MessagesResponseHeader.withError(error)
  }
  const md5 = map.getString('md5')!
  const countTotal = map.getInt32('countTotal')!
  const countUnread = map.getInt32('countUnread')!
  const modified = map.getBoolean('modified')!
  const batchCount = map.getInt32('batchCount')!
  return MessagesResponseHeader.withPayload(md5, countTotal, countUnread, modified, batchCount)
}
