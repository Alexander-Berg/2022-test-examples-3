import { Int32, int64, Int64, Nullable, stringToInt32, stringToInt64 } from '../../../ys/ys'
import { ID, idFromString, LabelID } from '../common/id'
import {
  JSONItem,
  JSONItemKind,
  JSONItemToInt32,
  MapJSONItem,
  StringJSONItem,
} from '../../mail/logging/json-types'
import { recipientFromJSONItem } from '../recipient/recipient'
import { messageTypeMaskFromServerMessageTypes } from './message-type'

export class MessageMeta {
  public constructor(
    public readonly mid: ID,
    public readonly fid: ID,
    public readonly tid: Nullable<ID>,
    public readonly lid: readonly LabelID[],
    public readonly subjectEmpty: boolean,
    public readonly subjectPrefix: Nullable<string>,
    public readonly subjectText: string,
    public readonly firstLine: string,
    public readonly sender: string,
    public readonly unread: boolean,
    public readonly searchOnly: boolean,
    public readonly showFor: Nullable<string>,
    public readonly timestamp: Int64,
    public readonly hasAttach: boolean,
    public readonly attachments: Nullable<Attachments>,
    public readonly typeMask: Int32,
    public readonly threadCount: Nullable<string>,
  ) { }
}

export class Attachments {
  public constructor(
    public readonly attachments: readonly Attachment[],
  ) { }
}

export class Attachment {
  public constructor(
    public readonly hid: string,
    public readonly displayName: string,
    public readonly fileClass: string,
    public readonly isDisk: boolean,
    public readonly size: Int64,
    public readonly mimeType: string,
    public readonly previewSupported: boolean,
    public readonly previewUrl: Nullable<string>,
    public readonly downloadUrl: string,
    public readonly isInline: boolean,
    public readonly contentID: Nullable<string>,
  ) { }
}

export function attachmentsFromJSONItem(json: JSONItem): Nullable<Attachments> {
  if (json.kind !== JSONItemKind.map) {
    return null
  }
  const attachments: Attachment[] = []
  for (const item of (json as MapJSONItem).getArrayOrDefault('attachments', [])) {
    if (item.kind === JSONItemKind.map) {
      const map = item as MapJSONItem
      attachments.push(new Attachment(
        map.getString('hid')!,
        map.getString('display_name')!,
        map.getString('class')!,
        map.getBooleanOrDefault('narod', false),
        map.getInt64('size')!,
        map.getString('mime_type')!,
        map.getBooleanOrDefault('preview_supported', false),
        map.getString('preview_url'),
        map.getString('download_url')!,
        map.getBooleanOrDefault('is_inline', false),
        map.getString('content_id'),
      ))
    }
  }
  return new Attachments(attachments)
}

export function messageMetaFromJSONItem(json: JSONItem): Nullable<MessageMeta> {
  if (json.kind !== JSONItemKind.map) {
    return null
  }
  const map = json as MapJSONItem
  const mid = idFromString(map.getString('mid'))!
  const fid = idFromString(map.getString('fid'))!
  const tid = idFromString(map.getString('tid'))
  const lids = map.getArrayOrDefault('lid', []).map((item) => (item as StringJSONItem).value)
  const subjectEmpty = map.getBoolean('subjEmpty')!
  const subjectPrefix = map.getString('subjPrefix')!
  const subjectText = map.getString('subjText')!
  const firstLine = map.getStringOrDefault('firstLine', '')
  const sender = recipientFromJSONItem(map.get('from')!)!.asString()
  const isUnread = map.getArray('status')!.map((statusItem): Int32 => {
    const value: Nullable<Int32> = JSONItemToInt32(statusItem)
    return (value !== null) ? value : 0
  }).includes(1)
  const timestamp = stringToInt64(map.getString('utc_timestamp')!)! * int64(1000)
  const hasAttach = map.getBoolean('hasAttach')!
  const types = messageTypeMaskFromServerMessageTypes(map
    .getArray('types')!
    .map((item) => stringToInt32((item as StringJSONItem).value)!))
  let attachments: Nullable<Attachments> = null
  if (map.hasKey('attachments')) {
    attachments = attachmentsFromJSONItem(map.get('attachments')!)
  }
  const threadCount = map.getString('threadCount')
  return new MessageMeta(
    mid,
    fid,
    tid,
    lids,
    subjectEmpty,
    subjectPrefix,
    subjectText,
    firstLine,
    sender,
    isUnread,
    false,
    null,
    timestamp,
    hasAttach,
    attachments,
    types,
    threadCount,
  )
}

export function getMidToTimestampMap(metas: readonly MessageMeta[]): ReadonlyMap<ID, Int64> {
  const midToTimestamp = new Map<ID, Int64>()
  for (const meta of metas) {
    midToTimestamp.set(meta.mid, meta.timestamp)
  }
  return midToTimestamp
}
