import { Int32, Nullable } from '../../ys/ys'
import { ID } from '../client/common/id'
import { Email } from '../client/settings/settings-entities'

export function filterByOrders<T>(array: T[], byOrders: Set<Int32>): T[] {
  const result: T[] = []
  for (const i of byOrders.values()) {
    result.push(array[i])
  }
  return result
}

export function min(a: Int32, b: Int32): Int32 {
  return a < b ? a : b
}

export function max(a: Int32, b: Int32): Int32 {
  return a > b ? a : b
}

export function valuesArray<K, V>(iterable: Map<K, V>): V[] {
  const result: V[] = []
  for (const element of iterable.values()) {
    result.push(element)
  }
  return result
}

export function reduced(id: ID): string {
  const s = id.toString()
  return s.slice(s.length - 3, s.length)
}

export function display(email: Email): string { // TODO toString function in Email
  return `${email.login}@${email.domain}`
}

export function copyArray<T>(array: T[]): T[] {
  const result: T[] = []
  for (const element of array) {
    result.push(element)
  }
  return result
}

export function copySet<T>(set: Set<T>): Set<T> {
  const result: Set<T> = new Set<T>()
  for (const element of set.values()) {
    result.add(element)
  }
  return result

}

export function copyMap<T, S>(map: Map<T, S>): Map<T, S> {
  const result: Map<T, S> = new Map<T, S>()
  map.forEach(((value, key) => {
    result.set(key, value)
  }))
  return result
}

export function requireNonNull<T>(obj: Nullable<T>, message: string): T {
  if (obj === null) {
    throw new Error(message)
  }
  return obj!
}
