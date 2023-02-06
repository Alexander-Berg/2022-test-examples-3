/**
 * @typedef {Object} ItemData
 * @property {string} name
 * @property {number} price
 * @property {string} currency
 * @property {number} amount
 * @property {string} nds
 */

/**
 * @typedef {Object} RefundData
 * @property {string} caption
 * @property {ItemData[]} items
 */

/**
 * @function
 * @param {number[]} itemsAmountCoefficients
 * @returns {(item: Object, index: number) => ItemData}
 */
const makeItemDataCreator = itemsAmountCoefficients => (item, index) => {
  const amountCoefficient = itemsAmountCoefficients[index];

  return {
    name: item.name,
    price: item.price,
    currency: item.currency,
    amount: item.amount * amountCoefficient,
    nds: item.nds
  };
};

/**
 * @function
 * @param {Object} item
 * @returns {boolean}
 */
const rejectItemWithZeroAmount = item => {
  return item.amount !== 0;
};

/**
 * @function
 * @param {Object} options
 * @param {string} options.caption
 * @param {Object[]} options.items
 * @param {number[]} options.itemsAmountCoefficients
 * @returns {() => RefundData}
 */
const makeRefundDataCreator = ({
  caption,
  items,
  itemsAmountCoefficients
}) => () => {
  const createItemData = makeItemDataCreator(itemsAmountCoefficients);

  return {
    caption,
    items: items.map(createItemData).filter(rejectItemWithZeroAmount)
  };
};

module.exports = {
  makeRefundDataCreator
};
