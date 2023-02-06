import {POBase} from './POBase'
import {PlaceSelectors} from './selectors.generated/PlaceSelectors'
import {UiSelectors} from './selectors.generated/UiSelectors'
import {PhonesEditPO} from './Places/PhonesEditPO'
import {AddressEditPO} from './Places/AddressEditPO'

export class PlacePO extends POBase {
  readonly pageName: string = 'Рестораны'

  openRatingTab() {
    return this.browser.$(PlaceSelectors.navigationTabsRating).click()
  }

  openReviewsTab() {
    return this.browser.$(PlaceSelectors.navigationTabsReviews).click()
  }

  async openInformationTab() {
    await this.browser.$(PlaceSelectors.navigationTabsInfo).click()
    await this.browser.waitForHidden(UiSelectors.spinner, 15000)
  }

  clickPlace(placeName: string) {
    return this.browser.$(PlaceSelectors.card(placeName)).click()
  }

  async editPhones() {
    await this.browser.$(PlaceSelectors.commonInfoBtnEdit('Телефон')).click()
    return new PhonesEditPO(this.browser)
  }

  async editAddress() {
    await this.browser.$(PlaceSelectors.commonInfoBtnEdit('Адрес')).click()
    return new AddressEditPO(this.browser)
  }

  getAddress() {
    return this.browser.$(PlaceSelectors.commonInfoValue('Адрес')).getText()
  }

  async getRequiredPhones() {
    const [restaurant, manager, clients] = await Promise.all(
      ['Ресторан', 'Директор', 'Для клиентов'].map((title) =>
        this.browser.$(PlaceSelectors.infoPhoneNumber(title)).getText()
      )
    )

    return {restaurant, manager, clients}
  }
}
