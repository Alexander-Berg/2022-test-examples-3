'use strict';

const Cookie = require('./Cookie');

/**
 * @description Результаты выполнения запроса.
 */
class RequestResult {
    /**
     * @constructor
     *
     * @param {String|Object} response Ответ.
     * @param {Cookie[]} cookies Куки.
     * @param {Object<String, JSON>} logs Логи.
     */
    constructor(response, cookies, logs) {
        this._response = response;
        this._cookies = cookies;
        this._logs = logs;
    }

    /**
     * @description Возвращает ответ, предназначенный клиенту.
     *
     * @returns {JSON}
     */
    get response() {
        let response = this._response.body;
        try {
            response = JSON.parse(this._response.body);
        } catch (e) {
            //
        }

        return response;
    }

    /**
     * @description Возвращает куки, установленные пользователю во время запроса.
     *
     * @returns {Cookie[]}
     */
    get cookies() {
        return this._cookies;
    }

    getCookie(key) {
        return this._cookies.filter((cookie) => cookie.key === key)[0];
    }

    getCookieValue(key) {
        const cookie = this.getCookie(key);
        if (cookie) {
            return cookie.value;
        }
    }

    /**
     * @description Возвращает логи, сформированные во время запроса.
     *
     * @returns {Object<String, JSON>}
     */
    get logs() {
        return this._logs;
    }
}

module.exports = RequestResult;
