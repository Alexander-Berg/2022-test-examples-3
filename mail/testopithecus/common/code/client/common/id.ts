import { Int64, int64ToString, Nullable, stringToInt64 } from '../../../ys/ys'

export type ID = Int64
export type LabelID = string

export function idFromString(value: Nullable<string>): Nullable<ID> {
  if (value === null) {
    return null
  }
  return stringToInt64(value!)
}
export function idToString(value: Nullable<ID>): Nullable<string> {
  if (value === null) {
    return null
  }
  return int64ToString(value!)
}
