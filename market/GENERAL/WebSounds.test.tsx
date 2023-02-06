import React from 'react'
import {render, screen, act} from '@testing-library/react'
import AlarmCancels from './AlarmCancels'
import {IEnv} from 'core-legacy/types'
import {Provider} from 'core-di'
import {AppStateType} from 'core-legacy/AppState'
import {LoggerStub, SocketEventsMock} from 'shared/test-utils'
import AlarmChanges from './AlarmChanges'
import AlarmIncomes from './AlarmIncomes'

const socketEvent$ = new SocketEventsMock()
const logger = new LoggerStub()
let audio: HTMLAudioElement

describe('AlarmIncomes', () => {
  beforeEach(() => {
    spyOn(HTMLAudioElement.prototype, 'play')
  })

  test('вставляет audio тэг на паузе', async () => {
    renderWithServices(<AlarmIncomes mode='infinite' unacceptedOrdersCount={0} finiteDurationMs={1000} />)
    await initAudio()
    expect(audio.loop).toBe(false)
    expect(typeof audio.src).toBe('string')
    expect(audio.paused).toBe(true)
    expect(audio.title).toBe('Новый заказ')
  })

  describe('в режиме "infinite"', () => {
    test('воспроизводит звук если есть непринятые заказы', async () => {
      renderWithServices(<AlarmIncomes mode='infinite' unacceptedOrdersCount={1} finiteDurationMs={1000} />)
      await initAudio()

      expect(audio.play).toHaveBeenCalled()
      expect(audio.loop).toBe(true)
    })

    test('начинает воспроизводить звук при поступлении нового заказа', async () => {
      const {rerender} = renderWithServices(
        <AlarmIncomes mode='infinite' unacceptedOrdersCount={0} finiteDurationMs={1000} />
      )
      await initAudio()

      expect(audio.play).not.toHaveBeenCalled()
      expect(audio.loop).toBe(false)

      rerender(<AlarmIncomes mode='infinite' unacceptedOrdersCount={1} finiteDurationMs={1000} />)

      expect(audio.play).toHaveBeenCalled()
      expect(audio.loop).toBe(true)
    })

    test('прекращает воспроизводить звук при принятии заказа', async () => {
      const {rerender} = renderWithServices(
        <AlarmIncomes mode='infinite' unacceptedOrdersCount={1} finiteDurationMs={1000} />
      )
      await initAudio()

      rerender(<AlarmIncomes mode='infinite' unacceptedOrdersCount={0} finiteDurationMs={1000} />)

      expect(audio.loop).toBe(false)
    })

    test('не воспроизводит звук на событие нового заказа', async () => {
      renderWithServices(<AlarmIncomes mode='infinite' unacceptedOrdersCount={0} finiteDurationMs={1000} />)
      await initAudio()

      act(() => {
        socketEvent$.sendOrderNew()
      })

      expect(audio.play).not.toHaveBeenCalled()
      expect(audio.loop).toBe(false)
    })
  })

  describe('в режиме "finite"', () => {
    beforeEach(() => {
      jest.useFakeTimers()
    })

    test('воспроизводит звук при поступлении нового заказа в течении 20 секунд', async () => {
      const duration = 1000
      renderWithServices(<AlarmIncomes mode='finite' unacceptedOrdersCount={0} finiteDurationMs={duration} />)
      await initAudio()

      act(() => {
        socketEvent$.sendOrderNew()
      })

      expect(audio.play).toHaveBeenCalled()
      expect(audio.loop).toBe(true)

      act(() => {
        jest.runOnlyPendingTimers()
      })

      expect(audio.loop).toBe(false)
    })

    test('не воспроизводит звук даже если есть непринятые заказы', async () => {
      renderWithServices(<AlarmIncomes mode='finite' unacceptedOrdersCount={1} finiteDurationMs={1000} />)
      await initAudio()

      expect(audio.play).not.toHaveBeenCalled()
      expect(audio.loop).toBe(false)
    })
  })

  async function initAudio() {
    audio = (await screen.findByTestId('orders__new-order-alarm')) as HTMLAudioElement
  }
})

describe('AlarmChanges', () => {
  beforeEach(async () => {
    renderWithServices(<AlarmChanges />)
    audio = (await screen.findByTestId('orders__change-order-alarm')) as HTMLAudioElement
    spyOn(audio, 'play')
  })

  test('вставляет audio тэг на паузе', () => {
    expect(audio.loop).toBe(false)
    expect(typeof audio.src).toBe('string')
    expect(audio.paused).toBe(true)
    expect(audio.title).toBe('Изменение заказа')
  })

  test('воспроизводит звук при изменении заказа', () => {
    socketEvent$.sendOrderChangedItems()
    expect(audio.play).toHaveBeenCalled()
  })
})

describe('AlarmCancels', () => {
  let audio: HTMLAudioElement

  beforeEach(async () => {
    renderWithServices(<AlarmCancels />)
    audio = (await screen.findByTestId('orders__cancel-order-alarm')) as HTMLAudioElement
    spyOn(audio, 'play')
  })

  test('вставляет audio тэг на паузе', () => {
    expect(audio.loop).toBe(false)
    expect(typeof audio.src).toBe('string')
    expect(audio.paused).toBe(true)
    expect(audio.title).toBe('Заказ отменен')
  })

  test('воспроизводит звук при переводе заказа в статус "refused"', () => {
    socketEvent$.sendOrderChangedStatus('refused')
    expect(audio.play).toHaveBeenCalled()
  })

  test('воспроизводит звук при переводе заказа в статус "no_show"', () => {
    socketEvent$.sendOrderChangedStatus('no_show')
    expect(audio.play).toHaveBeenCalled()
  })

  test('молчит при изменении заказа на другой статус', () => {
    socketEvent$.sendOrderChangedStatus('accepted')
    expect(audio.play).not.toHaveBeenCalled()
  })
})

function renderWithServices(jsx: JSX.Element) {
  const env = ({
    logger,
    socketEvent$
  } as unknown) as IEnv
  const wrapJsx = (jsx: JSX.Element) => (
    <Provider appState={(null as unknown) as AppStateType} env={env}>
      {jsx}
    </Provider>
  )
  const {rerender, ...rest} = render(wrapJsx(jsx))

  return {
    ...rest,
    rerender: (jsx: JSX.Element) => rerender(wrapJsx(jsx))
  }
}
