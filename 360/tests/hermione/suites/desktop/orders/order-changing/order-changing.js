describe('Order changing', () => {
  beforeEach(function() {
    return this.browser.yaAuthAny();
  });

  afterEach(function() {
    return this.browser.unlockAccount();
  });

  /**
   * @see https://testpalm2.yandex-team.ru/testcase/pay-12
   */
  it('Деактивировать платеж', function() {
    const PO = this.PO;

    return (
      this.browser
        // - do: создать новый платеж на 1 товар
        .yaCreateOrder({ itemsCount: 1 })
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
              // - do: нажать на "Деактивировать"
              .click(orderPO.menu())
              .click(orderPO.menu.deactivateOrderButton())
              // - assert: исчезло меню
              .yaWaitForHidden(orderPO.menu())
              // - screenshot: вид деактивированного платежа [order_was_deactivated]
              .assertView('order_was_deactivated', orderPO.mainBlock(), {
                ignoreElements: [orderPO.subtitle(), orderPO.date()]
              })
          );
        })
    );
  });
});
