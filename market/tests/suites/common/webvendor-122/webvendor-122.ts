import {assert} from 'chai'
import {MenuPO} from '../../../page-objects/MenuPO'
import {MenuSelectors} from '../../../page-objects/selectors.generated/MenuSelectors'

it('[webvendor-122] Добавление/Редактирование категорий в меню', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()
  let categoryEditModal = await menu.clickAddCategory()

  const steps3to11 = async (type: 'add' | 'edit') => {
    await categoryEditModal.openCategoriesListSelect()
    await categoryEditModal.clickCategoriesSelectOption('Ланч')
    assert(
      !(await categoryEditModal.isSubmitDisabled()),
      'Кнопка должна быть активна. Категории еще нет в списке меню.'
    )

    await this.browser.assertView(`${type}-step3-Select-NoValidationErrors`, MenuSelectors.categoryNameControl)

    await categoryEditModal.openCategoriesListSelect()
    await categoryEditModal.clickCategoriesSelectOption('Салаты')
    assert(await categoryEditModal.isSubmitDisabled(), 'Кнопка должна быть НЕ активна. Категория уже есть списке меню.')

    await this.browser.assertView(`${type}-step4-Select-CategoryAlreadyExists`, MenuSelectors.categoryNameControl)

    await categoryEditModal.clickCustomCategoryRadio()
    let inputValue = await this.browser.$(MenuSelectors.inputCategoryName).getValue()
    assert(
      inputValue === 'Салаты',
      'Название категории из чекбокса "Выбрать из списка" НЕ прокидывается в чекбокс "Своя категория"'
    )

    await categoryEditModal.enterCategoryName('Особая категория')
    assert(!(await categoryEditModal.isSubmitDisabled()), 'Кнопка "Добавить" не стала активной')

    await categoryEditModal.enterCategoryName('Салаты')
    assert(
      await categoryEditModal.isSubmitDisabled(),
      'Кнопка "Добавить" должна быть НЕ активна. Категория уже есть списке меню.'
    )
    await this.browser.assertView(`${type}-step8-Input-CategoryAlreadyExists`, MenuSelectors.categoryNameControl)

    const longCategoryName = ''.padEnd(90, 'f')
    await categoryEditModal.enterCategoryName(longCategoryName)
    inputValue = await this.browser.$(MenuSelectors.inputCategoryName).getValue()
    assert(inputValue.length === 80, 'Название не может превышать 80 символов.')
    assert(!(await categoryEditModal.isSubmitDisabled()), 'Кнопка "Добавить" не стала активной')

    await this.browser.clearValue(MenuSelectors.inputCategoryName)
    assert(await categoryEditModal.isSubmitDisabled(), 'Кнопка "Добавить" должна быть НЕ активна. Пустое название')
    await this.browser.assertView(`${type}-step10-Input-CategoryMustHaveName`, MenuSelectors.categoryNameControl)

    await categoryEditModal.enterCategoryName(' ')
    assert(
      await categoryEditModal.isSubmitDisabled(),
      'Кнопка "Добавить" должна быть НЕ активна. Только пробел в названии'
    )
  }

  await steps3to11('add')

  await categoryEditModal.close()

  // Редактирование готовой категории
  const category = await menu.selectCategory('Пицца')
  categoryEditModal = await category.editCategory()

  await steps3to11('edit')
})
