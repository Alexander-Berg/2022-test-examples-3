import {renderHook} from '@testing-library/react-hooks'
import useInstance from './useInstance'

describe('useInstance', () => {
  test('всегда возвращает ссылку на один и тот-же instance', () => {
    class Test {
      dispose() {}
    }
    const factory = jest.fn(() => new Test())
    const {result, rerender} = renderHook(() => useInstance(factory))

    const instance = result.current

    expect(factory).toHaveBeenCalledTimes(1)
    expect(instance).toBeInstanceOf(Test)

    rerender()

    expect(factory).toHaveBeenCalledTimes(1)
    expect(result.current).toBe(instance)
  })

  test('вызывает dispose при unmount', () => {
    const dispose = jest.fn()
    const {unmount} = renderHook(() => useInstance(() => ({dispose})))

    unmount()

    expect(dispose).toHaveBeenCalledTimes(1)
  })
})
