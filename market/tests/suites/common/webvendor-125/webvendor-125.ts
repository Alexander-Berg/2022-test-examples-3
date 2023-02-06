import {MenuPO} from '../../../page-objects/MenuPO'
import {MenuSelectors} from '../../../page-objects/selectors.generated/MenuSelectors'
import {assert} from 'chai'

hermione.config.testTimeout(100000)
it('[webvendor-125] Добавление опций к блюду', async function () {
  await this.browser.authorize()
  const menu = new MenuPO(this.browser)
  await menu.open()
  const category = await menu.selectCategory('Салаты')
  const position = await category.addPosition()

  await position.fill({name: 'Оливье 1'})
  await position.addOptionsGroup()
  await position.enterOptionGroupName('New Option Group')

  await position.removeOptionGroup()

  await position.addOptionsGroup()
  await position.enterOptionGroupName('OptionGroup1')
  await position.addOption()

  await position.toggleOptionRequired()
  await position.enterOptionName('Required Option')
  assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')

  await position.enterOptionName('')
  assert(await position.isSubmitDisabled(), 'Кнопка сохранения позиции должна быть НЕ активна')

  await position.enterOptionName('Option 1')

  await position.enterOptionPrice('!"№%:;(abc')
  let inputValue = await this.browser.$(MenuSelectors.positionOptionPriceInput).getValue()
  assert(inputValue.length === 0, 'В поле "Цена" опции не должны вводится символы и буквы')

  await position.enterOptionPrice('12345678')
  inputValue = await this.browser.$(MenuSelectors.positionOptionPriceInput).getValue()
  assert(inputValue === '1234567,00', 'Нельзя ввести значения больше чем 7 символов')

  assert(!(await position.isSubmitDisabled()), 'Кнопка сохранения позиции должна быть активна')

  await position.submit()

  await this.browser.wait(500)

  await category.addPosition()
  await position.fill({name: 'Оливье 2'})

  await position.addOptionsGroup()
  await position.enterOptionGroupName('New Option Group')
  await position.addOption()
  await position.enterOptionName('Not required option')
  await position.submit()

  await category.toggleExpandPosition('Оливье 1')
  await category.toggleExpandPosition('Оливье 2')
})
