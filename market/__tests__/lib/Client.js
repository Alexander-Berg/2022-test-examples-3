'use strict';

const { EventEmitter } = require('events');

const Cookie = require('./Cookie');
const ApiMock = require('./ApiMock');
const RequestResult = require('./RequestResult');

/**
 * @description Клиент Советника.
 */
class Client extends EventEmitter {
    /**
     * @constructor
     *
     * @param {Object} [partnerSettings] Настройки партнера.
     * @param {Object} [userSettings] Настройки пользователя.
     * @param {Boolean} [hasButton = false] Идентификатор присутствия кнопки у пользователя.
     * @param {...ApiMock} [apiMocks] Правила, по которым сервер не должен совершать
     *  запросы к API, а должен возвращать предопределенные значения.
     */
    constructor(partnerSettings, userSettings, hasButton = false, ...apiMocks) {
        super();

        this._partnerSettings = partnerSettings;
        this._userSettings = userSettings;
        this._hasButton = hasButton;
        this._apiMocks = apiMocks;
    }

    /**
     * @private
     * @static
     *
     * @param {Array<*>} array
     *
     * @returns {Array<Function>}
     */
    static _flatten(...array) {
        return [].concat(...array.map((a) => Array.isArray(a) ? Client._flatten(...a) : a));
    }

    /**
     * @description Формирует данные запроса.
     * @private
     *
     * @param {Object=} [query = {}] TODO
     * @param {Object=} [body = {}] TODO
     * @param {Object=} [headers = {}] TODO
     *
     * @returns {{cookies: {}, query: {}, body: {}, headers: {}}}
     */
    _createRequest(query = {}, body = {}, headers = {}) {
        let cookies = {};
        if (this._partnerSettings) {
            cookies['svt-partner'] = Cookie.encrypt(JSON.stringify(this._partnerSettings));
        }
        if (this._userSettings) {
            cookies['svt-user'] = Cookie.encrypt(JSON.stringify(this._userSettings));
        }
        if (this._hasButton) {
            cookies['svt-button'] = true;
        }

        return {
            headers: headers,
            cookies,
            query: query,
            body: body
        };
    }

    static _runMiddleware(middleware, req, res) {

        const callNextMiddleware = (err) => {
            if (err) {
                return;
            }

            let currentStep = middleware.shift();
            if (currentStep) {
                try {
                    currentStep(req, res, () => callNextMiddleware());
                } catch (exp) {
                    console.error(exp);
                }
            }
        };

        callNextMiddleware();
    }


    /**
     * @private
     * @static
     *
     * @param {ApiMock[]} mock
     *
     * @returns {undefined}
     */
    _specifyingMock(mock) {
        if (Array.isArray(mock)) {
            mock.forEach((item) => item.activate(this));
        }
    }


    /**
     * @description Выполняет запрос в соответсвующие middlewares.
     *
     * @param {Function[]} middlewares TODO
     *
     * @param {Object} [requestParams = {}] Данные о запросе.
     * @param {Object} [requestParams.headers] TODO
     * @param {string|JSON} [requestParams.query] TODO
     * @param {Object} [requestParams.body] TODO
     *
     * @param {...ApiMock} [apiMocks] Замоканные данные.
     *
     * @returns {Promise<void>}
     */
    async request(middlewares, requestParams = {}, ...apiMocks) {
        if (!Array.isArray(middlewares)) {
            throw new TypeError('\'middlewares\' should be array of functions.');
        }

        const { query, body, headers } = requestParams;

        const queryObject = query
            ? ApiMock.queryToObject(query)
            : {};

        const _apiMocks = [...this._apiMocks, ...apiMocks];
        this._specifyingMock(_apiMocks);

        const req = this._createRequest(queryObject, body, headers);

        return await new Promise((resolve) => {
            const res = {
                _cookies: [],
                clearCookie(key, options) {
                    this._cookies = this._cookies.filter((cookie) => {
                        return !(
                            cookie.key === key ||
                            options && options.path && options.path !== cookie.path ||
                            options && options.domain && options.domain !== cookie.domain
                        );
                    });
                },
                status(status) {
                    this._status = status;
                },
                header(field, value) {
                    this._header = this._header || {};
                    this._header[field] = value;
                },
                on(...args) {
                    //TODO: on handle
                },
                cookie(key, value, options) {
                    this._cookies.push(new Cookie(key, value, options));
                },
                jsonp(body) {
                    _apiMocks.forEach((mock) => mock.deactivate());

                    res.body = body;
                    const requestResult = new RequestResult(res, this._cookies, req.logs);
                    resolve(requestResult);
                },
                json(body) {
                    this.jsonp(body);
                }
            };

            Client._runMiddleware(Client._flatten(middlewares), req, res);
        });
    }

    /**
     * @description Обновляет настройки клиента.
     *
     * @param {Object} [partnerSettings] TODO
     * @param {Object} [userSettings] TODO
     *
     * @returns {undefined}
     */
    applySettings(partnerSettings, userSettings) {
        if (partnerSettings) {
            this._partnerSettings = partnerSettings;
        }
        if (userSettings) {
            this._userSettings = userSettings;
        }
    }
}

module.exports = Client;
