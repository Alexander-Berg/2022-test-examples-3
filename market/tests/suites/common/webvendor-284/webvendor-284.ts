import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'

it('[webvendor-284] Изменение адреса', async function () {
  await this.browser.authorize()
  const places = new PlacePO(this.browser)
  await places.open()
  await places.clickPlace('Бом-Бом')
  await places.openInformationTab()

  const oldAddress = await places.getAddress() // Россия, Оренбург, Знаменский проезд, 1/1
  const addressModal = await places.editAddress()

  const modalDisplayed = await addressModal.modal.isDisplayed()
  const inputDisplayed = await addressModal.input.isDisplayed()
  assert(modalDisplayed && inputDisplayed, 'Должно появится модальное окно с полем для ввода адреса')

  await addressModal.input.setValue('Пресненская набережная')
  const firstSuggestion = addressModal.getSuggestion(1)
  const newAddress = await firstSuggestion.getText()

  assert(newAddress !== oldAddress, 'Новый адрес должен отличаться от старого')

  await firstSuggestion.click()

  await addressModal.saveButton.click()
  const isSaveEnabled = await addressModal.saveButton.isEnabled()
  assert(!isSaveEnabled, 'Должна быть недоступна кнопка сохранить')
  await addressModal.modal.waitForDisplayed({reverse: true})

  await this.browser.$(UiSelectors.snackbarMessage).waitForDisplayed()
  // Ждем пока пройдет модерация
  await this.browser.$(PlaceSelectors.infoModerationBanner).waitForDisplayed()
  await this.browser.waitForHidden(PlaceSelectors.infoModerationBanner, 10000)
  // Следующий снэкбар, что данные обновились
  await this.browser.waitForHidden(UiSelectors.snackbarMessage, 10000)

  const currentAddress = await places.getAddress()

  assert(oldAddress !== currentAddress, 'Должен измениться адрес')
})
