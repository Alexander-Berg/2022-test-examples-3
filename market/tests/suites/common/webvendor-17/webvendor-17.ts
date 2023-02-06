import {assert} from 'chai'

import {MenuPO} from '../../../page-objects/MenuPO'
import {MenuSelectors} from '../../../page-objects/selectors.generated/MenuSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-17] Исключение категории из стоп-листа (web)', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()
  await menu.waitForLoad()
  const category = await menu.selectCategory('Закуски')
  assert((await category.getActiveCategoryLength()) === 5, 'Кол-во активных категорий')
  assert((await category.getDisableCategoryLength()) === 1, 'Кол-во неактивных категорий')

  const hasCategoryInDisabled = this.browser.$(MenuSelectors.disableCategoriesList).$(MenuSelectors.category('Закуски'))
  assert(await hasCategoryInDisabled.isDisplayed(), 'В разделе "Неактивные" отображаются отключенные категории меню')

  await menu.toggleSelectedCategory()
  await menu.clickSaveButton()
  await this.browser.$(UiSelectors.snackbarMessage).waitForDisplayed({timeout: 15000}) // Ждем всплывашки что меню сохранилось

  const hasCategoryInActive = this.browser.$(MenuSelectors.activeCategoriesList).$(MenuSelectors.category('Закуски'))

  assert(await hasCategoryInActive.isDisplayed(), 'Категория переместилась в раздел "Активные"')
  assert(category.getCheckedCheckbox(), 'Слева от названия категории проставился чекбокс')
  assert((await category.getActiveCategoryLength()) === 6, 'Кол-во не активных категорий')

  const disableCategoryContainer = await this.browser.$(MenuSelectors.disableCategoriesLength)

  assert(!(await disableCategoryContainer.isDisplayed()), 'Категория "Неактивные" больше не отображается')
})
