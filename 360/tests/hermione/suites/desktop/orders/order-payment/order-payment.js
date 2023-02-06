describe('Order payment', () => {
  beforeEach(function() {
    return this.browser.yaAuthAny();
  });

  afterEach(function() {
    return this.browser.unlockAccount();
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-10
   */
  it('Оплатить счет на один товар', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар
        .yaCreateOrder({ itemsCount: 1 })
        // - do: открыть страницу '/transaction/{order_hash}'
        .then(order => this.browser.url(`/transaction/${order.order_hash}`))
        // - screenshot: вид формы оплаты [payment_was_opened]
        .assertView('payment_was_opened', PO.payment(), {
          ignoreElements: [PO.payment.emailInput()]
        })
        // - do: нажать на "Оплатить"
        .click(PO.payment.startPaymentButton())
        // - assert: открылся сервис Yandex.Trust
        .yaWaitForTrust()
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-11
   */
  it('Оплатить счет на несколько товаров', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров
        .yaCreateOrder({ itemsCount: 2 })
        // - do: открыть страницу '/transaction/{order_hash}'
        .then(order => this.browser.url(`/transaction/${order.order_hash}`))
        // - screenshot: вид формы оплаты [payment_was_opened]
        .assertView('payment_was_opened', PO.payment(), {
          ignoreElements: [PO.payment.emailInput()]
        })
        // - do: нажать на "Оплатить"
        .click(PO.payment.startPaymentButton())
        // - assert: открылся сервис Yandex.Trust
        .yaWaitForTrust()
    );
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-13
   */
  it('Активировать повторно неактивный счет и оплатить его', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар и деактивировать его
        .yaCreateOrder({ itemsCount: 1 })
        .then(order => this.browser.yaDeactivateOrder(order))
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
              // - do: нажать на карточку платежа
              .click(orderPO())
              // - assert: открылась полная карточка платежа
              .waitForVisible(orderPO.mix(PO.orders.openedOrder)())
              // - do: нажать на "Активировать"
              .click(orderPO.activateOrderButton())
              // - assert: исчезла кнопка "Активировать"
              .yaWaitForHidden(orderPO.activateOrderButton())
          );
        })
        // - do: открыть страницу '/transaction/{order_hash}'
        .then(() => this.browser.getMeta('currentOrder'))
        .then(order => this.browser.url(`/transaction/${order.order_hash}`))
        // - screenshot: вид формы оплаты [payment_was_opened]
        .assertView('payment_was_opened', PO.payment(), {
          ignoreElements: [PO.payment.emailInput()]
        })
        // - do: нажать на "Оплатить"
        .click(PO.payment.startPaymentButton())
        // - assert: открылся сервис Yandex.Trust
        .yaWaitForTrust()
    );
  });

  it('Оплата деактивированного счета', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на несколько товаров
        .yaCreateOrder({ itemsCount: 1 })
        .then(order => this.browser.yaDeactivateOrder(order))
        // - do: открыть страницу '/transaction/{order_hash}'
        .then(order => this.browser.url(`/transaction/${order.order_hash}`))
        // - screenshot: вид формы оплаты [payment_was_opened]
        .assertView('payment_was_opened', PO.payment(), {
          ignoreElements: [PO.payment.emailInput()]
        })
        // - do: нажать на "Оплатить"
        .click(PO.payment.startPaymentButton())
        // - assert: открылась страницы с ошибкой оплаты
        .waitForVisible(PO.paymentError())
        // - screenshot: вид ошибки оплаты [payment_error]
        .assertView('payment_error', PO.paymentError())
    );
  });
});
