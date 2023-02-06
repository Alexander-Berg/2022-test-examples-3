import {assert} from 'chai'
import {PlacePO} from '../../../page-objects/PlacePO'
import {PlaceSelectors} from '../../../page-objects/selectors.generated/PlaceSelectors'

it('[webvendor-288] Страница Рестораны (один ресторан)', async function () {
  await this.browser.authorize('hermione-single-place@mail.com', 'hermione')
  const place = new PlacePO(this.browser)
  await place.open()

  const nativeDeliveryIconVisible = await this.browser.$(PlaceSelectors.deliveryIconNative).waitForDisplayed()
  assert(nativeDeliveryIconVisible, 'Должна отображаться иконка доставки Яндекса')

  const disablePlaceBtnVisible = await this.browser.$(PlaceSelectors.actionButtonDisable).isDisplayed()
  assert(disablePlaceBtnVisible, 'Должна отображаться кнопка "Отключить" ресторан')

  const ratingTabVisible = await this.browser.$(PlaceSelectors.navigationTabsRating).isDisplayed()
  const reviewsTabVisible = await this.browser.$(PlaceSelectors.navigationTabsReviews).isDisplayed()
  const infoTabVisible = await this.browser.$(PlaceSelectors.navigationTabsInfo).isDisplayed()

  assert(ratingTabVisible && reviewsTabVisible && infoTabVisible, 'Должны отображаться табы навигации')
})
