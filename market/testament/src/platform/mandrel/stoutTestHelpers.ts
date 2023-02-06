// @ts-nocheck
/* global createSpyObj */
/* eslint-disable @typescript-eslint/no-empty-function, @typescript-eslint/explicit-module-boundary-types */

import deepmerge from 'deepmerge';
import _ from 'lodash';
import Response from 'Response';
import {AbtData} from '@yandex-market/abt-headers';
import stout from '@yandex-market/stout';
import ResourceModule from '@yandex-market/mandrel/modules/Resource';
// eslint-disable-next-line no-restricted-modules
import logger from '@yandex-market/logger';
import Request from '@yandex-market/stout/lib/node_modules/HTTPServer/Request';
import type StoutUser from '@yandex-market/mandrel/lib/User/StoutUser';

import difference from './difference';

const originalGetWidget = stout.getWidget;
const originalGetPage = stout.getPage;
const originalLogger = stout.logger;
const originalWidget = stout.Page.prototype.widget;
const originalModule = stout.Page.prototype.module;

const originalResource = ResourceModule.prototype.resource;

/**
 * здесь сохраняем сконфигурированные стабы для страниц
 * @type {Object}
 * @private
 */
let _pageStubsRegistry = {};

/**
 * здесь сохраняем сконфигурированные стабы для виджетов
 * @type {Object}
 * @private
 */
let _widgetStubsRegistry = {};

/**
 * Конфигурация стабов ресурса
 * @type {Array}
 * @private
 */
let _resourceStubParams = [];

/**
 * Дефолтный результат работы модуля @yandex-market/abt-headers
 * @type {AbtData}
 */
export const DEFAULT_ABT_DATA = new AbtData();

const defaultRouterMock = {
    buildURL(pageId) {
        return this._routes[pageId] || null;
    },

    _routes: {
        'market:catalog':
            'https://market.yandex.ru/catalog--tovary-so-skidkoi/61522',
    },
};

/**
 *
 * Подсунуть в stout stub в качестве логгера
 * @param {Function} [stubObject]
 */
export function stubLogger(stubObject) {
    stout.setLogger(stubObject || jest.fn());
}

/**
 *
 * Вернуть базовую логгер в stout
 */
export function setOriginalLogger() {
    stout.setLogger(originalLogger);
}

/**
 *
 * Сконфигурировать stout, чтобы он отдавал stub при запросе виджета через `getWidget`
 * @param {String} [widgetName] если не передан, то stub будет отдаваться для всех виджетов
 * @param {Object} [stubObject] если не передан будет отдаваться базовый виджет stout
 */
export function stubGetWidget(widgetName, stubObject) {
    const stubKey = widgetName || '__all';

    _widgetStubsRegistry[stubKey] = stubObject || originalGetWidget();

    stout.getWidget = jest.fn(
        widget => _widgetStubsRegistry.__all || _widgetStubsRegistry[widget],
    );
}

/**
 *
 * Очистить сконифгурированные стабы для метода `getWidget`
 */
export function clearWidgetsStubs() {
    _widgetStubsRegistry = {};
}

/**
 * Очистить конфигурацию стабов ресурса
 */
export function clearResourceStubs() {
    _resourceStubParams = [];
}

/**
 *
 * Вернуть базовую логику работы `stout.getWidget`
 */
export function setOriginalGetWidget() {
    stout.getWidget = originalGetWidget;
}

/**
 *
 * Возвращает fake-object с методами из прототипа Request
 * @returns {Request}
 */
export function createRequestStub() {
    const spy = createSpyObj(
        'requestStub',
        Object.getOwnPropertyNames(Request.prototype),
    );
    spy.params = {};

    return spy;
}

/**
 * Создаёт фейковый объект Request c переданными параметрами
 * @param {Object} [requestParams={}] - параметры запроса
 * @param {Object} [requestParams.data]
 * @param {Object} [requestParams.params]
 * @param {String} [requestParams.ip]
 * @param {String} [requestParams.port]
 * @param {String} [requestParams.remoteIp]
 * @param {String} [requestParams.remotePort]
 * @param {String} [requestParams.method]
 * @param {Object} [requestParams.headers]
 * @param {Object} [requestParams.cookie]
 * @param {String} [requestParams.body]
 * @param {String} [requestParams.url]
 * @param {Object} [requestParams.abt]
 * @returns {Request}
 */
export function createRequestFake(requestParams = {}) {
    const httpRequest = {
        client: {
            localAddress: requestParams.ip,
            localPort: requestParams.port,
            remoteAddress: requestParams.remoteIp,
            remotePort: requestParams.remotePort,
        },
        method: requestParams.method || 'GET',
        headers: _.assign(
            {
                host: 'localhost',
                cookie: _.map(
                    requestParams.cookie,
                    (cookieValue, cookeName) =>
                        `${cookeName}=${encodeURIComponent(cookieValue)}`,
                ).join('; '),
            },
            requestParams.headers,
        ),
        body: requestParams.body,
        url: requestParams.url || '/',
    };

    const request = new Request(httpRequest);

    request.params = requestParams.params || {};
    request.data = requestParams.data || {};
    request.files = requestParams.files;
    request.abt = requestParams.abt || DEFAULT_ABT_DATA;

    return request;
}

/**
 * Создаёт фейковый объект Response c переданными параметрами
 * @returns {Object}
 */
export function createResponseFake() {
    return {
        end: () => {},
        setHeader: () => {},
        setCookie: () => {},
    };
}

/**
 * Создаёт фейковый объект User.
 *
 * @param {Object} [params]
 * @param {Object} [params.isNeedToConvertCurrency=false]
 * @returns {Object}
 */
export function createUserFake(params = {}): StoutUser {
    const user = {
        ips: '',
        region: deepmerge(
            {
                id: 213,
                name: 'Москва',
                realId: '',
                timezone: {},
                info: {},
                linguistics: {
                    preposition: 'в',
                    prepositional: 'Москве',
                },
                SOURCES: {
                    INTERNAL_NETWORK: 10,
                    DEFAULT: 20,
                    DOMAIN: 30,
                    IP: 40,
                    DETECTED: 50,
                    SETTINGS: 60,
                    EXTRA: 70,
                },
                __regionsBySource: {
                    40: 213,
                },

                idBySource(sourceId) {
                    return this.__regionsBySource[sourceId];
                },

                geobase: {
                    __countriesById: {
                        // Moscow - Russia
                        213: 225,
                        // Ruan - France
                        105065: 124,
                    },

                    __regionByIpMap: {
                        '10.10.10.10': 213,
                        '11.11.11.11': 105065,
                    },

                    getRegionByIp(ip) {
                        return Promise.resolve({
                            // eslint-disable-next-line no-bitwise
                            id: this.__regionByIpMap[ip] | 213,
                        });
                    },

                    getCountryInfo(id) {
                        return Promise.resolve({id: this.__countriesById[id]});
                    },
                },
            },
            params.region ?? {},
        ),
        UID: params.UID || '9876543210',
        yandexuid: params.yandexuid || '1234567890',
        browser: params.browser || {},
        isNeedToConvertCurrency: () => params.isNeedToConvertCurrency !== false,
        initBrowser: () => ({
            onResolve: () => {},
            onReject: () => {},
        }),
        hasExtendedPermissions: () => !!params.hasExtendedPermissions,
        isRobot: () => !!params.isRobot,
        isAuth: () => !!params.isAuth,
        isJsAvailable: () => params.isJsAvailable !== false,
        getDefaultEmail: () =>
            params.defaultEmail || 'vasya_pupkin@yandex-team.ru',
        isYandexEmployee: () => !!params.isYandexEmployee,
        isBetaTester: () => !!params.isBetaTester,
        dbField: () => params.dbFields || '',
    };

    user.settings = {
        __settings: {...params.settings},
        getSetting(name) {
            return this.__settings[name];
        },
        setSetting(name, value) {
            this.__settings[name] = value;
        },
    };

    return user;
}

/**
 *
 * Инстанцировать виджет с переданными параметрами
 * @param {stout#Widget} Widget
 * @param {Object} [widgetParams]
 * @param {Request} [request]
 * @param {User} [user]
 * @returns {stout#Widget}
 */
export function setupWidget(Widget, widgetParams, request, user) {
    const parent = createSpyObj('pageStub', ['setCookie', 'setHeader']);

    parent._modules = {};
    parent.request = request || module.exports.createRequestFake();
    parent.ctx = {logger};

    if (user) {
        parent.request.setData('user', user);
    }

    return Widget.make([parent, widgetParams], [widgetParams]);
}

/**
 *
 * Инстанцировать модуль с переданными параметрами
 * @param {stout#Module} Module
 * @param {Request} [request]
 * @param {Object} [moduleParams]
 * @param {User} [user]
 * @returns {stout#Module}
 */
export function setupModule(Module, request, moduleParams, user) {
    const parent = jest.fn();

    parent.route = module.exports.createRouteStub();
    parent._modules = {};
    parent.request = request || module.exports.createRequestFake();

    if (user) {
        parent.request.setData('user', user);
    }

    return Module.make([parent, moduleParams], [moduleParams]);
}

/**
 *
 * Инстанцировать страницу с переданными параметрами
 * @param {stout#Page} Page
 * @param {Object} [params]
 * @param {http#ClientRequest} [params.request] кастомный объект запроса. Если не передан, будет обычный фейк
 * @param {http#ServerResponse} [params.response] кастомный объект ответа. Если не передан, будет обычный фейк
 * @param {Route} [params.route] кастомный объект роута. Если не передан, будет обычный стаб
 * @param {User} [user]
 * @returns {stout#Page}
 */
export function setupPage(Page, params = {}, user = null) {
    const fakeRequest = params.request || module.exports.createRequestFake();
    const fakeResponse = params.response || module.exports.createResponseFake();
    const route = params.route || module.exports.createRouteStub();

    if (user) {
        fakeRequest.setData('user', user);
    }

    return Page.make([fakeRequest, fakeResponse, route], [fakeRequest]);
}

export type Route = {
    getName(): string;
    getData(): unknown;
};

/**
 * Создаем стаб роута
 * @param {RouteOptions} [config] - конфигурация роута, объект той же структуры, что используется в конфигах
 *          приложения, он же RouteOptions для susanin.
 *          Если конфиг не передан, то используется конфиг по умолчанию из createDefaultRouteConfig
 */
export function createRouteStub(
    config = module.exports.createDefaultRouteConfig(),
): Route {
    const routeStub = {};
    routeStub.getData = jest.fn(() => config.data);
    routeStub.getName = jest.fn(() => config.name);

    return routeStub;
}

/**
 * Создаем дефолтную конфигурацаю роута
 */
export function createDefaultRouteConfig() {
    return {
        name: 'market:fake-route',
        data: {
            pageData: {},
        },
    };
}

/**
 *
 * Вернуть базовую логику работы метода `resource`
 */
export function setOriginalResource() {
    ResourceModule.prototype.resource = originalResource;

    module.exports.clearResourceStubs();
}

/**
 * Конфигурация ответа при вызове ресурса (через `this.resource`)
 *
 * @typedef {Object} ResourceStubParams
 * @property {String} [resourceId] - id ресурса, если не указан, то сработает для всех ресурсов
 * @property {Object} [params] - параметры, с которыми должен быть вызван ресурс
 * @property {*} resolve - вернуть этот ответ, обернутый в `resolve`
 * @property {*} reject - вернуть этот ответ, обернутый в `reject`
 */

/**
 *
 * Сконфигурировать ответы метода `resource`
 * Если передан массив конфигураций, то будет использована последняя сработавшая
 *
 * @example
 * // стаб всех вызовов ресурса
 * stoutTestHelpers.stubResource();
 *
 * @example
 * // стаб вызова конкретного ресурса зарезолвленым ответом
 * stoutTestHelpers.stubResource({resourceId: 'myResource.method', resolve: {my: 'response'}});
 *
 * @example
 * // стаб вызова конкретного ресурса с ответом в зависимости от параметров вызова
 * var spy = stoutTestHelpers.stubResource([
 *  {resourceId: 'myResource.method', resolve: {my: 'response'}},
 *  {resourceId: 'myResource.method', params: {unknown: '1'}, reject: 'Unknown parameter'}
 * ]);
 *
 * expect(spy).toHaveBeenCalled();
 *
 * @param {ResourceStubParams|ResourceStubParams[]} [newStubParams]
 * @returns {Function} spy-object
 */
export function stubResource(newStubParams) {
    const spy = jest.fn();

    if (!_.isUndefined(newStubParams)) {
        // eslint-disable-next-line no-param-reassign
        newStubParams = _.castArray(newStubParams);
        _resourceStubParams = _mergeResourceParams(
            _resourceStubParams,
            newStubParams,
        );
    }

    ResourceModule.prototype.resource = spy;
    ResourceModule.prototype.resource.mockImplementation(
        (resourceId, params) => {
            let result = Response.resolve({});

            _.forEach(_resourceStubParams, stubParams => {
                let resourceResult;

                if (stubParams.reject) {
                    resourceResult = Response.reject(stubParams.reject);
                } else {
                    resourceResult = Response.resolve(
                        _.isUndefined(stubParams.resolve)
                            ? {}
                            : stubParams.resolve,
                    );
                }

                if (!stubParams.resourceId) {
                    result = resourceResult;
                    return;
                }

                if (stubParams.resourceId === resourceId) {
                    if (
                        !stubParams.params ||
                        !difference(stubParams.params, params)
                    ) {
                        result = resourceResult;
                    }
                }
            });

            return result;
        },
    );

    return spy;
}

/**
 *
 * Вернуть базовую логику работы метода `resource`
 */
export function setOriginalWidget(Base) {
    Base.prototype.widget = originalWidget;
}

/**
 * Конфигурация ответа при вызове виджета (через `this.widget`)
 * @typedef {Object} WidgetStubParams
 * @property {String} [widget] - имя виджета, если не указан, то сработает для всех виджетов
 * @property {Object} [params] - параметры, с которыми должен быть вызван виджет
 * @property {*} result - вернуть этот результат работы виджета
 * @property {*} reject - вернуть ошибку работы виджета
 */
/**
 *
 * Сконфигурировать ответы метода `widget`
 * Если передан массив конфигураций, то будет использована последняя сработавшая
 *
 * @example
 * // стаб всех вызовов `this.widget`
 * stoutTestHelpers.stubWidget(SomeWidget);
 *
 * @example
 * // стаб вызова конкретного виджета с указанием результатов его работы
 * stoutTestHelpers.stubWidget(SomeWidget, {widget: 'MyWidget', result: {my: 'result'}});
 *
 * @example
 * // стаб вызова конкретного виджета с результатом, зависящим от параметров вызова
 * stoutTestHelpers.stubWidget(SomeWidget, [
 *  {widget: 'MyWidget', result: {my: 'response'}},
 *  {widget: 'MyWidget', params: {unknown: '1'}, reject: 'Unknown parameter'}
 * ]);
 *
 * @param {Base} Base - виджет / страница
 * @param {WidgetStubParams|WidgetStubParams[]} [stubParams]
 * @returns {Function} spy-object
 */
export function stubWidget(Base, stubParams) {
    return stubStoutApiMethod(Base, stubParams, 'widget');
}

/**
 * Конфигурация ответа при вызове модуля (через `this.module`)
 * @typedef {Object} ModuleStubParams
 * @property {String} [module] - имя модуля, если не указан, то сработает для всех модулей
 * @property {Object} [params] - параметры, с которыми должен быть вызван модуль
 * @property {*} result - вернуть этот результат работы модуля
 * @property {*} reject - вернуть ошибку работы модуля
 */
/**
 *
 * Сконфигурировать ответы метода `module`
 * Если передан массив конфигураций, то будет использована последняя сработавшая
 *
 * @example
 * // стаб всех вызовов `this.module`
 * stoutTestHelpers.stubModule(SomeWidget);
 *
 * @example
 * // стаб вызова конкретного виджета с указанием результатов его работы
 * stoutTestHelpers.stubModule(SomeWidget, {module: 'MyWidget', result: {my: 'result'}});
 *
 * @example
 * // стаб вызова конкретного виджета с результатом, зависящим от параметров вызова
 * stoutTestHelpers.stubModule(SomeWidget, [
 *  {module: 'MyModule', result: {my: 'response'}},
 *  {module: 'MyModule', params: {unknown: '1'}, reject: 'Unknown parameter'}
 * ]);
 *
 * @param {Base} Base - виджет / страница / модуль
 * @param {ModuleStubParams|ModuleStubParams[]} [stubParams]
 */
export function stubModule(Base, stubParams) {
    return stubStoutApiMethod(Base, stubParams, 'module', originalModule);
}

/**
 *
 * Вернуть базовую логику работы метода `module`
 */
export function setOriginalModule(Base) {
    Base.prototype.module = originalModule;
}

/**
 * Нужно, чтобы переопределить страницу с определённым именем,
 * если хотим отнаследовать тестируемую страницу от фейка
 * @param PageName
 * @param Page
 */
export function stubPage(PageName, Page) {
    _pageStubsRegistry[PageName] = Page;

    stout.getPage = jest.fn(
        // eslint-disable-next-line no-shadow
        PageName => _pageStubsRegistry[PageName] || originalGetPage(PageName),
    );
}

/**
 * Очистить стабы страниц
 */
export function clearPageStubs() {
    stout.getPage = originalGetPage;
    _pageStubsRegistry = [];
}

export function setupStoutRouter(router = defaultRouterMock) {
    // Делаем require заново, потому что в тестах может быть вызов jest.resetModules()
    // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
    require('@yandex-market/stout').router = router;
}

export function clearStoutRouter() {
    // eslint-disable-next-line @typescript-eslint/no-var-requires,global-require
    require('@yandex-market/stout').router = null;
}

function stubStoutApiMethod(Base, stubParams, entityName, defaultValueGetter) {
    let finalStubParams;

    if (typeof stubParams !== 'undefined') {
        finalStubParams = _.isArray(stubParams) ? stubParams : [stubParams];
    } else {
        finalStubParams = [];
    }

    const spy = jest.fn(function stub(entity, params) {
        let result;

        // eslint-disable-next-line no-shadow
        _.forEach(finalStubParams, stubParams => {
            let entityResult;

            if (stubParams.reject) {
                entityResult = Response.reject(stubParams.reject);
            } else {
                entityResult = Response.resolve(stubParams.result);
            }

            if (!stubParams[entityName]) {
                result = entityResult;
                return;
            }

            const expectedEntity = stubParams[entityName];

            if (entity === expectedEntity || entity.name === expectedEntity) {
                if (
                    !stubParams.params ||
                    !difference(stubParams.params, params)
                ) {
                    result = entityResult;
                }
            }
        });

        if (!result) {
            result = defaultValueGetter
                ? defaultValueGetter.call(this, entity, params)
                : Response.resolve();
        }

        result.name = entity.name;
        return result;
    });

    Base.prototype[entityName] = spy;

    return spy;
}

/**
 * Список имен параметров ресурса, которые участвуют в сравнении
 *
 * @type {string[]}
 * @private
 */
const _comparableParams = ['resourceId', 'params'];

/**
 * Возвращает объект только с теми параметрами ресурса,
 * которые можно сравнить
 *
 * @param {Object} resourceParams
 * @returns {Object}
 * @private
 */
const _pickComparable = _.partialRight(_.pick, _comparableParams);

/**
 * Сравнивает параметры стаба ресурса без учета параметра
 * результата вызова ресурса.
 *
 * @param {Object} resourceParam1
 * @param {Object} resourceParam2
 * @returns {boolean}
 * @private
 */
const _compareResourceParams = _.flow(
    _.rest(_.identity),
    _.partialRight(_.map, _pickComparable),
    _.spread(_.isEqual),
);

/**
 * Смешивает параметры для стаба ресурса,
 * заменяя старую конфигурацию на основе имени
 * ресурса и параметрах вызова. Возвращает
 * результирующие параметры.
 *
 * @param {Array} oldParams
 * @param {Array} newParams
 * @returns {Array}
 * @private
 */
function _mergeResourceParams(oldParams, newParams) {
    return _.concat(
        _.differenceWith(oldParams, newParams, _compareResourceParams),
        newParams,
    );
}
