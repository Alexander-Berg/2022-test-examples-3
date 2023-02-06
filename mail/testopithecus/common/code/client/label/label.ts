import { Int32, int64, Int64, Nullable, stringToInt32 } from '../../../ys/ys'
import { ID, LabelID } from '../common/id'
import { JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'

export const enum LabelType {
  user = 1,
  system = 3,
  important = 6,
}
export function int32ToLabelType(value: Int32): LabelType {
  switch (value) {
    case 1: return LabelType.user
    case 3: return LabelType.system
    case 6: return LabelType.important
    default: return LabelType.system
  }
}
export function labelTypeToInt32(value: LabelType): Int32 {
  switch (value) {
    case LabelType.user: return 1
    case LabelType.system: return 3
    case LabelType.important: return 6
  }
}

export class Label {
  public constructor(
    public readonly lid: LabelID,
    public readonly type: LabelType,
    public readonly name: Nullable<string>,
    public readonly unreadCounter: Int32,
    public readonly totalCounter: Int32,
    public readonly color: Int32,
    public readonly symbol: Int64,
  ) { }
}

export function labelFromJSONItem(item: JSONItem): Nullable<Label> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const lid = map.getString('lid')!
  const name = map.getString('display_name')
  const unread = map.getInt32('count_unread')!
  const total = map.getInt32('count_all')!
  const type = int32ToLabelType(map.getInt32('type')!)
  const colorString = map.getString('color')!
  let color = stringToInt32(colorString, 16)
  if (color === null) {
    color = 0
  }
  return new Label(lid, type, name, unread, total, color!, int64(0))
}
