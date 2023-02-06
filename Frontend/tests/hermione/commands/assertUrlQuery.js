const urlHelper = require('url');
const { assert } = require('chai');

/**
 * Сравнивает фактический url с заданным значением
 * @param {String} url - ожидаемый url
 * @returns {Object}
 */
module.exports = function assertUrlQuery(url) {
    const expectedQuery = urlHelper.parse(url, true).query;

    return this
        .getUrl()
        .then(
            _url => {
                const query = urlHelper.parse(_url, true).query;

                return assert.deepEqual(
                    urlHelper.parse(_url, true).query,
                    expectedQuery,
                    `Expected ${urlHelper.format({ query: expectedQuery })}, got ${urlHelper.format({ query })}.`
                );
            }
        );
};
