import {AuthSelectors} from '../page-objects/selectors.generated/AuthSelectors'
import {UiSelectors} from '../page-objects/selectors.generated/UiSelectors'

export async function authorize(
  this: WebdriverIO.Browser,
  email = process.env.HERMIONE_LOGIN || 'hermione@vendor.com',
  password = process.env.HERMIONE_PASSWORD || 'hermione'
) {
  await this.url('/')
  const emailInput = this.$(AuthSelectors.email)
  const passwordInput = this.$(AuthSelectors.password)
  const submitBtn = this.$(AuthSelectors.submit)
  const pageContent = this.$(UiSelectors.pageContent)

  await emailInput.waitForDisplayed()
  await emailInput.setValue(email)
  await passwordInput.setValue(password)
  await submitBtn.click()

  await pageContent.waitForExist({timeout: 10000, timeoutMsg: 'PageContent не отрендерился в течении 10 секунд'})
}
