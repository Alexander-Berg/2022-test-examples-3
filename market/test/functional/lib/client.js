'use strict';

const qs = require('qs');
const nmock = require('nmock');
const {EventEmitter} = require('events');

const Cookie = require('./cookie');
const RequestResult = require('./request-result');

const EXTERNAL_API_MOCKS = require('./../data/external-api-mocks');
const ROUTES = require('./routes');
const SETTINGS = require('./../data/settings');
const CLIENT_EVENTS = require('./../data/client-events');
const PRODUCT_REQUESTS = require('./../data/products-requests');
const SETTINGS_REQUESTS = require('./../data/settings-requests');
const SOVETNIK_DISABLED = require('./../data/sovetnik-disabled-request');

/**
 * Класс, представляющий клиента.
 */
class Client extends EventEmitter {
    /**
     * @typedef {Object} ApiMock
     * @param {String|RegExp} ApiMock.hostname
     * @param {String|RegExp} [ApiMock.path] Ресурс к мокаемому API, запрос к которому переопределяется.
     * @param {Object<String, String|RegExp>} [ApiMock.query] Правила по которым сравниваются параметры, переданные в запросе.
     *      Если параметры, присутствующие в запросе совпадают с apiMock.query - запрос подменяется. Если apiMock.query
     *      отсутствует, то подменяются все запросы к apiMock.path.
     * @param {{status: Number, body: Object}|String} ApiMock.result Ответ на подмененный запрос. Может быть либо JSON-объектом, либо названием файла.
     */

    /**
     * @constructor
     * @param {Object} [partnerSettings] Настройки партнера.
     * @param {Object} [userSettings] Настройки пользователя.
     * @param {Boolean} [hasButton = false] True, если у пользователя есть расширение с кнопкой.
     * @param {ApiMock} [apiMock] Правила, по которым сервер не должен совершать запросы к API Маркета, а возвращать
     *      предопределенные значения.
     */
    constructor(partnerSettings, userSettings, hasButton = false, ...apiMock) {
        super();

        this._partnerSettings = partnerSettings;
        this._userSettings = userSettings;
        this._hasButton = hasButton;
        this._apiMock = apiMock;
    }

    /**
     * Преобразует строку query-параметров в JSON-формат.
     * @param {String} query
     * @return {Object|JSON}
     * @private
     */
    static _queryToObject(query) {
        let q = query || {};
        if (typeof query === 'string') {
            q = qs.parse(query);
        }

        return Object.keys(q).reduce((curr, prev) => {
            try {
                curr[prev] = JSON.parse(q[prev], (key, value) => {
                    if (!isNaN(parseInt(q[prev], 10)) && isFinite(q[prev]) && q[prev].length > 16) {
                        return q[prev];
                    }
                    return value;
                });
            } catch (e) {
                curr[prev] = q[prev];
            }
            return curr;
        }, {});
    }

    /**
     * @param {Array<*>} array
     * @returns {Array<Function>}
     * @private
     */
    static _flatten(...array) {
        return [].concat(...array.map((a) => Array.isArray(a) ? Client._flatten(...a) : a));
    }

    /**
     * Формирует запрос.
     * @param {Object=} [query = {}]
     * @param {Object=} [body = {}]
     * @param {Object=} [headers = {}]
     * @returns {{cookies: {}, query: {}, body: {}, headers: {}}}
     * @private
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
     *
     * @param {Object} object
     * @param {Object} attrs объект, содержащий в себе необходимые для объекта атрибуты.
     * @return {boolean}
     * @private
     *
     * @example
     *      const object = {
     *          a: {
     *              b: 'lol',
     *              c: 'lolol'
     *          }
     *      };
     *
     *      const attrs = {
     *          a: {
     *              c: /olo/
     *          }
     *      };
     *
     *      const result = Client._isMatch(object, attrs); // true
     */
    static _isMatch(object, attrs = {}) {
        const keys = Object.keys(attrs);
        if (object == null) {
            return !keys.length;
        }
        const obj = Object(object);
        return keys.every((key) => {
            if (key in obj) {
                return typeof obj[key] === 'object' && typeof attrs[key] === 'object'
                    ? Client._isMatch(obj[key], attrs[key])
                    : key in obj && (attrs[key] == obj[key] ||
                attrs[key] instanceof RegExp && attrs[key].test(obj[key]));
            }
        });
    }

    /**
     * @param {Array<ApiMock>} mock
     * @private
     */
    _specifyingMock(mock) {
        mock && Array.isArray(mock) && mock.forEach((mock) => {
            if (!mock.hostname || !mock.result) {
                return;
            }

            const mockOptions = {
                allowUnmocked: true
            };

            let scope = nmock(mock.hostname, mockOptions)
                .persist()
                .get(mock.path || '/')
                .query((actualQuery) => {
                    return mock.query
                        ? Client._isMatch(actualQuery, Client._queryToObject(mock.query))
                        : true;
                });

            scope = scope
                .reply(
                    mock.result.status || 200,
                    mock.result.body
                );

            scope.on('replied', (req, interceptor, options) => {
                this.emit('mocked-request', Object.assign({
                    mockInfo: {
                        isMocked: true,
                        mock: mock.result.comment
                    },
                    response: {
                        status: mock.result.status || 200,
                        body: mock.result.body
                    }
                }, options));
            });

            return scope;
        });
    }

    /**
     * @static
     * @private
     */
    static _cleanMock() {
        nmock.cleanAll();
    }

    /**
     * @typedef {Object} RequestParams
     * @property {Object} [headers]
     * @property {JSON|String} [query] query в формате JSON или String
     * @property {Object} [body] body параметры
     */

    /**
     * Возвращает ответ на запрос.
     * @param {Array<Function>} middleware
     * @param {RequestParams=} [requestParams={}] данные о запросе
     * @param {ApiMock} [apiMock] Правила, по которым сервер не должен совершать запросы к API Маркета, а возвращать
     *      предопределенные значения.
     */
    async request(middleware, requestParams={}, ...apiMock) {
        if (!middleware || !Array.isArray(middleware)) {
            throw new TypeError('\'middleware\' should be array of function.');
        }

        const {query, body, headers} = requestParams;

        const queryObject = query ? Client._queryToObject(query) : {};
        const req = this._createRequest(queryObject, body, headers);

        this._specifyingMock([...this._apiMock, ...apiMock]);

        return await new Promise((resolve, reject) => {
            let res = {
                _apiRequests: [],
                _cookies: [],
                clearCookie(key, options) {
                    this._cookies = this._cookies.filter((cookie) => {
                        return !(cookie.key === key ||
                        options && options.path && options.path !== cookie.path ||
                        options && options.domain && options.domain !== cookie.domain);
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
                    res.body = body;
                    const requestResult = new RequestResult(res, this._cookies, req.logs, this._apiRequests);

                    Client._cleanMock();

                    resolve(requestResult);
                },
                json(body) {
                    this.jsonp(body);
                }
            };

            Client._runMiddleware(Client._flatten(middleware), req, res);
        });
    }

    /**
     * Обновляем настройки клиента на другие
     * Если хотим удалить какие-то настройки, нужно передать пустой объект
     *
     * @param {Object} [partnerSettings]
     * @param {Object} [userSettings]
     */
    applySettings(partnerSettings, userSettings) {
        if (partnerSettings) {
            this._partnerSettings = partnerSettings;
        }
        if (userSettings) {
            this._userSettings = userSettings;
        }
    }

    static get API_MARKET_MOCKS() {
        return EXTERNAL_API_MOCKS.MARKET;
    }

    static get ROUTES() {
        return ROUTES;
    }

    static get SETTINGS() {
        return SETTINGS;
    }

    static get CLIENT_EVENTS() {
        return CLIENT_EVENTS;
    }

    static get PRODUCT_REQUESTS() {
        return PRODUCT_REQUESTS;
    }

    static get SETTINGS_REQUESTS() {
        return SETTINGS_REQUESTS;
    }

    static get SOVETNIK_DISABLED() {
        return SOVETNIK_DISABLED;
    }
}

/**
 *
 * @type {Client}
 */
module.exports = Client;
