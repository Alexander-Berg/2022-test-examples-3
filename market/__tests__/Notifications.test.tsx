import React from 'react'
import {fireEvent, renderWithProviders, setupIntersectionObserverMock} from 'core-legacy/test-utils'
import Notifications from 'main-communications/components/Notifications/Notifications'
import Notification from 'main-communications/models/Notification'
import {MetrikaStub} from 'shared/test-utils'

jest.mock('react-intersection-observer', () => ({
  useInView: () => ({ref: React.createRef(), inView: true})
}))

function generateNotifications(count: number) {
  return new Array(count).fill(0).map((_, i) =>
    Notification.create({
      id: `notification${i}`,
      created: 'date',
      status: 'published',
      payload: {
        content: {text: '', media_id: ''},
        info: {important: false, priority: 0, show_as_fullscreen: false},
        preview: {title: `notification${i}`}
      }
    })
  )
}

describe('communications::notifications', () => {
  beforeEach(() => {
    setupIntersectionObserverMock()
  })

  afterAll(() => {
    jest.clearAllMocks()
  })

  test('notifications list rendering', async () => {
    const {asFragment, rerender} = await renderWithProviders(<Notifications notifications={[]} />)

    const emptyState = asFragment()
    expect(emptyState).toMatchSnapshot()

    rerender(<Notifications notifications={generateNotifications(3)} />)
    const filledState = asFragment()
    expect(filledState).toMatchSnapshot()

    rerender(<Notifications notifications={generateNotifications(5)} />)
    const collapsedState = asFragment()
    expect(collapsedState).toMatchSnapshot()
  })

  test('notifications list expands', async () => {
    const {asFragment, getByText} = await renderWithProviders(
      <Notifications notifications={generateNotifications(5)} />
    )

    const showAll = getByText('Показать все')
    expect(showAll).toBeTruthy()

    fireEvent(showAll, new MouseEvent('click', {bubbles: true}))

    const expandedState = asFragment()

    expect(expandedState).toMatchSnapshot()
  })

  test('metrica calls for notification', async () => {
    const metrika = new MetrikaStub()
    metrika.reachGoal = jest.fn()

    const notificationsCount = 3

    await renderWithProviders(<Notifications notifications={generateNotifications(notificationsCount)} />, {
      envOverrides: {
        metrika
      }
    })

    expect(metrika.reachGoal).toBeCalledTimes(notificationsCount)
  })
})
