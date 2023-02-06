import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'

const RESTAURANT_PHONE = '+79999999999'
const MANAGER_PHONE = '+79999999998'
const CLIENTS_PHONE = '+79999999997'

it('[webvendor-291] Изменение номера телефона', async function () {
  await this.browser.authorize()
  const place = new PlacePO(this.browser)
  await place.open()
  await place.clickPlace('Столовочка')
  await place.openInformationTab()

  const initialPhones = await place.getRequiredPhones() // Проверяем, что начальные номера отличаются от тех на которые будем менять
  assert(initialPhones.restaurant !== RESTAURANT_PHONE, 'Номер телефона ресторана должен отличаться от нового')
  assert(initialPhones.manager !== MANAGER_PHONE, 'Номер телефона управляющего должен отличаться от нового')
  assert(initialPhones.clients !== CLIENTS_PHONE, 'Номер телефона для клиентов должен отличаться от нового')

  const phoneEditModal = await place.editPhones()

  assert(await this.browser.$(UiSelectors.modal).isDisplayed(), 'Должно открыться модальное окно')

  assert(await phoneEditModal.isInputsVisible(), 'Должны отображаться все инпуты номеров')

  assert(await phoneEditModal.isSaveDisabled(), 'Кнопка сохранить должна быть задизейблена')

  await phoneEditModal.setRestaurantPhone(RESTAURANT_PHONE)
  await phoneEditModal.setManagerPhone(MANAGER_PHONE)
  await phoneEditModal.setClientsPhone(CLIENTS_PHONE)

  assert(await phoneEditModal.isSaveEnabled(), 'Кнопка сохранить должна быть активна')

  await this.browser.clearValue(phoneEditModal.restaurantPhoneField.input)

  await phoneEditModal.clickSave()

  const errorText = await this.browser.$(phoneEditModal.restaurantPhoneField.error).getText()
  assert(errorText === 'Не может быть пустым', 'Должно появится сообщение об ошибке')

  await phoneEditModal.setRestaurantPhone('!@#"|$*(&[]')

  const inputValue = await this.browser.$(phoneEditModal.restaurantPhoneField.input).getValue()
  assert(inputValue === '', 'Должно быть запрещено вводить спецсимволы в инпут номера')

  await phoneEditModal.setRestaurantPhone('+79999999999')
  await phoneEditModal.setRestaurantPhoneComment('Телефон ресторана')

  await phoneEditModal.clickSave()

  await this.browser.$(UiSelectors.snackbarMessage).waitForClickable()
  const successText = await this.browser.$(UiSelectors.snackbarMessage).getText()
  assert(
    successText === 'Проверяем изменения. Обновлённые данные скоро появятся здесь',
    'Должно появится уведомление, что телефоны сохранены'
  )

  // Ждем пока пройдет модерация
  await this.browser.$(PlaceSelectors.infoModerationBanner).waitForDisplayed()
  await this.browser.waitForHidden(PlaceSelectors.infoModerationBanner)

  await this.browser.waitForHidden(UiSelectors.snackbarMessage)

  // Следующий снэкбар, что данные обновились
  await this.browser.$(UiSelectors.snackbarMessage).waitForClickable()
  const updateText = await this.browser.$(UiSelectors.snackbarMessage).getText()
  assert(
    updateText ===
      `Обновили данные ресторана
«Столовочка»`,
    'Должен появится уведомление, что данные ресторана обновлены'
  )

  const updatedPhones = await place.getRequiredPhones()

  assert(updatedPhones.restaurant === RESTAURANT_PHONE, 'Должен обновится номер телефона ресторана')
  assert(updatedPhones.manager === MANAGER_PHONE, 'Должен обновится номер телефона управляющего')
  assert(updatedPhones.clients === CLIENTS_PHONE, 'Должен обновится номер телефона для клиентов')
})
