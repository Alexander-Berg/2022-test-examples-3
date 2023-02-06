const utils = require('../../utils');

/**
 * Возвращает счетчик по селектору
 *
 * @param {String} selector - selector
 *
 * @returns {Promise}
 */
module.exports = function(selector) {
    return this
        .getAttribute(selector, 'data-counter')
        .then(function(counter) {
            if (!counter) {
                return;
            }

            const parsedCounterRaw = JSON.parse(utils.firstOrDefault(counter));

            if (parsedCounterRaw.length === 0) {
                return;
            }

            return {
                path: parsedCounterRaw[1],
                vars: parsedCounterRaw[2]
            };
        });
};
