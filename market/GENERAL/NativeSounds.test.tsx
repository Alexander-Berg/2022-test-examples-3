import React, {ComponentProps} from 'react'
import {render, act} from '@testing-library/react'
import {LoggerStub} from 'shared/test-utils'
import NativeSounds from './NativeSounds'

jest.mock('core-di', () => {
  return {
    useEnv: () => ({
      logger: new LoggerStub()
    })
  }
})

describe('NativeSounds', () => {
  const updateState = jest.fn(() => Promise.resolve())

  const commonProps: ComponentProps<typeof NativeSounds> = {
    updateState,
    unacceptedOrdersIds: [],
    ringOnNewOrders: false,
    syncStateInterval: 1000,
    finiteDurationMs: 1000,
    soundMode: 'infinite'
  }

  beforeEach(() => {
    updateState.mockClear()
  })

  test('вызывает updateState на старте с переданными атрибутами', () => {
    render(
      <NativeSounds
        {...commonProps}
        unacceptedOrdersIds={['test1', 'test2']}
        ringOnNewOrders
        soundMode='finite'
        finiteDurationMs={10000}
      />
    )

    expect(updateState).toHaveBeenCalledWith({
      ringOnNewOrders: true,
      unacceptedOrdersIds: ['test1', 'test2'],
      soundMode: 'finite',
      finiteDurationMs: 10000
    })
  })

  test('повторяет вызовы updateState через syncStateInterval', () => {
    jest.useFakeTimers()

    render(<NativeSounds {...commonProps} syncStateInterval={1000} />)

    expect(updateState).toHaveBeenCalledTimes(1)

    act(() => {
      jest.runTimersToTime(1000)
    })

    expect(updateState).toHaveBeenCalledTimes(2)

    act(() => {
      jest.runTimersToTime(1000)
    })

    expect(updateState).toHaveBeenCalledTimes(3)
  })

  test('вызывает updateState при изменении входных данных', () => {
    const {rerender} = render(
      <NativeSounds
        {...commonProps}
        unacceptedOrdersIds={['1']}
        soundMode='infinite'
        finiteDurationMs={15000}
        ringOnNewOrders
      />
    )

    expect(updateState).toHaveBeenCalledTimes(1)
    updateState.mockClear()

    rerender(
      <NativeSounds
        {...commonProps}
        unacceptedOrdersIds={[]}
        soundMode='finite'
        finiteDurationMs={10000}
        ringOnNewOrders={false}
      />
    )

    expect(updateState).toHaveBeenCalledWith({
      soundMode: 'finite',
      unacceptedOrdersIds: [],
      finiteDurationMs: 10000,
      ringOnNewOrders: false
    })
  })
})
