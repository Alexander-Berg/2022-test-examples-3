import { int64, Int64, Nullable } from '../../../ys/ys';

export function currentTimeMs(): Int64 {
  return int64(Date.now())
}

/**
 * Returns null if input value is an empty string
 */
export function nullIfEmptyString(value: Nullable<string>): Nullable<string> {
  return value === '' ? null : value
}

export function emptyStringIfNull(value: Nullable<string>): string {
  return value ===  null ? '' : value;
}

export function quote(value: string): string {
  return '"' + value + '"'
}
