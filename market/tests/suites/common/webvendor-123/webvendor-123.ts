import {MenuPO} from '../../../page-objects/MenuPO'
import {assert} from 'chai'
import {MenuSelectors} from '../../../page-objects/selectors.generated/MenuSelectors'

hermione.config.testTimeout(100000)
it('[webvendor-123] Добавление/Редактирование блюда', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()

  // 1
  const editCategoryModal = await menu.clickAddCategory()
  let position = await editCategoryModal.submitCategory()

  const step2to9 = async () => {
    await this.browser.$(MenuSelectors.positionNameInput).waitForDisplayed()
    // 2
    await position.fill()
    // await position.uploadPhoto(path.resolve(__dirname, 'position-image.jpeg'))
    assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')
    await this.browser.wait(1000)

    // 3
    await position.enterName('')
    assert(await position.isSubmitDisabled(), 'Нет названия, Кнопка сохранения позиции должна быть НЕ активна')

    // 4
    await position.enterName(''.padEnd(260, ' Name'))
    const inputValue = await this.browser.$(MenuSelectors.positionNameInput).getValue()
    assert(inputValue.length === 255, 'Название должно обрезаться до 255 символов')
    assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')

    // 5
    await position.enterContent('')
    assert(await position.isSubmitDisabled(), 'Нет состава, Кнопка сохранения позиции должна быть НЕ активна')

    // 6
    await position.enterContent('!"№%:,.;()'.padEnd(300, ' состав'))
    assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')

    // 7
    await position.enterWeight('')
    await position.enterPrice('')
    assert(await position.isSubmitDisabled(), 'Нет веса и состава, Кнопка сохранения позиции должна быть НЕ активна')

    // 8
    await position.enterWeight('only digits!@%;')
    let weightValue = await this.browser.$(MenuSelectors.positionWeightInput).getValue()
    assert(weightValue.length === 0, 'В поле "Вес" можно вводить только цифры')

    await position.enterWeight('12345678')
    weightValue = await this.browser.$(MenuSelectors.positionWeightInput).getValue()
    assert(weightValue === '1234567', 'В поле "Вес" можно вводить не более 7 цифр')

    await position.enterWeight('3.25')
    weightValue = await this.browser.$(MenuSelectors.positionWeightInput).getValue()
    assert(weightValue === '3.25', 'В поле "Вес" можно вводить десятичные значения')

    await position.enterPrice('only digits!@%;')
    let priceValue = await this.browser.$(MenuSelectors.positionPriceInput).getValue()
    assert(priceValue.length === 0, 'В поле "Цена" можно вводить только цифры')

    await position.enterPrice('12345678')
    priceValue = await this.browser.$(MenuSelectors.positionPriceInput).getValue()
    assert(priceValue === '1234567,00', 'В поле "Цена" можно вводить не более 7 цифр до запятой')

    assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')

    // 9
    await position.submit()
  }

  // mode new
  await step2to9()

  // 11
  const category = await menu.selectCategory('Салаты')
  position = await category.editPosition('Салат Греческий')

  // mode edit
  await step2to9()
})
