'use strict';

const qs = require('qs');

/**
 * Класс, содержащий данные о запросе к API Маркета и его результате.
 */
class ApiRequest {
    /**
     * @constructor
     * @param {String} path Путь запроса.
     * @param {Object|String} query Query-параметры запроса.
     * @param {Object} response Ответ от API.
     * @param {Number} statusCode Код ответа.
     * @param {Boolean=} [isMocked = false] True, если ответ был подменен, иначе - false.
     */
    constructor(path, query = {}, response, statusCode, isMocked = false) {
        this._path = path;
        this._query = typeof query === 'string' ? qs.parse(query) : query;
        this._response = response;
        this._statusCode = statusCode;
        this._isMocked = isMocked;
    }

    /**
     * Возвращает true, если value удовлетворяет условиям.
     * @param {String} value
     * @param {String|RegExp} matcher
     * @return {Boolean} Возвращает true, если matcher равен value или matcher.test(value) равен true, иначе - false.
     * @private
     */
    static _match(value, matcher) {
        let result = false;
        if (typeof matcher === 'string') {
            result = value === matcher;
        } else if (matcher instanceof RegExp) {
            result = matcher.test(value);
        }
        return result;
    }

    /**
     * Возвращает путь запроса.
     * @returns {String}
     */
    get path() {
        return this._path;
    }

    /**
     * Возвращает query-параметры запроса.
     * @returns {Object|*}
     */
    get query() {
        return this._query;
    }

    /**
     * Возвращает ответ от API.
     * @returns {Object}
     */
    get response() {
        return this._response;
    }

    /**
     * Возвращаект код ответа.
     * @returns {Number}
     */
    get statusCode() {
        return this._statusCode;
    }

    /**
     * Возвращает true, если ответ был подменен, иначе - false.
     * @returns {Boolean}
     */
    get isMocked() {
        return this._isMocked;
    }

    /**
     * Возвращает true, если запрос удовлетворяет заданным условиям.
     * @param {(String|RegExp)=} [path]
     * @param {Object.<String, String|RegExp>=} [query]
     * @return {Boolean}
     */
    match(path, query) {
        let p = path;
        let q = query;
        if (!query && (typeof path === 'object' && !(p instanceof RegExp))) {
            p = undefined;
            q = path;
        }

        let pathMatch = p
            ? ApiRequest._match(this._path, p)
            : true;

        let queryMatch = q
            ? Object.keys(q).every((key) => {
                return ApiRequest._match(this._query[key], q[key]);
            })
            : true;

        return pathMatch && queryMatch;
    }
}

/**
 *
 * @type {ApiRequest}
 */
module.exports = ApiRequest;
