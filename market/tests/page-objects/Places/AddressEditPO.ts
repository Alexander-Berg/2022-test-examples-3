import {UiSelectors} from '../selectors.generated/UiSelectors'
import {PlaceSelectors} from '../selectors.generated/PlaceSelectors'

export class AddressEditPO {
  constructor(private browser: WebdriverIO.Browser) {}

  get modal() {
    return this.browser.$(UiSelectors.modal)
  }

  get input() {
    return this.modal.$('input')
  }

  get saveButton() {
    return this.browser.$(PlaceSelectors.saveAddressBtn)
  }

  get cancelButton() {
    return this.browser.$(PlaceSelectors.cancelEditAddressBtn)
  }

  getSuggestion(index: number) {
    return this.browser.$(`[role="option"][data-option-index="${index}"]`)
  }
}
