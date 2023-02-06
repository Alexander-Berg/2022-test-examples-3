import {assert} from 'chai'
import {AuthSelectors} from '../../../page-objects/selectors.generated/AuthSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'
import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'

it('[webvendor-149] Авторизация с корректными данными и проставленным чекбоксом "Запомнить меня"', async function () {
  await this.browser.url('/')
  await this.browser.$(AuthSelectors.email).waitForDisplayed()
  await this.browser.$(AuthSelectors.email).setValue('hermione@vendor.com')
  await this.browser.$(AuthSelectors.password).setValue('hermione')
  await this.browser.$(AuthSelectors.submit).click()
  await this.browser.waitForHidden(UiSelectors.spinner, 10000)

  const ordersVisible = this.browser.$(OrdersSelectors.page).isDisplayed()
  assert(ordersVisible, 'Страница заказов не загрузилась')

  await this.browser.url('/')
  await this.browser.$(UiSelectors.pageContent).waitForDisplayed({timeout: 15000})
  await this.browser.logout()
  const email = await this.browser.$(AuthSelectors.email).getValue()
  assert(email.length === 0, 'Email не пустой')
  const password = await this.browser.$(AuthSelectors.password).getValue()
  assert(password.length === 0, 'Password не пустой')
})
