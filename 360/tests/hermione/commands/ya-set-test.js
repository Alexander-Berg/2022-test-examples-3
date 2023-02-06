const meta = require('../constants/meta');
const { getTestMeta } = require('../helpers/test');

/**
 * @param {any} test
 * @returns {Promise<any>}
 */
module.exports = function yaSetTest(test) {
  const testMeta = getTestMeta(test);

  return Promise.resolve(this.setMeta(meta.TEST, testMeta));
};
