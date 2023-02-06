import {SitesSelectors} from '../../../page-objects/selectors.generated/SitesSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-233] Создание сайта', async function () {
  const {browser} = this
  await browser.authorize()

  await browser.$(UiSelectors.sidebarSection('Ваш сайт')).click()

  const templateCard = browser.$(SitesSelectors.templateCard)
  await templateCard.waitForDisplayed()
  await templateCard.click()

  await browser.$('iframe').waitForDisplayed()

  const backToTemplates = browser.$(SitesSelectors.backToTemplates)
  await backToTemplates.waitForDisplayed()
  await backToTemplates.click()

  await browser.$(SitesSelectors.card).waitForDisplayed()
})
