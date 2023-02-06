import { Int32, Nullable } from '../../../ys/ys'
import { JSONItem, JSONItemKind, MapJSONItem } from '../../mail/logging/json-types'
import { StringBuilder } from '../string-builder'

export class Recipient {
  public constructor(
    public readonly email: string,
    public readonly name: Nullable<string>,
    public readonly type: RecipientType,
  ) { }

  public asString(): string {
    const result = new StringBuilder()
    const hasName = this.name !== null && this.name.length > 0
    if (hasName) {
      result.add(this.name!)
    }
    if (this.email.length > 0) {
      result.add(hasName ? ' ' : '').add('<').add(this.email).add('>')
    }
    return result.build()
  }
}

export const enum RecipientType {
  to = 1,
  from = 2,
  cc = 3,
  bcc = 4,
  replyTo = 5,
}

export function int32ToRecipientType(value: Int32): Nullable<RecipientType> {
  switch (value) {
    case 1: return RecipientType.to
    case 2: return RecipientType.from
    case 3: return RecipientType.cc
    case 4: return RecipientType.bcc
    case 5: return RecipientType.replyTo
    default: return null
  }
}

export function recipientTypeToInt32(value: RecipientType): Int32 {
  switch (value) {
    case RecipientType.to: return 1
    case RecipientType.from: return 2
    case RecipientType.cc: return 3
    case RecipientType.bcc: return 4
    case RecipientType.replyTo: return 5
  }
}

export function recipientFromJSONItem(item: JSONItem): Nullable<Recipient> {
  if (item.kind !== JSONItemKind.map) {
    return null
  }
  const map = item as MapJSONItem
  const email = map.getString('email')!
  const name = map.getString('name')!
  const type = int32ToRecipientType(map.getInt32('type')!)
  if (type === null) {
    return null
  }
  return new Recipient(email, name, type)
}

export function recipientToJSONItem(recipient: Recipient): JSONItem {
  const item = new MapJSONItem()
  item.putString('email', recipient.email)
  if (recipient.name !== null) {
    item.putString('name', recipient.name)
  }
  item.putInt32('type', recipientTypeToInt32(recipient.type))
  return item
}
