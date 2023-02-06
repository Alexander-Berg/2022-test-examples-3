const url = require('url');

function getOrParseUrl(someUrl) {
    if (someUrl instanceof url.Url) return someUrl;

    if (typeof someUrl === 'string') return url.parse(someUrl, true, true);

    if (someUrl && someUrl.queryValidator && typeof someUrl.url === 'string') return url.parse(someUrl.url, true, true);

    return someUrl;
}

/**
 * Команда для проверки URL
 *
 * @param {Object|String} actualUrl - Cравниваемый URL
 * @param {Object|String} expectedUrl - Ожидаемый URL
 * @param {Function} [expectedUrl.queryValidator] - Кастомный валидатор параметров
 * @param {String} [expectedUrl.url] - Ожидаемый URL
 * @param {String} [message=Ошибочный адрес] - Кастомное базовое сообщение об ошибке,
 *   которое будет дополнено информацией о конкретной ошибке
 * @param {Object} [params]
 * @param {Boolean} [params.skipProtocol=false] - Отключить проверку протокола
 * @param {Boolean} [params.skipHostname=false] - Отключить проверку hostname
 * @param {Boolean} [params.skipPathname=false] - Отключить проверку pathname
 * @param {Boolean} [params.skipQuery=false] - Отключить проверку параметров запроса
 * @param {Boolean} [params.skipHash=false] - Отключить проверку hash
 */

module.exports = function(actualUrl, expectedUrl, message, params) {
    const actual = getOrParseUrl(actualUrl);
    const expected = getOrParseUrl(expectedUrl);

    if (typeof message !== 'string') {
        params = message;
        message = 'Ошибочный адрес';
    }

    const errorMessage = part => `${message}, неправильный '${part}'`;

    params = params || {};

    if (!params.skipProtocol) {
        assert.equal(actual.protocol, expected.protocol, errorMessage('protocol'));
    }

    if (!params.skipHostname) {
        assert.equal(actual.hostname, expected.hostname, errorMessage('hostname'));
    }

    if (!params.skipPathname) {
        assert.equal(actual.pathname, expected.pathname, errorMessage('pathname'));
    }

    if (!params.skipQuery) {
        expectedUrl.queryValidator instanceof Function ?
            assert(expectedUrl.queryValidator(actual.query, actual), errorMessage('query')) :
            assert.deepEqual(actual.query, expected.query, errorMessage('query'));
    }

    if (!params.skipHash) {
        assert.equal(actual.hash, expected.hash, errorMessage('hash'));
    }
};
