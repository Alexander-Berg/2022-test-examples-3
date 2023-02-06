const _ = require('lodash');
const stout = require('@yandex-market/stout');
const {ValidationError} = require('@yandex-market/validator');
const stoutTestHelpers = require('@self/platform/spec/unit/helpers/stoutTestHelpers');

const FAKE_URL = 'https://market.market/market/market';

module.exports = function (page) {
    return {
        /**
         * Проверяет, что падает ошибка валидации.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {string} matcher Подстрока сообщения об ошибке валидации.
         * @param {Function} done Обратный вызов на завершение теста.
         */
        validationErrorExpected: function (params, matcher, done) {
            errorExpectedInternal(page, params, {
                class: ValidationError,
                message: matcher,
            }, done);
        },

        /**
         * Проверяет, что падает ошибка.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {string} matcher Ожидаемая ошибка
         * @param {Function} done Обратный вызов на завершение теста.
         */
        errorExpected: function (params, matcher, done) {
            errorExpectedInternal(page, params, matcher, doneWithClear(done));
        },

        /**
         * Проверяет, что происходит обращение к указанным ресурсам/модулям/виджетам.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} matcher Ожидаемые параметры вызова ресурса/модуля/виджета.
         * @param {Object} [matcher.resource]
         * @param {Object} [matcher.module]
         * @param {Object} [matcher.widget]
         * @param {Function} done Обратный вызов на завершение теста.
         */
        calls(params, matcher, done) {
            callsInternal(page, params, matcher, doneWithClear(done), true);
        },

        /**
         * Проверяет, что не происходит обращения к указанным ресурсам/модулям/виджетам.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} matcher Ожидаемые параметры вызова ресурса/модуля/виджета.
         * @param {Object} [matcher.resource]
         * @param {Object} [matcher.module]
         * @param {Object} [matcher.widget]
         * @param {Function} done Обратный вызов на завершение теста.
         */
        notCalls(params, matcher, done) {
            callsInternal(page, params, matcher, doneWithClear(done), false);
        },

        /**
         * Проверяет, что страница делает редирект.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} route Pоут.
         * @param {string} route.name Имя роута.
         * @param {Object} [route.params] Параметры роута.
         * @param {Function} done Обратный вызов на завершение теста.
         */
        redirects(params, route, done) {
            redirectsInternal(page, params, route, doneWithClear(done), true);
        },

        /**
         * Проверяет, что страница не делает никаких редиректов.
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Function} done Обратный вызов на завершение теста.
         */
        notRedirects(params, done) {
            redirectsInternal(page, params, null, doneWithClear(done), false);
        },

        /**
         * Проверяет, что страница отвечает с определенным http кодом
         * @param {Object} params Параметры инстанцирования страницы.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.route] Параметры роута, аналогично stoutTestHelpers.createRouteStub.
         * @param {Object} [params.request] Параметры запроса, аналогично stoutTestHelpers.createRequestFake.
         * @param {[]|Object} [params.stubs] Стабы страницы, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {number} statusCode ожидаемый http код ответа
         * @param {Function} done Обратный вызов на завершение теста.
         */
        statusCodeExpected(params, statusCode, done) {
            statusCodeExpectedInternal(page, params, statusCode, done);
        },
    };
};

function preparePageInstance(page) {
    stout.router = {
        buildURL: () => FAKE_URL,
    };
    jest.spyOn(stout.router, 'buildURL').mockReturnValue(FAKE_URL);

    if (!stout.get('config')) {
        stout.set('config', {});
    }

    let result;

    if (_.isString(page)) {
        if (page.startsWith('Html')) {
            result = require(`@self/platform/app/pages/html/${page}`);
        } else if (page.startsWith('Api')) {
            result = require(`@self/platform/app/pages/api/${page}`);
        }
    } else {
        result = page;
    }

    stoutTestHelpers.stubWidget(result);
    stoutTestHelpers.stubModule(result);
    stoutTestHelpers.stubResource();

    return result;
}

function setupPageWithParams(page, params) {
    const setupPageParams = {};

    setupPageParams.request = stoutTestHelpers.createRequestFake(params.request || {});
    if (params.route) {
        const routeStubConfig = _.assign(stoutTestHelpers.createDefaultRouteConfig(), params.route);
        setupPageParams.route = stoutTestHelpers.createRouteStub(routeStubConfig);
    }

    const resultPage = stoutTestHelpers.setupPage(page, setupPageParams, params.user);
    if (params.user) {
        resultPage.user = params.user;
    }

    return resultPage;
}

function callsInternal(page, params, matcher, done, shouldCall) {
    const pageInstance = preparePageInstance(page, params);
    const [resourceSpy, moduleSpy, widgetSpy] = stubAll(pageInstance, params);

    setupPageWithParams(pageInstance, params).then(() => {
        createResourceExpectation(resourceSpy, matcher.resource, shouldCall);
        createExpectation(moduleSpy, matcher.module, shouldCall);
        createExpectation(widgetSpy, matcher.widget, shouldCall);

        done();
    }, done);
}

function createExpectation(spy, matcherDescriptor, shouldCall) {
    if (spy && matcherDescriptor) {
        _.forEach(matcherDescriptor, conf => {
            const expectation = shouldCall ? expect(spy) : expect(spy).not;

            const klass = jasmine.objectContaining({name: conf.name});

            if ('params' in conf) {
                expectation.toHaveBeenCalledWith(klass, conf.params);
            } else {
                expectation.toHaveBeenCalledWith(klass);
            }
        });
    }
}

function createResourceExpectation(spy, matcherDescriptor, shouldCall) {
    if (spy && matcherDescriptor) {
        _.forEach(matcherDescriptor, conf => {
            const expectation = shouldCall ? expect(spy) : expect(spy).not;

            if ('params' in conf) {
                expectation.toHaveBeenCalledWith(conf.name, conf.params);
            } else {
                expectation.toHaveBeenCalledWith(conf.name);
            }
        });
    }
}

function stubAll(page, params) {
    const stubs = _.compact(_.castArray(params.stubs));

    const resourceStubs = _.filter(stubs, 'resourceId');
    const resourceSpy = stoutTestHelpers.stubResource(resourceStubs);

    const moduleStubs = _.filter(stubs, 'module');
    const moduleSpy = stoutTestHelpers.stubModule(page, moduleStubs);

    const widgetStubs = _.filter(stubs, 'widget');
    const widgetSpy = stoutTestHelpers.stubWidget(page, widgetStubs);

    return [resourceSpy, moduleSpy, widgetSpy];
}

function errorExpectedInternal(page, params, matcher, done) {
    const preparedPage = preparePageInstance(page, params);
    stubAll(preparedPage, params);
    const pageInstance = setupPageWithParams(preparedPage, params);

    expect(pageInstance).toBeRejectedWith(matcher, done);
}

function redirectsInternal(page, params, route, done, shouldRedirect) {
    const preparedPage = preparePageInstance(page, params);
    stubAll(preparedPage, params);
    const pageInstance = setupPageWithParams(preparedPage, params);
    pageInstance.then(function () {
        try {
            if (shouldRedirect) {
                expect(this.redirect).toHaveBeenCalledWith(FAKE_URL);

                if ('params' in route) {
                    expect(stout.router.buildURL).toHaveBeenCalledWith(
                        route.name,
                        // Вызов createSyncResolver почему-то приводит к тому, что в request.params
                        // добавляются анонимные функции под Symbol(stringify). Поэтому сравниваем так
                        expect.objectContaining(route.params)
                    );
                } else {
                    expect(stout.router.buildURL).toHaveBeenCalledWith(route.name);
                }
            } else {
                expect(this.redirect).not.toHaveBeenCalled();
            }
        } catch (e) {
            done(e);
        }
        done();
    }, done);
}

function statusCodeExpectedInternal(page, params, statusCode, done) {
    const preparedPage = preparePageInstance(page, params);
    stubAll(preparedPage, params);
    const pageInstance = setupPageWithParams(preparedPage, params);
    pageInstance.then(function () {
        expect(this.setStatus).toHaveBeenCalledWith(statusCode);

        done();
    }, done);
}

function doneWithClear(done) {
    const result = function (err) {
        if (err) {
            done(err);
            return;
        }

        stoutTestHelpers.clearResourceStubs();
        stoutTestHelpers.clearPageStubs();
        stoutTestHelpers.clearWidgetsStubs();
        delete stout.router;
        done();
    };
    return result;
}
