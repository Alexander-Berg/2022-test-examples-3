/**
 * @function
 * @param {Object} browser
 * @param {number} orderId
 * @returns {Promise<any>}
 */
const deactivateOrder = (browser, orderId) => {
  return browser.yaApiRequest(`api/orders/${orderId}/deactivate`, {
    method: 'post'
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
 * @param {Object} order
 * @returns {Promise<any>}
 */
module.exports = async function yaDeactivateOrder(order) {
  const orderId = order.id;

  await deactivateOrder(this, orderId);

  return getOrder(this, orderId);
};
