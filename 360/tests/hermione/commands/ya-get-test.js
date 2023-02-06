const meta = require('../constants/meta');

/**
 * @typedef {import('../helpers/test').TestMeta} TestMeta
 */

/**
 * @returns {Promise<TestMeta>}
 */
module.exports = function yaGetTest() {
  return Promise.resolve(this.getMeta(meta.TEST));
};
