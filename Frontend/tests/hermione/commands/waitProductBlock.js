const checkoutPage = require('../page-objects/checkout');

/**
 * Ожидает появления блока с товарами внутри фрейма чекаута
 *
 * @returns {Promise}
 */
module.exports = async function waitProductBlock() {
    await this.waitForVisible(checkoutPage.primaryOrderProducts);
};
