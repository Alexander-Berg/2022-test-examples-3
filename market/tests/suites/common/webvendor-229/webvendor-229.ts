import {assert} from 'chai'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-229] В меню отображается раздел "Ваш сайт"', async function () {
  await this.browser.authorize()
  const isDisplayed = await this.browser.$(UiSelectors.sidebarSection('Ваш сайт')).waitForDisplayed()
  assert(isDisplayed, 'В меню не отображается раздел "Ваш сайт"')
})
