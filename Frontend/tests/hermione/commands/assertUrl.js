const urlHelper = require('url');

/**
 * Сравнивает фактический url с заданным значением
 * @param {String} url - ожидаемый url
 * @returns {Object}
 */
module.exports = function assertUrl(url) {
    return this
        .assertUrlPath(urlHelper.parse(url).pathname)
        .assertUrlQuery(url);
};
