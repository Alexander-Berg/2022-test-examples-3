import assert from 'assert'
import {OrdersPO} from '../../../page-objects/OrdersPO'
import {OrdersSelectors} from '../../../page-objects/selectors.generated/OrdersSelectors'
import {UiSelectors} from '../../../page-objects/selectors.generated/UiSelectors'

it('[webvendor-168] Отмена заказа рестораном', async function () {
  await this.browser.authorize()
  await this.browser.mockSocket()
  await this.browser.mockDate(new Date(2021, 8, 24, 13, 53))

  /** Шаг 1: Сформировать новый ASAP заказ в ресторане c платной доставкой. */
  // Указать количество приборов - 3.
  // Добавить комментарий
  // Оплатить заказ
  // Назначить курьера
  const orders = new OrdersPO(this.browser)
  const orderId = '210924-027293'
  await orders.open()
  await orders.waitPennyConfigLoad()

  const order = orders.getItemById(orderId)
  await order.waitForDisplayed()
  await order.scrollIntoView()

  assert.strictEqual(
    await order.$(OrdersSelectors.listItemStatus).getText(),
    'Новый заказ',
    'В кратком описании заказа указан статус заказа'
  )
  assert.strictEqual(
    await order.$(OrdersSelectors.listItemTitle).getText(),
    '1200.00 ₽',
    'В кратком описании заказа указана стоимость заказа 1200.00 ₽'
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

  // 2. Нажать на новый заказ в общем списке заказов
  await order.click()
  await orders.statusBadge.waitForDisplayed()

  // В карточке заказа отображаются:
  assert.strictEqual(
    await orders.statusBadge.getText(),
    'Новый',
    'В карточке заказа отображается статус заказа - "Новый"'
  )

  const assertOrderContent = async () => {
    assert.strictEqual(
      await orders.deliveryBadge.getText(),
      'Доставка Яндекса',
      'В карточке заказа отображается тип доставки - "Доставка Яндекса"'
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
      '14:07',
      'В карточке заказа отображается время, к которому необходимо приготовить заказ'
    )
    assert.strictEqual(
      await orders.getOrderPropertyValue(OrdersSelectors.orderClientInfo),
      'Yandex.Eda 8 800 600 12 10',
      'В карточке заказа отображается информация о клиенте'
    )
    assert.strictEqual(
      await orders.getOrderPropertyByLabel('Курьер:'),
      'Василиса',
      'В карточке заказа отображается имя курьера'
    )
    assert.strictEqual(
      await orders.getOrderPropertyByLabel('Телефон:'),
      '+7 916 211 95 69',
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
      'comment',
      'В карточке заказа отображается комментарий'
    )
    assert.strictEqual(
      await this.browser.$(OrdersSelectors.orderPositionName).getText(),
      '1 x Соте из морепродуктов',
      'В карточке заказа отображается состав заказа'
    )
    assert.strictEqual(
      await this.browser.$(OrdersSelectors.orderDeliveryCost).getText(),
      '0.00 ₽',
      'В карточке заказа отображается стоимость доставки'
    )
    assert.strictEqual(
      await this.browser.$(OrdersSelectors.orderTotal).getText(),
      '1200.00 ₽',
      'В карточке заказа отображается общая стоимость заказа'
    )
  }
  await assertOrderContent()

  const {primaryButton} = orders
  await primaryButton.waitForEnabled()

  // 3. Нажать на кнопку "Принять заказ"
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
    '13:55',
    'Появляется время принятия в работу'
  )

  // Проверяем, что остальная информация о заказе не изменилась
  await assertOrderContent()

  // 4. Нажать Отменить заказ
  await orders.cancelButton.click()

  assert(
    await this.browser.$(UiSelectors.modal).isDisplayed(),
    'Должно открыться модальное окно с подтверждением отмены'
  )

  await orders.confirmCancelButton.waitForClickable()
  // 5. Подтвердить отмену заказа
  await orders.confirmCancelButton.click()

  await orders.waitStatus('Отменен')

  assert.strictEqual(
    await orders.statusBadge.getText(),
    'Отменен',
    'Заказ отменился, в статусе отображается, что заказ отменен'
  )

  await this.browser.waitUntil(
    async () => {
      await orders.refreshButton.waitForClickable()
      await orders.refreshButton.click()
      await orders.refreshButton.waitForClickable()

      return orders.cancellationReasonWidget.isDisplayed()
    },
    {timeout: 30000}
  )

  assert(await orders.cancellationReasonWidget.isDisplayed(), 'Должен отображаться бабл с причиной отмены заказа')
})
