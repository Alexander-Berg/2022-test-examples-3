import {
  Double,
  doubleToInt32,
  doubleToInt64,
  doubleToString,
  Int32,
  int32ToInt64,
  Int64,
  int64ToDouble,
  int64ToInt32,
  int64ToString,
  Nullable,
  stringToDouble,
  stringToInt32,
  stringToInt64,
  undefinedToNull,
} from '../../../ys/ys'
import { quote } from './logging-utils';

export enum JSONItemKind {
  integer,
  double,
  string,
  boolean,
  nullItem,
  map,
  array,
}

export function JSONItemKindToString(kind: JSONItemKind): string {
  switch (kind) {
    case JSONItemKind.integer:
      return 'integer'
    case JSONItemKind.double:
      return 'double'
    case JSONItemKind.string:
      return 'string'
    case JSONItemKind.boolean:
      return 'boolean'
    case JSONItemKind.nullItem:
      return 'nullItem'
    case JSONItemKind.map:
      return 'map'
    case JSONItemKind.array:
      return 'array'
  }
}

export function JSONItemGetValueDebugDescription(item: JSONItem): string {
  switch (item.kind) {
    case JSONItemKind.integer:
      return int64ToString((item as IntegerJSONItem).asInt64())
    case JSONItemKind.double:
      return doubleToString((item as DoubleJSONItem).value)
    case JSONItemKind.string:
      return quote((item as StringJSONItem).value)
    case JSONItemKind.boolean:
      return (item as BooleanJSONItem).value ? 'true' : 'false'
    case JSONItemKind.nullItem:
      return 'null'
    case JSONItemKind.map:
      const map = item as MapJSONItem
      const mapValues: string[] = []
      map.asMap().forEach((value: JSONItem, key: string) => {
        mapValues.push(`"${key}": ${JSONItemGetDebugDescription(value)}`)
      })
      return `{${mapValues.join(', ')}}`
    case JSONItemKind.array:
      const array = item as ArrayJSONItem
      const arrayValues: string[] = array.asArray().map((value) => JSONItemGetDebugDescription(value))
      return `[${arrayValues.join(', ')}]`
  }
}

export function JSONItemGetDebugDescription(item: JSONItem): string {
  const valueDescription = JSONItemGetValueDebugDescription(item)
  return `<JSONItem kind: ${JSONItemKindToString(item.kind)}, value: ${valueDescription}>`
}

// tslint:disable:max-classes-per-file

export interface JSONItem {
  readonly kind: JSONItemKind
}

export class IntegerJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.integer

  private constructor(private readonly value: Int64, public readonly isInt64: boolean) { }

  public static fromInt32(value: Int32): IntegerJSONItem {
    return new IntegerJSONItem(int32ToInt64(value), false)
  }
  public static fromInt64(value: Int64): IntegerJSONItem {
    return new IntegerJSONItem(value, true)
  }

  public asInt32(): Int32 {
    return int64ToInt32(this.value)
  }
  public asInt64(): Int64 {
    return this.value
  }
}

export class DoubleJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.double

  public constructor(public readonly value: Double) { }
}

export class StringJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.string

  public constructor(public readonly value: string) { }
}

export class BooleanJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.boolean

  public constructor(public readonly value: boolean) { }
}

// tslint:disable-next-line:max-classes-per-file
export class NullJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.nullItem
}

// tslint:disable-next-line:max-classes-per-file
export class MapJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.map

  public constructor(private readonly value: Map<string, JSONItem> = new Map<string, JSONItem>()) { }

  public asMap(): Map<string, JSONItem> {
    return this.value
  }

  public put(key: string, value: JSONItem): MapJSONItem {
    this.value.set(key, value)
    return this
  }
  public putInt32(key: string, value: Int32): MapJSONItem {
    this.value.set(key, IntegerJSONItem.fromInt32(value))
    return this
  }
  public putInt64(key: string, value: Int64): MapJSONItem {
    this.value.set(key, IntegerJSONItem.fromInt64(value))
    return this
  }
  public putDouble(key: string, value: Double): MapJSONItem {
    this.value.set(key, new DoubleJSONItem(value))
    return this
  }
  public putBoolean(key: string, value: boolean): MapJSONItem {
    this.value.set(key, new BooleanJSONItem(value))
    return this
  }
  public putString(key: string, value: string): MapJSONItem {
    this.value.set(key, new StringJSONItem(value))
    return this
  }
  public putStringIfPresent(key: string, value: Nullable<string>): MapJSONItem {
    if (value !== null) {
      this.putString(key, value!)
    }
    return this
  }
  public putNull(key: string): MapJSONItem {
    this.value.set(key, new NullJSONItem())
    return this
  }

  public get(key: string): Nullable<JSONItem> {
    return undefinedToNull(this.value.get(key))
  }
  public getArray(key: string): Nullable<readonly JSONItem[]> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null || result.kind !== JSONItemKind.array) {
      return null
    }
    return (result as ArrayJSONItem).asArray()
  }
  public getArrayOrDefault(key: string, value: readonly JSONItem[]): readonly JSONItem[] {
    return this.getArray(key) ?? value
  }
  public getMap(key: string): Nullable<ReadonlyMap<string, JSONItem>> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null || result.kind !== JSONItemKind.map) {
      return null
    }
    return (result as MapJSONItem).asMap()
  }
  public getMapOrDefault(key: string, value: ReadonlyMap<string, JSONItem>): ReadonlyMap<string, JSONItem> {
    return this.getMap(key) ?? value
  }
  public getInt32(key: string): Nullable<Int32> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null) {
      return null
    }
    return JSONItemToInt32(result)
  }
  public getInt32OrDefault(key: string, value: Int32): Int32 {
    return this.getInt32(key) ?? value
  }
  public getInt64(key: string): Nullable<Int64> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null) {
      return null
    }
    return JSONItemToInt64(result)
  }
  public getInt64OrDefault(key: string, value: Int64): Int64 {
    return this.getInt64(key) ?? value
  }
  public getDouble(key: string): Nullable<Double> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null) {
      return null
    }
    return JSONItemToDouble(result)
  }
  public getDoubleOrDefault(key: string, value: Double): Double {
    return this.getDouble(key) ?? value
  }
  public getBoolean(key: string): Nullable<boolean> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null || result.kind !== JSONItemKind.boolean) {
      return null
    }
    return (result as BooleanJSONItem).value
  }
  public getBooleanOrDefault(key: string, value: boolean): boolean {
    return this.getBoolean(key) ?? value
  }
  public getString(key: string): Nullable<string> {
    const result = undefinedToNull(this.value.get(key))
    if (result === null || result.kind !== JSONItemKind.string) {
      return null
    }
    return (result as StringJSONItem).value
  }
  public getStringOrDefault(key: string, value: string): string {
    return this.getString(key) ?? value
  }
  public isNull(key: string): boolean {
    const result = undefinedToNull(this.value.get(key))
    if (result === null) {
      return false
    }
    return result.kind === JSONItemKind.nullItem
  }
  public hasKey(key: string): boolean {
    return undefinedToNull(this.value.get(key)) !== null
  }
}

// tslint:disable-next-line:max-classes-per-file
export class ArrayJSONItem implements JSONItem {
  public readonly kind: JSONItemKind = JSONItemKind.array

  public constructor(private readonly value: JSONItem[] = []) { }

  public asArray(): readonly JSONItem[] {
    return this.value
  }

  public getCount(): Int32 {
    return this.value.length
  }

  public add(value: JSONItem): ArrayJSONItem {
    this.value.push(value)
    return this
  }
  public addInt32(value: Int32): ArrayJSONItem {
    this.value.push(IntegerJSONItem.fromInt32(value))
    return this
  }
  public addInt64(value: Int64): ArrayJSONItem {
    this.value.push(IntegerJSONItem.fromInt64(value))
    return this
  }
  public addDouble(value: Double): ArrayJSONItem {
    this.value.push(new DoubleJSONItem(value))
    return this
  }
  public addBoolean(value: boolean): ArrayJSONItem {
    this.value.push(new BooleanJSONItem(value))
    return this
  }
  public addString(value: string): ArrayJSONItem {
    this.value.push(new StringJSONItem(value))
    return this
  }
  public addNull(): ArrayJSONItem {
    this.value.push(new NullJSONItem())
    return this
  }

  public get(index: Int32): JSONItem {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    return this.value[index]
  }
  public getMap(index: Int32): ReadonlyMap<string, JSONItem> {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const result = this.value[index]
    if (result.kind !== JSONItemKind.map) {
      throw new Error(`Type is not ${JSONItemKind.map} at index ${index}. It's ${result.kind}`)
    }
    return (result as MapJSONItem).asMap()
  }
  public getArray(index: Int32): readonly JSONItem[] {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const result = this.value[index]
    if (result.kind !== JSONItemKind.array) {
      throw new Error(`Type is not ${JSONItemKind.array} at index ${index}. It's ${result.kind}`)
    }
    return (result as ArrayJSONItem).asArray()
  }
  public getInt32(index: Int32): Int32 {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const value = this.value[index]
    const result: Nullable<Int32> = JSONItemToInt32(value)
    if (result !== null) {
      return result
    }
    throw new Error(`Type is not Int32 at index ${index}. It's ${value.kind}`)
  }
  public getInt64(index: Int32): Int64 {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const value = this.value[index]
    const result: Nullable<Int64> = JSONItemToInt64(value)
    if (result !== null) {
      return result
    }
    throw new Error(`Type is not Int64 at index ${index}. It's ${value.kind}`)
  }
  public getDouble(index: Int32): Double {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const value = this.value[index]
    const result: Nullable<Double> = JSONItemToDouble(value)
    if (result !== null) {
      return result
    }
    throw new Error(`Type is not Double at index ${index}. It's ${value.kind}`)
  }
  public getBoolean(index: Int32): boolean {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const result = this.value[index]
    if (result.kind !== JSONItemKind.boolean) {
      throw new Error(`Type is not ${JSONItemKind.boolean} at index ${index}. It's ${result.kind}`)
    }
    return (result as BooleanJSONItem).value
  }
  public getString(index: Int32): string {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    const result = this.value[index]
    if (result.kind !== JSONItemKind.string) {
      throw new Error(`Type is not ${JSONItemKind.string} at index ${index}. It's ${result.kind}`)
    }
    return (result as StringJSONItem).value
  }
  public isNull(index: Int32): boolean {
    if (index < 0 || index >= this.value.length) {
      throw new Error('Index is out of bounds')
    }
    return this.value[index].kind === JSONItemKind.nullItem
  }
}

export function JSONItemToInt32(item: JSONItem): Nullable<Int32> {
  switch (item.kind) {
    case JSONItemKind.double:
      return doubleToInt32((item as DoubleJSONItem).value)
    case JSONItemKind.integer:
      return (item as IntegerJSONItem).asInt32()
    case JSONItemKind.string:
      return stringToInt32((item as StringJSONItem).value)
    default:
      return null
  }
}

export function JSONItemToInt64(item: JSONItem): Nullable<Int64> {
  switch (item.kind) {
    case JSONItemKind.double:
      return doubleToInt64((item as DoubleJSONItem).value)
    case JSONItemKind.integer:
      return (item as IntegerJSONItem).asInt64()
    case JSONItemKind.string:
      return stringToInt64((item as StringJSONItem).value)
    default:
      return null
  }
}

export function JSONItemToDouble(item: JSONItem): Nullable<Double> {
  switch (item.kind) {
    case JSONItemKind.double:
      return (item as DoubleJSONItem).value
    case JSONItemKind.integer:
      return int64ToDouble((item as IntegerJSONItem).asInt64())
    case JSONItemKind.string:
      return stringToDouble((item as StringJSONItem).value)
    default:
      return null
  }
}

export function JSONItemGetValue(item: JSONItem): Nullable<any> {
  switch (item.kind) {
    case JSONItemKind.string:
      return (item as StringJSONItem).value
    case JSONItemKind.boolean:
      return (item as BooleanJSONItem).value
    case JSONItemKind.integer:
      const i = item as IntegerJSONItem
      return i.isInt64 ? i.asInt64() : i.asInt32()
    case JSONItemKind.double:
      return (item as DoubleJSONItem).value
    case JSONItemKind.array:
      return (item as ArrayJSONItem).asArray().map((it) => JSONItemGetValue(it))
    case JSONItemKind.map:
      const res = new Map<string, any>();
      (item as MapJSONItem).asMap().forEach((v, k) => {
        const val = JSONItemGetValue(v)
        if (val !== null) {
          res.set(k, val)
        }
      })
      return res
    default:
      return null
  }
}
