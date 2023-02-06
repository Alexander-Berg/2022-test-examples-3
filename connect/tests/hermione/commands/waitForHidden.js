/**
 * Удобная обёртка над waitForVisible(selector, delay, true)
 *
 * @param {String} selector
 * @param {Number} delay
 * @returns {Promise}
 */
module.exports = function(selector, delay) {
    return this.waitForVisible(selector, delay, true);
};
