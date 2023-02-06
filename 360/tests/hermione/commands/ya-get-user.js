const meta = require('../constants/meta');

/**
 * @typedef {Object} User
 * @property {number} uid
 * @property {string} login
 */

/**
 * @returns {Promise<User>}
 */
module.exports = function yaGetUser() {
  return Promise.resolve(this.getMeta(meta.TUS));
};
