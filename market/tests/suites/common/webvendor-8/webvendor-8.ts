import assert from 'assert'
import {OrdersPO} from '../../../page-objects/OrdersPO'
import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

hermione.config.testTimeout(120000) // В конце длинное ожидание
it('[webvendor-8] Заказ ASAP (наш ресторан)', async function () {
  await this.browser.authorize()
  await this.browser.mockSocket()
  await this.browser.mockDate(new Date(2021, 8, 21, 13, 23))

  /** Шаг 1: Сформировать новый ASAP заказ в ресторане c платной доставкой. */
  // Указать количество приборов - 3.
  // Добавить комментарий
  // Оплатить заказ
  // Назначить курьера

  const orders = new OrdersPO(this.browser)
  const orderId = '210921-379729'
  await orders.open()
  const order = orders.getItemById(orderId)
  await order.waitForDisplayed()

  assert.strictEqual(
    await order.$(OrdersSelectors.listItemStatus).getText(),
    'Новый заказ',
    'В кратком описании заказа указан статус заказа'
  )

  await this.browser.waitUntil(async () => {
    const price = await order.$(OrdersSelectors.listItemTitle).getText()

    return price === '730.00 ₽'
  })

  await order.scrollIntoView()

  assert.strictEqual(
    await order.$(OrdersSelectors.listItemTitle).getText(),
    '730.00 ₽',
    'В кратком описании заказа указана стоимость заказа'
  )
  assert.strictEqual(
    await order.$(OrdersSelectors.listItemDelivery).getText(),
    'Доставка Яндекса',
    'В кратком описании заказа указан тип доставки'
  )
  assert(
    order.$(OrdersSelectors.listItemProgress).isDisplayed(),
    'В кратком описании заказа должен быть таймер с оставшимся временем для готовки'
  )

  /** Шаг 2: Нажать на новый заказ в общем списке заказов. */
  await order.click()

  // Кнопка "Принять заказ" доступна
  const {primaryButton} = orders
  await primaryButton.waitForEnabled()
  assert.strictEqual(
    await orders.primaryButton.$(OrdersSelectors.buttonPrimaryActionText).getText(),
    'Принять заказ',
    'Кнопка "Принять заказ" доступна'
  )

  // В карточке заказа отображаются:
  assert.strictEqual(
    await orders.statusBadge.getText(),
    'Новый',
    'В карточке заказа отображается статус заказа - "Новый"'
  )
  assert.strictEqual(
    await orders.deliveryBadge.getText(),
    'Доставка Яндекса',
    'В карточке заказа отображается тип доставки - "Доставка ресторана"'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderId).getText(),
    orderId,
    'В карточке заказа отображается номер заказа'
  )
  assert(
    await orders.callbackButton.isClickable(),
    'В карточке заказа отображается кнопка с иконкой телефонной трубки (отображается и кликабельная)'
  )
  assert(
    await orders.printButton.isClickable(),
    'В карточке заказа отображается кнопка с иконкой принтера (отображается и кликабельная)'
  )
  assert(
    await orders.chatButton.isClickable(),
    'В карточке заказа отображается кнопка чатов поддержки (отображается и кликабельная)'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderRestaurantTitle).getText(),
    'Кофемания NEW (Москва, Лесная улица, 5)',
    'В карточке заказа отображается название и адрес ресторана'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderPrepareFor),
    '13:37',
    'В карточке заказа отображается время, к которому необходимо приготовить заказ'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderClientInfo),
    'Yandex.Eda 8 800 600 12 10',
    'В карточке заказа отображается информация о клиенте'
  )
  assert.strictEqual(
    await orders.getOrderPropertyByLabel('Курьер:'),
    'Тестер',
    'В карточке заказа отображается имя курьера'
  )
  assert.strictEqual(
    await orders.getOrderPropertyByLabel('Телефон:'),
    '+7 987 121 78 38',
    'В карточке заказа отображается телефон курьера'
  )
  assert(
    await this.browser.$(OrdersSelectors.orderMapButton).isClickable(),
    'В карточке заказа отображается кнопка "Показать карту"'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderPersonsBadge).getText(),
    '3',
    'В карточке заказа отображается количество приборов'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderComment).getText(),
    'Звонок не работает, постучите, пожалуйста, в дверь',
    'В карточке заказа отображается комментарий'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderPositionName).getText(),
    '1 x Гречневая лапша с креветками',
    'В карточке заказа отображается состав заказа'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderDeliveryCost).getText(),
    '0.00 ₽',
    'В карточке заказа отображается стоимость доставки'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderTotal).getText(),
    '730.00 ₽',
    'В карточке заказа отображается общая стоимость заказа'
  )

  /** Шаг 3: Нажать на кнопку "Принять заказ" */
  await primaryButton.click()
  await primaryButton.waitForEnabled()

  assert.strictEqual(await orders.statusBadge.getText(), 'В работе', 'Статус заказа меняется с "Новый" на "В работе".')
  assert(await orders.cancelButton.isDisplayed(), 'Появляется кнопка "Отменить заказ"')
  assert.strictEqual(
    await orders.primaryButton.$(OrdersSelectors.buttonPrimaryActionText).getText(),
    'Заказ готов',
    'Появляется кнопка "Заказ готов"'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderConfirmationTime),
    '13:23',
    'Появляется время принятия в работу'
  )

  /** Шаг 4: Кликнуть по кнопке "Заказ готов" */
  await primaryButton.click()
  await orders.confirmDialog() // Нажать на кнопку "Подтвердить" в модальном окне.
  await primaryButton.waitForEnabled()

  assert.strictEqual(await orders.statusBadge.getText(), 'Готов к выдаче', 'Заказ меняет статус на "Готов к выдаче".')
  assert.strictEqual(
    await orders.primaryButton.$(OrdersSelectors.buttonPrimaryActionText).getText(),
    'Заказ передан',
    'Появляется кнопка "Заказ передан"'
  )

  /** Шаг 5: Нажать на кнопку "Заказ передан" */
  await primaryButton.click()
  assert(
    (await this.browser.$(UiSelectors.dialogApplyButton).isDisplayed()) &&
      (await this.browser.$(UiSelectors.dialogCancelButton).isDisplayed()),
    'Появляется модальное окно подтверждения с кнопками "Подтвердить" и "Отменить"'
  )

  /** Шаг 6: Нажать на кнопку “Подтвердить” в модальном окне. */
  await orders.confirmDialog() // в появившемся попапе кликнуть Подтвердить
  await primaryButton.waitForDisplayed({reverse: true})
  assert.strictEqual(await orders.statusBadge.getText(), 'Заказ передан', 'Заказ меняет статус на "Заказ передан"".')

  /** Шаг 7: В админке перевести заказ в статус “Курьер забрал заказ” */
  await orders.waitStatus('Завершен')
  assert.strictEqual(await orders.statusBadge.getText(), 'Завершен', 'В вендорке заказ меняет статус на "Завершен".')

  assert.strictEqual(
    await orders.getItemById(orderId).$(OrdersSelectors.listItemId).getText(),
    `№ ${orderId}`,
    'Заказ перемещается в раздел списка заказов "Выполнены".'
  )
})
