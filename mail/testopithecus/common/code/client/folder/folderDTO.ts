import { Int32, Nullable, stringToInt32 } from '../../../ys/ys'
import { JSONItem, JSONItemKind, MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { nullIfEmptyString } from '../../mail/logging/logging-utils';
import { ID, idFromString } from '../common/id'

export const enum FolderType {
  inbox = 1,
  user = 2,
  outgoing = 3,
  sent = 4,
  draft = 5,
  spam = 6,
  trash = 7,
  archive = 8,
  templates = 9,
  discount = 10,
  other = 11,
  unsubscribe = 12,
  tab_relevant = 100,
  tab_news = 101,
  tab_social = 102,
}
export function int32ToFolderType(value: Int32): FolderType {
  switch (value) {
    case 1: return FolderType.inbox
    case 2: return FolderType.user
    case 3: return FolderType.outgoing
    case 4: return FolderType.sent
    case 5: return FolderType.draft
    case 6: return FolderType.spam
    case 7: return FolderType.trash
    case 8: return FolderType.archive
    case 9: return FolderType.templates
    case 10: return FolderType.discount
    case 11: return FolderType.other
    case 12: return FolderType.unsubscribe
    case 100: return FolderType.tab_relevant
    case 101: return FolderType.tab_news
    case 102: return FolderType.tab_social
    default: return FolderType.other
  }
}
export function folderTypeToInt32(value: FolderType): Int32 {
  switch (value) {
    case FolderType.inbox: return 1
    case FolderType.user: return 2
    case FolderType.outgoing: return 3
    case FolderType.sent: return 4
    case FolderType.draft: return 5
    case FolderType.spam: return 6
    case FolderType.trash: return 7
    case FolderType.archive: return 8
    case FolderType.templates: return 9
    case FolderType.discount: return 10
    case FolderType.other: return 11
    case FolderType.unsubscribe: return 12
    case FolderType.tab_relevant: return 100
    case FolderType.tab_news: return 101
    case FolderType.tab_social: return 102
  }
}
export function isFolderOfThreadedType(type: FolderType): boolean {
  switch (type) {
    case FolderType.trash: return false
    case FolderType.outgoing: return false
    case FolderType.templates: return false
    case FolderType.draft: return false
    case FolderType.spam: return false
    default: return true
  }
}

export function isFolderOfTabType(type: FolderType): boolean {
  switch (type) {
    case FolderType.tab_relevant: return true
    case FolderType.tab_news: return true
    case FolderType.tab_social: return true
    default: return false
  }
}

export const enum FolderSyncType {
  doNotSync = 0,
  silentSync = 1,
  pushSync = 2,
}
export function int32ToFolderSyncType(value: Int32): FolderSyncType {
  switch (value) {
    case 0: return FolderSyncType.doNotSync
    case 1: return FolderSyncType.silentSync
    case 2: return FolderSyncType.pushSync
    default: throw new Error(`Unknown FolderSyncType for ${value}`)
  }
}
export function folderSyncTypeToInt32(value: FolderSyncType): Int32 {
  switch (value) {
    case FolderSyncType.doNotSync: return 0
    case FolderSyncType.silentSync: return 1
    case FolderSyncType.pushSync: return 2
  }
}

export class FolderDTO {
  public constructor(
    public readonly fid: ID,
    public readonly type: FolderType,
    public readonly name: Nullable<string>,
    public readonly position: Int32,
    public readonly parent: Nullable<ID>,
    public readonly unreadCounter: Int32,
    public readonly totalCounter: Int32,
  ) { }
}
export function folderFromJSONItem(item: JSONItem): Nullable<FolderDTO> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const fid = idFromString(map.getString('fid')!)!
  const parent = idFromString(nullIfEmptyString(map.getString('parent')!))
  const name = map.getString('display_name')
  const unread = map.getInt32('count_unread')!
  const total = map.getInt32('count_all')!
  const type = int32ToFolderType(map.getInt32('type')!)
  const position = stringToInt32((map.getMap('options')!.get('position')! as StringJSONItem).value)!
  return new FolderDTO(fid, type, name, position, parent, unread, total)
}
