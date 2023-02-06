const { makeOrderDataCreator } = require('../../../../faker/order');

describe('Order creation', () => {
  beforeEach(function() {
    return this.browser.yaAuthAny();
  });

  afterEach(function() {
    return this.browser.unlockAccount();
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-15
   */
  it('Создать новый платеж на 1 товар', async function() {
    const PO = this.PO;
    const orderData = await this.browser.yaUseFaker(
      makeOrderDataCreator({ itemsCount: 1 })
    );

    return (
      this.browser
        // - do: открыть страницу '/'
        .url('/')
        .yaDisableAnimations([
          PO.orders.order(),
          PO.orders.order.mainBlock(),
          PO.orders.orderCreation(),
          PO.orders.orderCreation.body()
        ])
        // - do: нажать на "Создать новый платеж"
        .click(PO.orders.createOrderButton())
        // Работа с формой
        .then(() => {
          const orderPO = PO.orders.orderCreation;
          const [firstItem] = orderData.items;

          return (
            this.browser
              // - assert: открылась форма создания платежа
              .waitForVisible(orderPO())
              // - do: корректно заполнить форму, добавить один товар
              .yaClearValue(orderPO.captionInput())
              .setValue(orderPO.captionInput(), orderData.caption)
              .setValue(orderPO.item.nthChild(1).nameInput(), firstItem.name)
              .setValue(
                orderPO.item.nthChild(1).amountInput(),
                firstItem.amount
              )
              .setValue(orderPO.item.nthChild(1).priceInput(), firstItem.price)
              // - do: нажать на "Создать"
              .click(orderPO.startOrderCreationButton())
          );
        })
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - assert: платеж успешно создан
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного платежа [order_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-16
   */
  it('Создать новый платеж на несколько товаров', async function() {
    const PO = this.PO;
    const orderData = await this.browser.yaUseFaker(
      makeOrderDataCreator({ itemsCount: 2 })
    );

    return (
      this.browser
        // - do: открыть страницу '/'
        .url('/')
        .yaDisableAnimations([
          PO.orders.order(),
          PO.orders.order.mainBlock(),
          PO.orders.orderCreation(),
          PO.orders.orderCreation.body()
        ])
        // - do: нажать на "Создать новый платеж"
        .click(PO.orders.createOrderButton())
        // Работа с формой
        .then(() => {
          const orderPO = PO.orders.orderCreation;
          const [firstItem, secondItem] = orderData.items;

          return (
            this.browser
              // - assert: открылась форма создания платежа
              .waitForVisible(orderPO())
              // - do: корректно заполнить форму, добавить один товар
              .yaClearValue(orderPO.captionInput())
              .setValue(orderPO.captionInput(), orderData.caption)
              .setValue(orderPO.item.nthChild(1).nameInput(), firstItem.name)
              .setValue(
                orderPO.item.nthChild(1).amountInput(),
                firstItem.amount
              )
              .setValue(orderPO.item.nthChild(1).priceInput(), firstItem.price)
              // - do: Нажать на "Добавить позицию"
              .click(orderPO.addItemButton())
              // - assert: появилась поля для второго товара
              .waitForVisible(orderPO.item.nthChild(2)())
              // - do: корректно заполнить поля для второго товара
              .setValue(orderPO.item.nthChild(2).nameInput(), secondItem.name)
              .setValue(
                orderPO.item.nthChild(2).amountInput(),
                secondItem.amount
              )
              .setValue(orderPO.item.nthChild(2).priceInput(), secondItem.price)
              // - do: нажать на "Создать"
              .click(orderPO.startOrderCreationButton())
          );
        })
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - assert: платеж успешно создан
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного платежа [order_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-17
   */
  it('При создании платежа удалить/добавить товар', async function() {
    const PO = this.PO;
    const orderData = await this.browser.yaUseFaker(
      makeOrderDataCreator({ itemsCount: 2 })
    );

    return (
      this.browser
        // - do: открыть страницу '/'
        .url('/')
        .yaDisableAnimations([
          PO.orders.order(),
          PO.orders.order.mainBlock(),
          PO.orders.orderCreation(),
          PO.orders.orderCreation.body()
        ])
        // - do: нажать на "Создать новый платеж"
        .click(PO.orders.createOrderButton())
        // Работа с формой
        .then(() => {
          const orderPO = PO.orders.orderCreation;
          const [firstItem, secondItem] = orderData.items;

          return (
            this.browser
              // - assert: открылась форма создания платежа
              .waitForVisible(orderPO())
              // - do: корректно заполнить форму, добавить один товар
              .yaClearValue(orderPO.captionInput())
              .setValue(orderPO.captionInput(), orderData.caption)
              .setValue(orderPO.item.nthChild(1).nameInput(), firstItem.name)
              .setValue(
                orderPO.item.nthChild(1).amountInput(),
                firstItem.amount
              )
              .setValue(orderPO.item.nthChild(1).priceInput(), firstItem.price)
              // - do: Нажать на "Добавить позицию"
              .click(orderPO.addItemButton())
              // - assert: появилась поля для второго товара
              .waitForVisible(orderPO.item.nthChild(2)())
              // - do: корректно заполнить поля для второго товара
              .setValue(orderPO.item.nthChild(2).nameInput(), secondItem.name)
              .setValue(
                orderPO.item.nthChild(2).amountInput(),
                secondItem.amount
              )
              .setValue(orderPO.item.nthChild(2).priceInput(), secondItem.price)
              // - do: нажать на "Удалить" второго товара
              .click(orderPO.item.nthChild(2).deleteItemButton())
              // - assert: исчезли поля для второго товара
              .yaWaitForHidden(orderPO.item.nthChild(2)())
              // - do: нажать на "Создать"
              .click(orderPO.startOrderCreationButton())
          );
        })
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - assert: платеж успешно создан
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного платежа [order_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });
});
