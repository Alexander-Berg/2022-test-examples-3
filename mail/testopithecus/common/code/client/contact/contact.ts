import { int64ToString, Nullable } from '../../../ys/ys';
import { ArrayJSONItem, JSONItem, JSONItemKind, MapJSONItem, StringJSONItem } from '../../mail/logging/json-types'
import { ID, idFromString } from '../common/id';

export class Contact {
  public constructor(
    public readonly cid: ID,
    public readonly email: string,
  ) { }

  public copy(): Contact {
    return new Contact(this.cid, this.email);
  }

  public tostring(): string {
    return `Contact(${this.cid} ${this.email})`
  }
}

export function contactFromJSONItem(item: JSONItem): Nullable<Contact> {
  if (item.kind !== JSONItemKind.map) {
    return null;
  }
  const map = item as MapJSONItem
  const cid = idFromString(int64ToString(map.getInt64('cid')!))!
  const email = emailStringFromJSONItem(map.get('email')!)!;
  return new Contact(cid, email)

}

export function emailStringFromJSONItem(item: JSONItem): Nullable<string> {
  if (item.kind === JSONItemKind.array) {
    const array = item as ArrayJSONItem
    return emailStringFromJSONItem(array.get(0));
  }

  if (item.kind === JSONItemKind.string) {
    const str = item as StringJSONItem
    return str.value;
  }

  if (item.kind !== JSONItemKind.map) {
    return null;
  }
  const map = item as MapJSONItem
  return map.getString('value')!
}

export function emailStringFromContact(contact: Contact): string {
  return contact.email;
}
