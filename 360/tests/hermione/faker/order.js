const _times = require('lodash/times');

/**
 * @typedef {import('../commands/ya-use-faker').Faker} Faker
 */

/**
 * @typedef {Object} ItemData
 * @property {string} name
 * @property {number} price
 * @property {string} currency
 * @property {number} amount
 * @property {string} nds
 */

/**
 * @typedef {Object} OrderData
 * @property {string} caption
 * @property {ItemData[]} items
 * @property {string | void} test
 */

/**
 * @function
 * @param {Faker} faker
 * @returns {() => ItemData}
 */
const makeItemDataCreator = faker => () => {
  return {
    name: faker.commerce.productName(),
    price: faker.random.float({ min: 10, max: 1000, precision: 0.1 }),
    currency: 'RUB',
    amount: faker.random.float({ min: 1, max: 10, precision: 0.5 }),
    nds: 'nds_0'
  };
};

/**
 * @function
 * @param {Object} options
 * @property {number} options.itemsCount
 * @property {boolean} [options.withTestOkClear]
 * @returns {(faker: Faker) => OrderData}
 */
const makeOrderDataCreator = ({
  itemsCount,
  withTestOkClear = false
}) => faker => {
  const createItemData = makeItemDataCreator(faker);

  return {
    caption: `Заказ ${faker.random.uuid()}`,
    items: _times(itemsCount, createItemData),
    /**
     * Prepare an order for immediate test payment.
     * @see https://a.yandex-team.ru/arc/trunk/arcadia/mail/payments/payments/core/entities/enums.py?rev=7437534#L470
     */
    test: withTestOkClear ? 'test_ok_clear' : undefined
  };
};

module.exports = {
  makeOrderDataCreator
};
