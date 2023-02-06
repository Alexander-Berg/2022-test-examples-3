const checkoutPage = require('../page-objects/checkout');

/**
 * Ожидает загрузки адреса пользователя
 *
 * @returns {Promise}
 */
module.exports = async function waitDeliveryProfileAddressesLoad() {
    await this.waitForVisible(checkoutPage.deliveryProfileAddressesFirstOption, 10000);

    // Делаем паузу, чтобы успел скрыться лоадер,
    // так как он показывается через 250мс в течение минимум 750мс
    await this.pause(1000);
    await this.handleCheckoutEvent('cityChange', {});
    await this.handleCheckoutEvent('shippingAddressChange', {});
    await this.waitForVisible(checkoutPage.pageLoader, 5000, true);
};
