import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'
import {ScheduleSelectors} from '../../../page-objects/selectors.generated/ScheduleSelectors'
import moment from 'moment'
import {SchedulePO} from '../../../page-objects/SchedulePO'

it('[webvendor-289] Добавление расписания в праздничный день', async function () {
  await this.browser.authorize()
  const todayMock = new Date('2021-08-19T12:00:00.000Z')
  await this.browser.mockDate(todayMock)

  const place = new PlacePO(this.browser)
  await place.open()
  await place.clickPlace('Бом-Бом')
  await place.openInformationTab()
  await this.browser.waitForHidden(UiSelectors.spinner)

  await this.browser.$(PlaceSelectors.infoScheduleEditHolidayBtn).click()
  const scheduleModalOpened = await this.browser.$(UiSelectors.modal).isDisplayed()
  assert(scheduleModalOpened, 'Должно открываться модальное окно "Праздничные дни"')

  const schedule = new SchedulePO(this.browser)
  const dayRangePicker = await schedule.openDayRangePicker()

  const tomorrowDate = moment(todayMock).add(1, 'day')
  await dayRangePicker.selectRange(tomorrowDate)
  await dayRangePicker.apply()

  const dayRangeText = await this.browser.$(ScheduleSelectors.dayRangePicker).getText()
  const tomorrow = tomorrowDate.format('DD.MM.YYYY')
  assert(dayRangeText === `${tomorrow} — ${tomorrow}`, 'Должен отображаться выбранный период')

  await this.browser.$(ScheduleSelectors.timeRangeTooltip).click()
  const isWork24RadioVisible = await this.browser.$(ScheduleSelectors.work24).isDisplayed()
  const isNotWorkingRadioVisible = await this.browser.$(ScheduleSelectors.notWork).isDisplayed()

  assert(
    isWork24RadioVisible && isNotWorkingRadioVisible,
    'Должны отображаться radio кнопки "Работаем круглосуточно" и "Не работаем весь день"'
  )

  await this.browser.$(ScheduleSelectors.notWork).click()

  const isFromInputEnabled = await this.browser.$(ScheduleSelectors.timeFrom).isEnabled()
  const isToInputEnabled = await this.browser.$(ScheduleSelectors.timeTo).isEnabled()
  assert(!isFromInputEnabled && !isToInputEnabled, 'Должны быть задизейблены инпуты времени')

  const isSaveButtonEnabled = await this.browser.$(ScheduleSelectors.saveHolidaySchedule).isEnabled()
  assert(isSaveButtonEnabled, 'Должна быть активна кнопка сохранения')

  await this.browser.$(ScheduleSelectors.saveHolidaySchedule).click()

  const snackbar = this.browser.$(UiSelectors.snackbarMessage)
  await snackbar.waitForClickable()
  const successText = await snackbar.getText()
  assert(successText === 'График работы изменён', 'Должно появится уведомление, что график работы сохранен')
})
