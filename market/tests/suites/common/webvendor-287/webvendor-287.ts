import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'
import {OPERATOR} from '../../../utils/users'

it('[webvendor-287] Вкладка Информация с ролью Оператор', async function () {
  await this.browser.authorize(OPERATOR.email, OPERATOR.password)
  const place = new PlacePO(this.browser)
  await place.open()
  await place.openInformationTab()

  const editHolidayScheduleBtnVisible = await this.browser
    .$(PlaceSelectors.infoScheduleEditHolidayBtn)
    .waitForDisplayed()

  assert(editHolidayScheduleBtnVisible, 'Должна отображаться кнопка изменения расписания праздничных дней')

  const infoFields = [
    'Название',
    'Адрес',
    'Телефон',
    'Эл. почта',
    'Способ оплаты',
    'Как пройти курьеру',
    'Как пройти клиенту'
  ]

  const fieldEditBtnsVisibility = await Promise.all(
    infoFields.map((field) => this.browser.$(PlaceSelectors.commonInfoBtnEdit(field)).isDisplayed())
  )

  assert(
    fieldEditBtnsVisibility.every((visible) => !visible),
    'Должны быть скрыты все кнопки изменения общей информации для пользователя с ролью "Оператор"'
  )
})
