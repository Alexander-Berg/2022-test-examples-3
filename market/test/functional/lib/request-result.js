'use strict';

const Cookie = require('./cookie');
const ApiRequest = require('./api-request');

/**
 * Класс, представляющий результаты выполнения запроса.
 */
class RequestResult {
    /**
     * @constructor
     * @param {String|Object} response Ответ клиенту.
     * @param {Array.<Cookie>} cookies Куки, установленные в запросе.
     * @param {Object.<String, JSON>} logs Логи, записанные в запросе.
     * @param {Array.<ApiRequest>} apiRequests Запросы, отправленные к API Маркета.
     */
    constructor(response, cookies, logs, apiRequests) {
        this._response = response;
        this._cookies = cookies;
        this._logs = logs;
        this._apiRequests = apiRequests;
    }

    /**
     * Возвращает ответ, предназначенный клиенту.
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
     * Возвращает куки, установленные пользователю во время запроса.
     * @returns {Array.<Cookie>}
     */
    get cookies() {
        return this._cookies;
    }
    
    getCookie(key) {
        return this._cookies.filter((cookie) => cookie.key === key)[0];
    }
    
    getCookieValue(key) {
        let cookie = this.getCookie(key);
        if (cookie) {
            return cookie.value;
        }
    }

    /**
     * Возвращает логи, сформированные во время запроса.
     * @returns {Object.<String, JSON>}
     */
    get logs() {
        return this._logs;
    }

    /**
     * Возвращает запросы, отправленные сервером к API Маркета.
     * @returns {Array.<ApiRequest>}
     */
    get apiRequests() {
        return this._apiRequests;
    }
}

/**
 *
 * @type {RequestResult}
 */
module.exports = RequestResult;
