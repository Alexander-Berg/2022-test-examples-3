import {AuthSelectors} from '../../../page-objects/selectors.generated/AuthSelectors'
import {MailCatcherPO, MailCatcherSelectors} from '../../../page-objects/MailCatcherPO'
import {assert} from 'chai'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {DesiredCapabilities} from '@wdio/types/build/Capabilities'

it('[webvendor-3] Сброс пароля', async function (this: {browser: WebdriverIO.Browser}) {
  const browserName = (this.browser.capabilities as DesiredCapabilities).browserName
  const email = `reset-password@${browserName}.com`
  await this.browser.url('/')
  const forgotPasswordBtn = this.browser.$(AuthSelectors.forgotPassword)
  await forgotPasswordBtn.waitForDisplayed()
  await forgotPasswordBtn.click()

  const emailInputForPasswordReset = this.browser.$(AuthSelectors.resetPasswordEmail)
  await emailInputForPasswordReset.waitForDisplayed()
  await emailInputForPasswordReset.setValue(email)

  await this.browser.$(AuthSelectors.restorePasswordBtn).click()

  const mailCatcher = new MailCatcherPO(this.browser)
  await mailCatcher.open()
  await mailCatcher.filterMails(email)
  await mailCatcher.clickFirstMail()
  await this.browser.assertView('ResetPasswordRequestMail', 'iframe')
  await mailCatcher.focusMailIframe()

  await this.browser.$(MailCatcherSelectors.ResetPasswordLink).waitForDisplayed()
  const resetPasswordUrl = await this.browser.$(MailCatcherSelectors.ResetPasswordLink).getAttribute('href')
  const url = new URL(resetPasswordUrl)

  await this.browser.url(url.pathname + url.search)

  const resetLoginBtn = this.browser.$(AuthSelectors.resetLoginBtn)
  await resetLoginBtn.waitForDisplayed()
  await resetLoginBtn.click()

  await mailCatcher.open()
  await mailCatcher.filterMails(email)
  await mailCatcher.clickFirstMail()
  await mailCatcher.focusMailIframe()

  const html = await this.browser.$('.email__text:nth-child(3)').getHTML()
  const newPassword = /(?<=Новый пароль для входа в сервис "Яндекс.Еда для ресторанов": )[\w]{6}/.exec(html)[0]
  assert(newPassword, 'Не удалось найти пароль в письме')

  await this.browser.url('/')

  const emailInput = this.browser.$(AuthSelectors.email)
  const passwordInput = this.browser.$(AuthSelectors.password)
  const submitBtn = this.browser.$(AuthSelectors.submit)

  await emailInput.waitForDisplayed()
  await emailInput.setValue(email)

  await passwordInput.setValue(newPassword)
  await submitBtn.click()

  await this.browser.$(UiSelectors.pageContent).waitForDisplayed({timeout: 15000})
})
