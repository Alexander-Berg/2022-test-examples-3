const RANDOM_LOGIN_PREFIX = 'e2e-rand';

/**
 * @param {string} [login]
 * @param {Object} [options]
 * @returns {Promise<any>}
 */
module.exports = function yaAuthRandom(
  login = RANDOM_LOGIN_PREFIX,
  options = {}
) {
  const randomInt = Math.floor(Math.random() * Date.now());
  const randomHash = randomInt.toString(36).slice(-6);

  return this.auth(`${login}-${randomHash}`, options);
};
