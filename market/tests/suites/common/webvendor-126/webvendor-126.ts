import {assert} from 'chai'
import {MenuPO} from '../../../page-objects/MenuPO'
import {MenuSelectors} from '../../../page-objects/selectors.generated/MenuSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-126] Удаление блюда и категории', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()
  const category = await menu.selectCategory('Десерты')

  await category.deletePosition('Тирамису')
  await menu.rejectDialog()
  let isVisible = await this.browser.$(MenuSelectors.item('Тирамису')).isDisplayed()
  assert(isVisible, 'Диалог не был подтвержден, но позиция удалилась')

  await category.deletePosition('Тирамису')
  await menu.confirmDialog()
  isVisible = await this.browser.$(MenuSelectors.item('Тирамису')).isDisplayed()
  assert(!isVisible, 'Диалог был подтвержден, но позиция НЕ удалилась')

  await menu.selectCategory('CategoryForRemove')
  await this.browser.assertView('BeforeCategoryDelete', UiSelectors.pageContent)
  await category.deleteCategory()
  await menu.rejectDialog()

  isVisible = await this.browser.$(MenuSelectors.categoryRoot).isDisplayed()
  assert(isVisible, 'Диалог не был подтвержден, категория не должна удалиться')

  await category.deleteCategory()
  await menu.confirmDialog(15000)
  await this.browser.$(UiSelectors.snackbarMessage).waitForDisplayed({timeout: 15000}) // Ждем всплывашки что меню сохранилось
  await this.browser.assertView('AfterCategoryDelete', UiSelectors.pageContent)
})
