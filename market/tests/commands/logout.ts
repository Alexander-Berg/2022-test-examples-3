import {UiSelectors} from '../page-objects/selectors.generated/UiSelectors'
import {AuthSelectors} from '../page-objects/selectors.generated/AuthSelectors'

export async function logout(this: WebdriverIO.Browser) {
  await this.$(UiSelectors.sidebarSection('Выход'))
  const emailInput = this.$(AuthSelectors.email)
  await emailInput.waitForDisplayed({timeout: 10000})
}
