// noinspection JSUnusedGlobalSymbols
export type Nullable<T> = T | null

export type Int32 = number
export type Int64 = bigint
export type Double = number

// tslint:disable-next-line:no-empty-interface
export interface Encodable { }
// tslint:disable-next-line:no-empty-interface
export interface Decodable { }

export interface Codable extends Encodable, Decodable { }

export function* range(from: Int32, to: Int32, step: Int32 = 1): Iterable<Int32> {
  if (step <= 0) {
    throw new Error(`Step argument must be greater than zero. Now it's ${step}.`)
  }
  for (let i = from; i < to; i += step) {
    yield i
  }
}

export function* decrementalRange(from: Int32, to: Int32, step: Int32 = 1): Iterable<Int32> {
  if (step <= 0) {
    throw new Error(`Step argument must be greater than zero. Now it's ${step}.`)
  }
  for (let i = from; i >= to; i -= step) {
    yield i
  }
}

// tslint:disable-next-line:ban-types
export function weakThis<F extends Function>(f: F): F {
  return f
}

export function weak(target: any, propertyKey: string) {
  // Marker attribute
}

/**
 * This function must be used to cast arrays as Swift YSArray<T> is invariant over T (Swift restrictions)
 * In order to emulate covariance, one should use the function in TS, and `cast` call will be generated in Swift.
 * Swift function must be implemented like this:
 * public func cast<T, U>(_ array: YSArray<T>) -> YSArray<U> {
 *   return YSArray<U>(array: array.map { (item) in item as! U })
 * }
 * @param array Array to cast
 */
// noinspection JSUnusedGlobalSymbols
export function cast<T, U>(array: readonly T[]): readonly U[] {
  return array.map((item) => item as any as U)
}

export function castToAny<T>(value: T): any {
  return value
}

/**
 * The function must have a counterpart in Native world. It converts numeric values to Int64.
 * @param value value to convert to Int64
 * @description The function will be obsolete as soon as Jest supports bigint literals out of the box (v25)
 */
export function int64(value: Int32 | Int64): Int64 {
  return typeof value === 'number' ? BigInt(value) : value
}

/**
 * The function must have a counterpart in Native world. It converts Int32 value to Int64
 * @param value value to convert to Int64
 */
export function int32ToInt64(value: Int32): Int64 {
  return BigInt(value)
}

/**
 * The function must have a counterpart in Native world. It converts Int64 value to Int32.
 * It should loose presision if overflown.
 * @param value value to convert to Int32
 */
export function int64ToInt32(value: Int64): Int32 {
  return Number(value)
}

/**
 * The function must have a counterpart in Native world. It converts Int64 value to Double.
 * @param value value to convert to Double
 */
export function int64ToDouble(value: Int64): Double {
  return Number(value)
}

/**
 * The Native function counterpart should basically be an identity function from nullable to nullable.
 * @param value value to convert to Int64
 */
export function undefinedToNull<T>(value: T | undefined | null): Nullable<T> {
  return value === null || value === undefined ? null : value
}

export function nullthrows<T>(value: T | undefined | null): T {
  if (value != null) {
    return value!
  }
  throw new Error('Got unexpected null or undefined')
}

/**
 * The function must have a counterpart in Native world. It converts string value to Int64.
 * @param value value to convert to Int64.
 * @description The function evaluates the argument. It analyzes the first two symbols of the string:
 * if it starts with '0x', it's treated as a hexadecimal number;
 * if it starts with '0b', it's treated as a binary number;
 * if it starts with neither of the above, it's treated as a decimal number.
 */
export function stringToInt64(value: string): Nullable<Int64> {
  if (value.length === 0) {
    return null
  }
  try {
    return BigInt(value)
  } catch {
    return null
  }
}

/**
 * The function must have a counterpart in Native world. It converts string value to Int32.
 * @param value value to convert to Int32
 */
export function stringToInt32(value: string, radix: Int32 = 10): Nullable<Int32> {
  const result = Number.parseInt(value, radix)
  return Number.isNaN(result) ? null : result
}

/**
 * The function must have a counterpart in Native world. It converts string value to Double.
 * @param value value to convert to Double
 */
export function stringToDouble(value: string): Nullable<Double> {
  const result = Number.parseFloat(value)
  return Number.isNaN(result) ? null : result
}

/**
 * The function must have a counterpart in Native world. It converts Int64 value to string.
 * @param value value to convert to string
 */
export function int64ToString(value: Int64): string {
  return value.toString()
}

/**
 * The function must have a counterpart in Native world. It converts Int32 value to string.
 * @param value value to convert to string
 */
export function int32ToString(value: Int32): string {
  return value.toString()
}

/**
 * The function must have a counterpart in Native world. It converts Double value to Int32, by truncating.
 * @param value value to convert to Int32
 */
export function doubleToInt32(value: Double): Int32 {
  return Math.trunc(value)
}

/**
 * The function must have a counterpart in Native world. It converts Double value to Int64, by truncating.
 * @param value value to convert to Int64
 * @description The function properly processes only Doubles with integer part in range -2^53..+2^53
 */
export function doubleToInt64(value: Double): Int64 {
  return BigInt(Math.trunc(value))
}

/**
 * The function must have a counterpart in Native world. It converts Double value to string.
 * @param value value to convert to string
 */
export function doubleToString(value: Double): string {
  return value.toString()
}

/**
 * The function must have a counterpart in Native world. It converts boolean value to Int32.
 * @param value value to convert to Int32
 */
export function booleanToInt32(value: boolean): Int32 {
  return value ? 1 : 0
}

/**
 * The function must have a counterpart in Native world. It converts Int32 value to boolean.
 * @param value value to convert to boolean
 */
export function int32ToBoolean(value: Int32): boolean {
  return value !== 0
}

/**
 * The function must have a counterpart in Native world. It converts Set value to Array.
 * @param value value to convert to Array
 */
export function setToArray<T>(value: ReadonlySet<T>): T[] {
  return Array.from(value)
}

/**
 * The function must have a counterpart in Native world. It converts Array value to Set.
 * @param value value to convert to Set
 */
export function arrayToSet<T>(value: readonly T[]): Set<T> {
  return new Set(value)
}

/**
 * The function must have a counterpart in Native world. It converts Iterable value to Array.
 * @param value value to convert to Array
 */
export function iterableToArray<T>(value: IterableIterator<T>): T[] {
  return Array.from(value)
}

/**
 * The function must have a counterpart in Native world. It converts Iterable value to Set.
 * @param value value to convert to Set
 */
export function iterableToSet<T>(value: IterableIterator<T>): Set<T> {
  return new Set(value)
}

/**
 * Collection of functions which allows to typecheck arbitrary objects
 */
export class TypeSupport {
  public static isString(value: any): boolean {
    return typeof value === 'string'
  }
  public static asString(value: any): Nullable<string> {
    return this.isString(value) ? value as string : null
  }
  public static isBoolean(value: any): boolean {
    return typeof value === 'boolean'
  }
  public static asBoolean(value: any): Nullable<boolean> {
    return this.isBoolean(value) ? value as boolean : null
  }
  public static isInt32(value: any): boolean {
    return Number.isInteger(value)
  }
  public static asInt32(value: any): Nullable<Int32> {
    return this.isInt32(value) ? value as Int32 : null
  }
  public static isInt64(value: any): boolean {
    return typeof value === 'bigint'
  }
  public static asInt64(value: any): Nullable<Int64> {
    return this.isInt64(value) ? value as Int64 : null
  }
  public static isDouble(value: any): boolean {
    return typeof value === 'number'
  }
  public static asDouble(value: any): Nullable<Double> {
    return this.isDouble(value) ? value as Double : null
  }
}
