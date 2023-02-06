const { makeRefundDataCreator } = require('../faker/refund');

/**
 * @typedef {import('../faker/refund').RefundData} RefundData
 */

/**
 * @function
 * @param {any} browser
 * @param {number} orderId
 * @param {RefundData} refundData
 * @returns {Promise<any>}
 */
const createRefund = (browser, orderId, refundData) => {
  return browser.yaApiRequest(`api/orders/${orderId}/refund`, {
    method: 'post',
    json: refundData
  });
};

/**
 * @function
 * @param {Object} order
 * @param {Object} options
 * @param {number[]} options.itemsAmountCoefficients
 * @returns {Promise<any>}
 */
module.exports = async function yaCreateRefund(
  order,
  { itemsAmountCoefficients }
) {
  const orderId = order.id;
  const refundData = await this.yaUseFaker(
    makeRefundDataCreator({
      itemsAmountCoefficients,
      caption: order.caption,
      items: order.items
    })
  );

  return createRefund(this, orderId, refundData);
};
