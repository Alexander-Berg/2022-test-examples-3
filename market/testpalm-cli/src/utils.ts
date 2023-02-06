import { curry, keys, reduce } from 'ramda'

export const isIterable = (obj: any) =>
  obj != null &&
  !Object.isSealed(obj) &&
  typeof obj[Symbol.iterator] === 'function'

export const isPlainObject = (obj: any) =>
  typeof obj === 'object' && !isIterable(obj)

export const isString = (str: any) => typeof str === 'string'

export const mapKeys = curry(function mapKeys(
  fn: (key: string) => string,
  obj,
) {
  return reduce(
    function(acc, key) {
      // @ts-ignore
      acc[fn(key)] = obj[key]
      return acc
    },
    {},
    keys(obj),
  )
})
