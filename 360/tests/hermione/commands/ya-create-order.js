const { makeOrderDataCreator } = require('../faker/order');
const { wait } = require('../helpers/time');

/**
 * @typedef {import('../faker/order').OrderData} OrderData
 */

/**
 * @function
 * @param {any} browser
 * @param {OrderData} orderData
 * @returns {Promise<any>}
 */
const createOrder = (browser, orderData) => {
  return browser.yaApiRequest('api/internal/order', {
    method: 'post',
    json: orderData
  });
};

/**
 * @function
 * @param {any} browser
 * @param {number} orderId
 * @returns {Promise<void>}
 */
const startOrderPayment = async (browser, orderId) => {
  await browser.yaApiRequest(`api/internal/order/${orderId}/start`, {
    method: 'post',
    json: {
      /**
       * This is required parameters.
       */
      return_url: '',
      email: ''
    }
  });
};

/**
 * @function
 * @param {any} browser
 * @param {number} orderId
 * @returns {Promise<any>}
 */
const getOrder = (browser, orderId) => {
  return browser.yaApiRequest(`api/orders/${orderId}`);
};

/**
 * @function
 * @param {Object} options
 * @param {number} options.itemsCount
 * @param {boolean} [options.withPaidStatus]
 * @returns {Promise<any>}
 */
module.exports = async function yaCreateOrder({
  itemsCount,
  withPaidStatus = false
}) {
  const orderData = await this.yaUseFaker(
    makeOrderDataCreator({
      itemsCount,
      withTestOkClear: withPaidStatus
    })
  );
  const order = await createOrder(this, orderData);
  const orderId = order.id;

  if (withPaidStatus) {
    await startOrderPayment(this, orderId);
    /**
     * Waiting for the payment status auto set.
     */
    await wait(5000);
  }

  return getOrder(this, orderId);
};
