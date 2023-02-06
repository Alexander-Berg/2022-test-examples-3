import {PlaceSelectors} from '../selectors.generated/PlaceSelectors'

const inputSelector = (selector: string): string => selector + ' input'
const errorSelector = (selector: string): string => selector + ' p.Mui-error'

const field = (selector) => ({
  input: inputSelector(selector),
  error: errorSelector(selector)
})

export class PhonesEditPO {
  readonly restaurantPhoneField = field(PlaceSelectors.phoneField('Ресторана'))
  readonly additionalPhoneField = field(PlaceSelectors.phoneField('Дополнительный'))
  readonly managerPhoneField = field(PlaceSelectors.phoneField('Директор'))
  readonly clientsPhoneField = field(PlaceSelectors.phoneField('Для клиентов'))

  constructor(private browser: WebdriverIO.Browser) {}

  get allInputsSelectors() {
    return [
      this.restaurantPhoneField.input,
      this.additionalPhoneField.input,
      this.managerPhoneField.input,
      this.clientsPhoneField.input
    ]
  }

  isInputsVisible(): Promise<boolean> {
    return Promise.all(
      this.allInputsSelectors.map((selector) => this.browser.$(selector).isDisplayed())
    ).then((visibility) => visibility.every((visible) => visible))
  }

  setRestaurantPhone(value: string) {
    return this.editField(this.restaurantPhoneField.input, value)
  }

  setRestaurantPhoneComment(value: string) {
    return this.editField(inputSelector(PlaceSelectors.phoneCommentField('Ресторана')), value)
  }

  setAdditionalPhone(value: string) {
    return this.editField(this.additionalPhoneField.input, value)
  }

  setManagerPhone(value: string) {
    return this.editField(this.managerPhoneField.input, value)
  }

  setClientsPhone(value: string) {
    return this.editField(this.clientsPhoneField.input, value)
  }

  async isSaveDisabled() {
    return !(await this.isSaveEnabled())
  }

  async isSaveEnabled() {
    return this.browser.$(PlaceSelectors.savePhonesBtn).isEnabled()
  }

  clickSave() {
    return this.browser.$(PlaceSelectors.savePhonesBtn).click()
  }

  private async editField(inputSelector: string, value: string) {
    await this.browser.clearValue(inputSelector)
    await this.browser.$(inputSelector).setValue(value)
  }
}
