import {MenuPO} from '../../../page-objects/MenuPO'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-16] Добавление категории в стоп-лист (web)', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()
  await menu.waitForLoad()
  await menu.selectCategory('Закуски')
  await this.browser.assertView('MenuCategory', UiSelectors.pageContent)
  await menu.toggleSelectedCategory()
  await menu.clickSaveButton()

  const snackbar = this.browser.$(UiSelectors.snackbarMessage)
  await snackbar.waitForDisplayed({timeout: 20000}) // Ждем всплывашки что меню сохранилось
  await this.browser.assertView('MenuCategoryDisabled', UiSelectors.pageContent)
})
