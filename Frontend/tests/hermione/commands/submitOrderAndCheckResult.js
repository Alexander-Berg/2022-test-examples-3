const assert = require('assert');

const checkoutPage = require('../page-objects/checkout');

/**
 * Нажимаем на кнопку оформленя заказа
 * И проверяем, что в getCheckoutResult отсутствует информация о ошибке
 *
 * @returns {Promise}
 */
module.exports = async function submitOrderAndCheckResult() {
    await this.click(checkoutPage.orderSummaryButton);
    await this.handleCheckoutEvent('paymentStart', {});

    // Ожидаем закрытия чекаута
    await this.waitForVisible(checkoutPage.root, 5000, true);

    const result = await this.getCheckoutResult();
    assert.ok(!result.error, 'Чекаут завершился ошибкой');
};
