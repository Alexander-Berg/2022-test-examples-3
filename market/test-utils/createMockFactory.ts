import {mergeWith, cloneDeep} from 'lodash-es'
import {DeepPartial} from 'core-legacy/types/util'

export function mergeMocks<TObject, TSource>(object: TObject, source: TSource): TObject & TSource {
  function customizer(objValue: unknown, srcValue: unknown) {
    if (Array.isArray(objValue)) {
      return srcValue
    }
  }

  return mergeWith(object, source, customizer)
}

export type MockFactory<T> = (overrides?: DeepPartial<T>) => T

export function createMockFactory<T>(defaultMock: T): MockFactory<T> {
  return (overrides) => mergeMocks(cloneDeep(defaultMock), overrides)
}
