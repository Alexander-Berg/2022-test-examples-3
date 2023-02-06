import React from 'react'
import {renderWithProviders} from 'core-legacy/test-utils'
import PromosStatisticsPage from './PromosStatisticsPage'
import * as hooks from 'main-promos/hooks'

describe('PromosStatisticsPage', () => {
  beforeAll(() => {
    jest.mock('core-di', () => ({promos: {hasPromos: false}}))
  })

  afterEach(() => {
    jest.clearAllMocks()
  })

  test('показываем плэйсхолдер ошибки', async () => {
    jest.spyOn(hooks, 'usePromosInitialization').mockImplementation(() => ({isLoading: false, error: new Error()}))

    const {findByText} = await renderWithProviders(<PromosStatisticsPage />, {})

    const placeholder = await findByText(
      'Не удалось получить информацию. Попробуйте ещё раз или обратитесь в поддержку'
    )

    expect(placeholder).toBeInTheDocument()
  })

  test('показываем спиннер', async () => {
    jest.spyOn(hooks, 'usePromosInitialization').mockImplementation(() => ({isLoading: true, error: undefined}))

    const {getByTestId} = await renderWithProviders(<PromosStatisticsPage />, {})

    const spinner = getByTestId('ui__spinner')

    expect(spinner).toBeInTheDocument()
  })

  test('показываем плэйсхолдер что нет статистики', async () => {
    jest.spyOn(hooks, 'usePromosInitialization').mockImplementation(() => ({isLoading: false, error: undefined}))

    const {getByText} = await renderWithProviders(<PromosStatisticsPage />, {})

    const placeholder = getByText('Тут появится ваша статистика по акциям')

    expect(placeholder).toBeInTheDocument()
  })
})
