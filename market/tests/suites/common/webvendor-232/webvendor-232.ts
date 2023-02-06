import {assert} from 'chai'
import {SitesSelectors} from '../../../page-objects/selectors.generated/SitesSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-232] У ресторана есть сайты', async function () {
  await this.browser.authorize()
  await this.browser.$(UiSelectors.sidebarSection('Ваш сайт')).click()

  await this.browser.waitForHidden(UiSelectors.spinner, 5000)

  const elements = await this.browser.$$(SitesSelectors.card)
  const title = await this.browser.$(SitesSelectors.headerTitle).getText()
  assert(title.includes(elements.length.toString()), 'Заголовок раздела соответствует количеству сайтов')

  const isTemplateVisible = await this.browser.$(SitesSelectors.templateCard).isDisplayed()
  assert(isTemplateVisible, 'На странице не отображается кнопка "Создать с нуля"')
})
