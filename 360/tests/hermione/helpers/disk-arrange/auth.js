const cookiesHelper = require('./cookies-helper');
const url = require('url');
const ask = require('asker-as-promised');

/**
 * Выполняет авторизацию в пасспорте и возвращает полученные куки.
 *
 * @param {Object} credentials
 * @param {string} credentials.login
 * @param {string} credentials.password
 * @param {string} passportUrl
 * @returns {Promise<Object>}
 */
module.exports = async function auth({ login, password }, passportUrl) {
    const options = {
        ...url.parse(passportUrl),
        method: 'POST',
        query: {
            mode: 'auth'
        },
        body: {
            login: login,
            passwd: password,
            timestamp: Math.round(Date.now() / 1000)
        },
        bodyEncoding: 'urlencoded',
        timeout: 2000
    };

    const response = await ask(options);
    const cookies = cookiesHelper.readCookies(response.headers);

    const error = _isAuthError(cookies, response);
    if (error) {
        throw new Error(error);
    }

    return cookies;
};

/**
 * Проверяет, успешно ли прошла авторизация, если нет то возвращает текстовое описание ошибки.
 *
 * @param {Object} cookies - Объект кук
 * @param {Object} response - Объект с ответом от пасспорта
 * @returns {string|boolean}
 * @private
 */
function _isAuthError(cookies, response) {
    const hasAuthCookies = cookies.Session_id && cookies.sessionid2;

    if (!hasAuthCookies) {
        let errorMsg = 'Неизвестная ошибка';
        const match = response.data.toString().match(/<div.*?passport-Domik-Form-Error.*?><!--.*?-->(.*?)<!--/);

        if (match && match[1]) {
            errorMsg = match[1];
        }

        return `Ошибка при аутентификации (${errorMsg})`;
    }

    return false;
}
