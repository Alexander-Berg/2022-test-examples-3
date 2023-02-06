import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

const sections = [
  'Рестораны',
  'Заказы',
  'Меню',
  'Статистика',
  'История',
  'График работы',
  'Время готовки',
  'Новости',
  'Поддержка',
  'База знаний',
  'Выход'
]

it('[webvendor-105] Пользователь с ролью "Оператор"', async function () {
  await this.browser.authorize('hermione-operator@yandex.ru', 'qwerty')

  await Promise.all(sections.map((name) => $(UiSelectors.sidebarSection(name)).waitForDisplayed()))
  await this.browser.assertView('OrdersFlow', OrdersSelectors.page)

  await this.browser.logout()

  await this.browser.assertView('LogoutSuccess', 'body')
})
