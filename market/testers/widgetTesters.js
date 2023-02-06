const _ = require('lodash');
const stout = require('@yandex-market/stout');
const {ValidationError} = require('@yandex-market/validator');
const stoutTestHelpers = require('@self/platform/spec/unit/helpers/stoutTestHelpers');
const mockRequire = require('mock-require');

module.exports = function (widgetName) {
    return {
        /**
         * Проверяет, что падает ошибка валидации.
         * @param {Object} params Параметры инстанцирования виджета.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.widgetParams] Параметры, которые будут переданы в виджет.
         * @param {[]|Object} [params.stubs] Стабы виджета, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {string} matcher Подстрока сообщения об ошибке валидации.
         * @param {Function} done Обратный вызов на завершение теста.
         */
        validationErrorExpected: function (params, matcher, done) {
            validationErrorExpectedInternal(widgetName, params, matcher, doneWithClear(done));
        },

        /**
         * Проверяет, что происходит обращение к указанным ресурсам/модулям/виджетам.
         * @param {Object} params Параметры инстанцирования виджета.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.widgetParams] Параметры, которые будут переданы в виджет.
         * @param {[]|Object} [params.stubs] Стабы виджета, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} matcher Ожидаемые параметры вызова ресурса/модуля/виджета.
         * @param {Object} [matcher.resource]
         * @param {Object} [matcher.module]
         * @param {Object} [matcher.widget]
         * @param {Function} done Обратный вызов на завершение теста.
         */
        calls(params, matcher, done) {
            callsInternal(widgetName, params, matcher, doneWithClear(done), true);
        },

        /**
         * Проверяет, что не происходит обращения к указанным ресурсам/модулям/виджетам.
         * @param {Object} params Параметры инстанцирования виджета.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.widgetParams] Параметры, которые будут переданы в виджет.
         * @param {[]|Object} [params.stubs] Стабы виджета, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} matcher
         * @param {Object} [matcher.resource]
         * @param {Object} [matcher.module]
         * @param {Object} [matcher.widget]
         * @param {Function} done Обратный вызов на завершение теста.
         */
        notCalls(params, matcher, done) {
            callsInternal(widgetName, params, matcher, doneWithClear(done), false);
        },

        /**
         * Проверяет, что результат работы виджета содержит ожидаемые значения.
         * @param {Object} params Параметры инстанцирования виджета.
         * @param {Object} [params.user] Пользователь.
         * @param {Object} [params.widgetParams] Параметры, которые будут переданы в виджет.
         * @param {[]|Object} [params.stubs] Стабы виджета, синтаксис совпадает со стаб-функциями из stoutTestHelpers.
         * @param {Object} matcher Ожидаемый в результате объект.
         * @param {Function} done Обратный вызов на завершение теста.
         */
        hasResults(params, matcher, done) {
            hasResultsInternal(widgetName, params, matcher, doneWithClear(done));
        },
    };
};

function prepareWidgetInstance(widgetName) {
    const fakeUrl = 'https://market.market/market/market';
    stout.router = {
        buildURL: () => fakeUrl,
    };
    jest.spyOn(stout.router, 'buildURL').mockReturnValue(fakeUrl);

    const widget = mockRequire.reRequire(`${process.cwd()}/app/widgets/data/${widgetName}`);
    stoutTestHelpers.stubWidget(widget);
    stoutTestHelpers.stubModule(widget);
    stoutTestHelpers.stubResource();

    return widget;
}

function setupWidgetWithParams(widget, params) {
    const request = stoutTestHelpers.createRequestFake(params.request || {});
    const resultWidget = stoutTestHelpers.setupWidget(widget, params.widgetParams || {}, request, params.user);
    if (params.user) {
        resultWidget.user = params.user;
    }

    return resultWidget;
}

function callsInternal(widgetName, params, matcher, done, shouldCall) {
    const widget = prepareWidgetInstance(widgetName, params);
    const [resourceSpy, moduleSpy, widgetSpy] = stubAll(widget, params);

    setupWidgetWithParams(widget, params).then(() => {
        if (resourceSpy && matcher.resource) {
            _.forEach(matcher.resource, (params, name) => {
                const expectation = shouldCall ? expect(resourceSpy) : expect(resourceSpy).not;
                if (typeof params === 'undefined' && !shouldCall) {
                    const calls = resourceSpy.mock.calls;
                    const calledResourceNames = _.map(calls, _.head);

                    expect(calledResourceNames).not.toEqual(
                        jasmine.arrayContaining([name]),
                        'Resource should not be called'
                    );
                } else {
                    expectation.toHaveBeenCalledWith(name, params);
                }
            });
        }
        if (moduleSpy && matcher.module) {
            _.forEach(matcher.module, (params, name) => {
                const expectation = shouldCall ? expect(moduleSpy) : expect(moduleSpy).not;
                expectation.toHaveBeenCalledWith(name, params);
            });
        }
        if (widgetSpy && matcher.widget) {
            _.forEach(matcher.widget, (params, name) => {
                const expectation = shouldCall ? expect(widgetSpy) : expect(widgetSpy).not;
                expectation.toHaveBeenCalledWith(jasmine.objectContaining({name}), params);
            });
        }
        done();
    }, done);
}

function stubAll(widget, params) {
    const stubs = _.compact(_.castArray(params.stubs));

    const resourceStubs = _.filter(stubs, 'resourceId');
    const resourceSpy = stoutTestHelpers.stubResource(resourceStubs);

    const moduleStubs = _.filter(stubs, 'module');
    const moduleSpy = stoutTestHelpers.stubModule(widget, moduleStubs);

    const widgetStubs = _.filter(stubs, 'widget');
    const widgetSpy = stoutTestHelpers.stubWidget(widget, widgetStubs);

    return [resourceSpy, moduleSpy, widgetSpy];
}

function validationErrorExpectedInternal(widgetName, params, matcher, done) {
    const widget = setupWidgetWithParams(prepareWidgetInstance(widgetName, params), params);
    expect(widget).toBeRejectedWith({
        class: ValidationError,
        message: matcher,
    }, done);
}

function hasResultsInternal(widgetName, params, matcher, done) {
    const widget = prepareWidgetInstance(widgetName, params);
    stubAll(widget, params);

    setupWidgetWithParams(widget, params).then(function () {
        expect(this.getResults()).toEqual(matcher);
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
        stoutTestHelpers.setOriginalGetWidget();

        delete stout.router;

        done();
    };
    return result;
}
