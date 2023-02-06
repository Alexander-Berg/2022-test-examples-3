import {POBase} from './POBase'
import {UiSelectors} from './selectors.generated/UiSelectors'
import {ScheduleSelectors} from './selectors.generated/ScheduleSelectors'
import moment from 'moment'
import 'moment/locale/ru'

moment().locale('ru')

type Days = 'Понедельник' | 'Вторник' | 'Среда' | 'Четверг' | 'Пятница' | 'Суббота' | 'Воскресенье'

export class SchedulePO extends POBase {
  readonly pageName = 'График работы'

  waitForLoad() {
    return this.browser.waitForHidden(UiSelectors.spinner, 10000)
  }

  addPeriod() {
    return this.browser.$(ScheduleSelectors.addMoreDaysBtn).click()
  }

  getPeriodSelector(periodIndex: number) {
    return `${ScheduleSelectors.periodSelectorsContainer}:nth-child(${periodIndex})`
  }

  async getPeriodsCount(): Promise<number> {
    const periods = await this.browser.$$(ScheduleSelectors.periodSelectorsContainer)
    return periods.length
  }

  /* Основной график работы */
  async selectDaysForPeriod(periodIndex: number, days: Days[]) {
    await this.browser.$(`${this.getPeriodSelector(periodIndex)} ${ScheduleSelectors.weekSelector}`).click()

    for (const day of days) {
      await this.browser.$(ScheduleSelectors.weekSelectorOption(day)).click()
    }

    await this.browser.$('body').click() // Скрыть селектор дней
    await this.browser.waitForHidden('[role="presentation"]', 1000)
  }

  async openTimeRangeTooltip(periodIndex: number) {
    await this.browser.$(`${this.getPeriodSelector(periodIndex)} ${ScheduleSelectors.timeRangeTooltip}`).click()
    await this.browser.$(ScheduleSelectors.work24).waitForDisplayed()
  }

  getWeekSelectorValue(periodIndex: number) {
    return this.browser
      .$(`${this.getPeriodSelector(periodIndex)} ${ScheduleSelectors.weekSelector} div[role="button"]`)
      .getText()
  }

  /* Праздничный график работы */
  async openDayRangePicker() {
    await this.browser.$(ScheduleSelectors.dayRangePicker).click()

    const close = async (apply: boolean) => {
      await this.browser.$(apply ? ScheduleSelectors.datePeriodModalOk : ScheduleSelectors.datePeriodModalClose).click()
      await this.browser.waitForHidden('.DayPicker', 2000)
    }

    return {
      selectRange: async (from: moment.Moment, to?: moment.Moment) => {
        await this.browser.$(`[aria-label="${from.format('ddd DD MMM YYYY г.')}"]`).click() // чт 19 авг. 2021 г.
        if (to) {
          await this.browser.$(`[aria-label="${to.format('ddd DD MMM YYYY г.')}"]`).click()
        }
      },
      apply: () => close(true),
      close: () => close(false)
    }
  }
}
