import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

const sections = [
  'Рестораны',
  'Заказы',
  'Меню',
  'Статистика',
  'История',
  'График работы',
  'Зоны доставки',
  'Продвижение',
  'Время готовки',
  'Акции',
  'Новости',
  'Ваш сайт',
  'Поддержка',
  'База знаний',
  'Выход'
]

it('[webvendor-112] Пользователь с обеими ролями', async function () {
  await this.browser.url('/')
  await this.browser.authorize('hermione-owner@yandex.ru', 'qwerty')

  await Promise.all(
    sections.map((name) => this.browser.$(UiSelectors.sidebarSection(name)).waitForDisplayed({timeout: 10000}))
  )
  await this.browser.assertView('OrdersFlow', OrdersSelectors.page)

  await this.browser.logout()

  await this.browser.assertView('LogoutSuccess', 'body')
})
