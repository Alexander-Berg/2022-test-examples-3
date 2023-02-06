import React from 'react'
import {renderWithProviders} from 'core-legacy/test-utils'
import DatePeriodSelector, {DatePeriod} from './DatePeriodSelector'
import {
  ALL_DATE_PERIOD,
  DEFAULT_DATE_PERIODS,
  SELECTABLE_PERIODS,
  MONTH_DATE_PERIOD,
  WEEK_DATE_PERIOD,
  CUSTOM_DATE_PERIOD
} from './constants'
import {fireEvent} from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import moment from 'moment'

describe('DatePeriodSelector', () => {
  const onChange = jest.fn()
  const baseDate = moment('2021-01-01')
  const testMin = moment('2020-01-01')
  const testMax = moment('2021-01-01')

  it('Показ недели(как пример дефолтного периода)', async () => {
    const {getByRole, getByText, findByText} = await renderWithProviders(
      <DatePeriodSelector
        baseDate={baseDate}
        min={testMin}
        max={testMax}
        periods={DEFAULT_DATE_PERIODS}
        value={{
          period: MONTH_DATE_PERIOD
        }}
        onChange={onChange}
      />
    )

    const trigger = getByRole('button')
    fireEvent.mouseDown(trigger)

    const option = getByText(SELECTABLE_PERIODS[WEEK_DATE_PERIOD])
    userEvent.click(option)

    await findByText(SELECTABLE_PERIODS[WEEK_DATE_PERIOD])
    expect(onChange).toBeCalledWith({
      period: WEEK_DATE_PERIOD,
      from: baseDate.subtract(WEEK_DATE_PERIOD),
      to: baseDate
    })
  })

  it('Показ кастомного периода', async () => {
    const CUSTOM_DATE_PERIODS = [...DEFAULT_DATE_PERIODS, ALL_DATE_PERIOD] as DatePeriod[]

    const {getByRole, getByText, findByText} = await renderWithProviders(
      <DatePeriodSelector
        min={testMin}
        max={testMax}
        periods={CUSTOM_DATE_PERIODS}
        value={{
          period: WEEK_DATE_PERIOD
        }}
        onChange={onChange}
      />
    )

    const trigger = getByRole('button')
    fireEvent.mouseDown(trigger)

    const option = getByText(SELECTABLE_PERIODS[ALL_DATE_PERIOD])
    userEvent.click(option)

    await findByText(SELECTABLE_PERIODS[ALL_DATE_PERIOD])
    expect(onChange).toBeCalledWith({
      period: ALL_DATE_PERIOD,
      from: testMin,
      to: testMax
    })
  })

  it('Показ только недели, за всё время и кастомной опций', async () => {
    const oneWeekTestMin = moment('2020-12-24')

    const {getByRole, findAllByRole} = await renderWithProviders(
      <DatePeriodSelector
        min={oneWeekTestMin}
        max={testMax}
        periods={[...DEFAULT_DATE_PERIODS, ALL_DATE_PERIOD]}
        value={{
          period: WEEK_DATE_PERIOD
        }}
        onChange={onChange}
      />
    )

    const trigger = getByRole('button')
    fireEvent.mouseDown(trigger)

    const options = await findAllByRole('option')
    const optionsText = options.map((option) => option.textContent)

    expect(optionsText).toStrictEqual(
      ([WEEK_DATE_PERIOD, CUSTOM_DATE_PERIOD, ALL_DATE_PERIOD] as (keyof typeof SELECTABLE_PERIODS)[]).map(
        (item) => SELECTABLE_PERIODS[item]
      )
    )
  })

  it('Показ всех периодов, вне зависимости от ограничения', async () => {
    const oneWeekTestMin = moment('2020-12-24')

    const {getByRole, findAllByRole} = await renderWithProviders(
      <DatePeriodSelector
        isAllPeriods
        min={oneWeekTestMin}
        max={testMax}
        value={{
          period: WEEK_DATE_PERIOD
        }}
        onChange={onChange}
      />
    )

    const trigger = getByRole('button')
    fireEvent.mouseDown(trigger)

    const options = await findAllByRole('option')
    const optionsText = options.map((option) => option.textContent)

    expect(optionsText).toStrictEqual(DEFAULT_DATE_PERIODS.map((item) => SELECTABLE_PERIODS[item]))
  })
})
