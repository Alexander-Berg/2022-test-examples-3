describe('Refund creation', () => {
  beforeEach(function() {
    return this.browser.yaAuthAny();
  });

  afterEach(function() {
    return this.browser.unlockAccount();
  });

  it('Оформит возврат через меню в карточке платежа', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров и оплатить его
        .yaCreateOrder({ itemsCount: 2, withPaidStatus: true })
        .then(order => this.browser.setMeta('currentOrder', order))
        // - do: открыть страницу '/'
        .url('/')
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => {
          const orderPO = PO.orders.order.withIndex(order.id);

          return (
            this.browser
              .waitForVisible(orderPO())
              // - do: нажать на карточку платежа
              .click(orderPO())
              // - assert: открылась полная карточка платежа
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - do: нажать на "Создать возврат"
              .click(orderPO.menu())
              .waitForVisible(orderPO.menu.createRefundButton())
              .click(orderPO.menu.createRefundButton())
          );
        })
        // - assert: открылась страница создания возврата
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => this.browser.yaWaitForPage(`/refund/${order.id}`))
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-67
   */
  it('Оформить возврат на платеж с одним товаром', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар и оплатить его
        .yaCreateOrder({ itemsCount: 1, withPaidStatus: true })
        // - do: открыть страницу '/refund/{order_id}'
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить единственный товар в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-68
   */
  it('Оформить возврат на платеж с несколькими товарами (выбрать все)', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров и оплатить его
        .yaCreateOrder({ itemsCount: 2, withPaidStatus: true })
        // - do: открыть страницу '/refund/{order_id}'
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить все товары в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        .click(PO.refund.item.nthChild(2).checkbox())
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-73
   */
  it('Оформить возврат на платеж с несколькими товарами (выбрать несколько товаров)', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров и оплатить его
        .yaCreateOrder({ itemsCount: 3, withPaidStatus: true })
        // - do: открыть страницу '/refund/{order_id}'
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить некоторые товары в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        .click(PO.refund.item.nthChild(2).checkbox())
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-74
   */
  it('Оформить возврат на платеж с несколькими товарами (изменить кол-во товаров)', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров и оплатить его
        .yaCreateOrder({ itemsCount: 2, withPaidStatus: true })
        .then(order => this.browser.setMeta('currentOrder', order))
        // - do: открыть страницу '/refund/{order_id}'
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить все товары в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        .click(PO.refund.item.nthChild(2).checkbox())
        // - do: изменить количество для выбранных товаров
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => {
          const [firstItem, secondItem] = order.items;

          return this.browser
            .setValue(
              PO.refund.item.nthChild(1).amountInput(),
              firstItem.amount * 0.2
            )
            .setValue(
              PO.refund.item.nthChild(2).amountInput(),
              secondItem.amount * 0.5
            );
        })
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-70
   */
  it('Оформить повторный возврат на платеж с одним товаром', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар и оплатить его
        .yaCreateOrder({ itemsCount: 1, withPaidStatus: true })
        .then(order => this.browser.setMeta('currentOrder', order))
        // - do: создать возврат на 1 товар
        .then(order =>
          this.browser.yaCreateRefund(order, {
            itemsAmountCoefficients: [0.5]
          })
        )
        // - do: открыть страницу '/refund/{order_id}'
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить единственный товар в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        // - do: изменить количество для выбранного товара
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => {
          const [firstItem] = order.items;

          return this.browser.setValue(
            PO.refund.item.nthChild(1).amountInput(),
            firstItem.amount * 0.2
          );
        })
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-71
   */
  it('Оформить повторный возврат на платеж с несколькими товарами', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров и оплатить его
        .yaCreateOrder({ itemsCount: 2, withPaidStatus: true })
        .then(order => this.browser.setMeta('currentOrder', order))
        // - do: создать возврат на несколько товаров
        .then(order =>
          this.browser.yaCreateRefund(order, {
            itemsAmountCoefficients: [0.5, 0.2]
          })
        )
        // - do: открыть страницу '/refund/{order_id}'
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить все товары в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        .click(PO.refund.item.nthChild(2).checkbox())
        // - do: изменить количество для выбранных товаров
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => {
          const [firstItem, secondItem] = order.items;

          return this.browser
            .setValue(
              PO.refund.item.nthChild(1).amountInput(),
              firstItem.amount * 0.2
            )
            .setValue(
              PO.refund.item.nthChild(2).amountInput(),
              secondItem.amount * 0.5
            );
        })
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-100
   */
  it('Оформить частичный возврат', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар и оплатить его
        .yaCreateOrder({ itemsCount: 1, withPaidStatus: true })
        .then(order => this.browser.setMeta('currentOrder', order))
        // - do: открыть страницу '/refund/{order_id}'
        .then(order => this.browser.url(`/refund/${order.id}`))
        // - do: отметить единственный товар в списке галочкой
        .click(PO.refund.item.nthChild(1).checkbox())
        // - do: изменить количество для выбранных товаров
        .then(() => this.browser.getMeta('currentOrder'))
        .setValue(
              PO.refund.item.nthChild(1).amountInput(),
              0.5
            )
        // - do: нажать на "Оформить возврат"
        .click(PO.refund.startRefundCreationButton())
        // - assert: открылась главная страница
        .waitForVisible(PO.orders())
        .yaDisableAnimations([PO.orders.order(), PO.orders.order.mainBlock()])
        // Работа с карточкой
        .then(() => {
          const orderPO = PO.orders.order.nthChild(1);

          return (
            this.browser
              // - do: нажать на карточку возврата
              .click(orderPO())
              // - assert: открылась полная карточка возврата
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - screenshot: вид успешно созданного возврата [refund_was_created]
              .assertView('refund_was_created', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });
})
