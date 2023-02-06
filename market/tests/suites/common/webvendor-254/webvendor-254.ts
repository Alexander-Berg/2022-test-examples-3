import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {ScheduleSelectors} from '../../../page-objects/selectors.generated/ScheduleSelectors'
import {SchedulePO} from '../../../page-objects/SchedulePO'

it('[webvendor-254] Изменение основного графика работы', async function () {
  await this.browser.authorize()
  await this.browser.mockDate(new Date('2021-08-18T10:00:00.000Z'))
  const place = new PlacePO(this.browser)
  await place.open()
  await place.clickPlace('Бом-Бом')
  await place.openInformationTab()
  await this.browser.waitForHidden(UiSelectors.spinner, 5000)

  await this.browser.$(PlaceSelectors.infoScheduleEditWorkdaysBtn).click()

  const scheduleModalOpened = await this.browser.$(UiSelectors.modal).isDisplayed()
  assert(scheduleModalOpened, 'Должно открываться модальное окно "Основной график работы"')

  const schedule = new SchedulePO(this.browser)
  await schedule.addPeriod()

  const lastPeriodIndex = await schedule.getPeriodsCount()
  await schedule.selectDaysForPeriod(lastPeriodIndex, ['Понедельник', 'Вторник'])

  const isDaysListVisible = await this.browser.$('[role="presentation"]').isDisplayed()
  assert(!isDaysListVisible, 'Должен скрыться селектор выбора дней')

  const selectorValue = await schedule.getWeekSelectorValue(lastPeriodIndex)
  assert(selectorValue === 'Пн, Вт', 'Должны появится дни в селекторе дней')

  await schedule.openTimeRangeTooltip(lastPeriodIndex)

  const isTooltipOpened = await this.browser.$('[role="tooltip"]').isDisplayed()
  assert(isTooltipOpened, 'Должен открыться тультип с выбором интервала времени')

  const isWork24CheckboxVisible = await this.browser.$(ScheduleSelectors.work24).isDisplayed()
  assert(isWork24CheckboxVisible, 'Должен отображаться чекбокс "Работаем круглосуточно"')

  await this.browser.$(ScheduleSelectors.work24).click()

  const isFromInputEnabled = await this.browser.$(ScheduleSelectors.timeFrom).isEnabled()
  const isToInputEnabled = await this.browser.$(ScheduleSelectors.timeTo).isEnabled()

  assert(!isFromInputEnabled && !isToInputEnabled, 'Должны быть задизейблены инпуты времени')

  let isSaveButtonEnabled = await this.browser.$(ScheduleSelectors.saveWorkdaysSchedule).isEnabled()
  assert(isSaveButtonEnabled, 'Должна быть активна кнопка "Сохранить"')

  await this.browser.$(ScheduleSelectors.saveWorkdaysSchedule).click()
  isSaveButtonEnabled = await this.browser.$(ScheduleSelectors.saveWorkdaysSchedule).isEnabled()
  assert(!isSaveButtonEnabled, 'Должна задизейблиться кнопка сохранить')

  const snackbar = this.browser.$(UiSelectors.snackbarMessage)
  await snackbar.waitForClickable()
  const successText = await snackbar.getText()
  assert(successText === 'График работы изменён', 'Должно появится уведомление, что график работы сохранен')
})
