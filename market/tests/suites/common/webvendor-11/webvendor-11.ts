import assert from 'assert'
import {OrdersPO} from '../../../page-objects/OrdersPO'
import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'

it('[webvendor-11] Заказ ASAP (маркетплейс)', async function () {
  await this.browser.authorize()
  await this.browser.mockDate(new Date(2021, 7, 31, 23, 20))
  const orders = new OrdersPO(this.browser)
  await orders.open()

  /** Шаг 1: Сформировать новый заказ. */
  // Заказ появляется в списке заказов.
  let order = orders.activeOrders.$(OrdersSelectors.listItem)
  await order.waitForDisplayed()

  await this.browser.waitUntil(async () => {
    const price = await order.$(OrdersSelectors.listItemTitle).getText()

    return price === '900.00 ₽'
  })

  assert.strictEqual(
    await order.$(OrdersSelectors.listItemStatus).getText(),
    'Новый заказ',
    'В кратком описании заказа указан статус заказа'
  )
  assert.strictEqual(
    await order.$(OrdersSelectors.listItemTitle).getText(),
    '900.00 ₽',
    'В кратком описании заказа указана стоимость заказа'
  )
  assert.strictEqual(
    await order.$(OrdersSelectors.listItemDelivery).getText(),
    'Доставка ресторана',
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
    'Доставка ресторана',
    'В карточке заказа отображается тип доставки - "Доставка ресторана"'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderId).getText(),
    '210831-194993',
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
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderRestaurantTitle).getText(),
    'Цурцум кафе // Zurzum cafe (Москва, 4-й Сыромятнический переулок, 1/8с6)',
    'В карточке заказа отображается название и адрес ресторана'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderDeliveryFor),
    '00:52',
    'В карточке заказа отображается время, к которому доставить заказ'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderPaymentInfo),
    'Безналичная',
    'В карточке заказа отображается вид оплаты'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderClientInfo),
    'Артур Удалов +7 968 892 43 53',
    'В карточке заказа отображаются имя и телефон клиента'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderClientAddress),
    'Москва, улица Льва Толстого, 16',
    'В карточке заказа отображается адрес клиента'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderPersonsBadge).getText(),
    '0',
    'В карточке заказа отображается количество приборов'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderPositionName).getText(),
    '1 x Греческий салат',
    'В карточке заказа отображается состав заказа'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderDeliveryCost).getText(),
    '100.00 ₽',
    'В карточке заказа отображается стоимость доставки'
  )
  assert.strictEqual(
    await this.browser.$(OrdersSelectors.orderTotal).getText(),
    '900.00 ₽',
    'В карточке заказа отображается общая стоимость заказа'
  )

  /** Шаг 3: Кликнуть по кнопке Принять заказ. */
  await primaryButton.click()
  await primaryButton.waitForEnabled()

  assert.strictEqual(await orders.statusBadge.getText(), 'В работе', 'Статус заказа меняется с "Новый" на "В работе".')
  assert(await orders.cancelButton.isDisplayed(), 'Появляется кнопка "Отменить заказ"')
  assert.strictEqual(
    await orders.primaryButton.$(OrdersSelectors.buttonPrimaryActionText).getText(),
    'Передать курьеру',
    'Появляется кнопка "Передать курьеру"'
  )
  assert.strictEqual(
    await orders.getOrderPropertyValue(OrdersSelectors.orderConfirmationTime),
    '23:20',
    'Появляется время принятия в работу'
  )

  /** Шаг 4: Кликнуть по кнопке "Передать курьеру" */
  await primaryButton.click()
  await orders.confirmDialog() // в появившемся попапе кликнуть Подтвердить
  await primaryButton.waitForEnabled()

  assert.strictEqual(
    await orders.statusBadge.getText(),
    'В доставке',
    'Статус заказа меняется с "В работе" на "В доставке".'
  )
  assert.strictEqual(
    await orders.primaryButton.$(OrdersSelectors.buttonPrimaryActionText).getText(),
    'Доставлено',
    'Появляется кнопка "Доставлено"'
  )

  /** Шаг 5: Кликнуть по кнопке "Доставлено" */
  await primaryButton.click()
  await orders.confirmDialog() // в появившемся попапе кликнуть Подтвердить
  await primaryButton.waitForDisplayed({reverse: true})

  assert.strictEqual(await orders.statusBadge.getText(), 'Завершен', 'В вендорке заказ меняет статус на "Завершен".')
  assert(
    (await this.browser.$(`${OrdersSelectors.orderUtilityActions} [alt="callback"]`).isDisplayed()) === false,
    'В карточке заказа исчезает кнопка с иконкой телефона.'
  )
  order = orders.completedOrders.$(OrdersSelectors.listItem)
  assert.strictEqual(
    await order.$(OrdersSelectors.listItemId).getText(),
    '№ 210831-194993',
    'Заказ перемещается в раздел списка заказов "Выполнены".'
  )
})
