/* global describe, it, beforeEach, afterEach */

/* eslint-disable no-console */
/* eslint-disable import/no-extraneous-dependencies */

let sinon = require('sinon');
let Vow = require('vow');
let assert = require('chai')
    .use(require('./lib/chai-helpers'))
    .assert;
let Debug = require('../../nodules-libs').Debug;
let Url = require('../../nodules-libs').Url;
let util = require('../../nodules-libs').util;

describe('Dispatcher', function() {
    let app = require('../app');
    let AppCoreError = app.AppCoreError;
    let Dispatcher = app.Dispatcher;
    let CoreController = app.Controller;
    let dispatcher;
    let createMockParams = require('./mocks/controller_params');
    let config = require('./fakes/config');
    let susaninMock = require('./mocks/susanin-for-find-route');
    let susaninUrlMock = require('./mocks/susanin-for-make-url');
    let susanin = susaninMock(null, {});
    let errorLogger = require('../app/lib/error_logger');
    let emptyFunc = sinon.stub();
    let TIMER_ID = 'Dispatcher';

    AppCoreError.setLogger(sinon.stub());
    Debug.prototype.log = sinon.spy(Debug.prototype.log);

    beforeEach(function() {
        dispatcher = new Dispatcher({
            dirname: __dirname,
            config: config,
            susanin: susanin,
            controller: CoreController.create(),
        });
    });

    describe('method', function() {
        let reqRes;
        let IP = '93.158.190.144';

        beforeEach(function() {
            reqRes = createMockParams({
                req: {
                    headers: {
                        host: 'wmfront.yandex.ru',
                        'x-real-ip': IP,
                        'x-forwarded-proto': 'https',
                    },
                    cookies: {
                        foo: 'bar',
                        notifications: {},
                    },
                    method: 'POST',
                    url: '/path?param=val',
                },
            });
        });

        describe('#init()', function() {
            let initMiddleware;

            beforeEach(function() {
                initMiddleware = dispatcher.init();
            });

            it('should return a function', function() {
                assert.isFunction(initMiddleware);
            });

            describe('middleware', function() {
                it('should extend the request properly', function() {
                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.instanceOf(reqRes.req.debug, Debug);

                    assert.isTrue(reqRes.req.debug.log.calledWith(reqRes.req, 'Request'));
                    assert.isTrue(reqRes.req.debug.log.calledWith(reqRes.res, 'Response'));

                    assert.instanceOf(reqRes.req.urlHelper, Url);
                    assert.strictEqual(reqRes.req.currentDomain, 'ru');

                    assert.deepEqual(reqRes.req.env, {
                        dirname: __dirname,
                        client_ip: IP,
                        config: config,
                    });

                    assert.strictEqual(reqRes.req.susanin, susanin);
                });

                it('should set req.protocol equal to value of header X-Forwarded-Proto', function() {
                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.equal(reqRes.req.protocol, reqRes.req.headers['x-forwarded-proto']);
                });

                it('should set req.protocol to `http` if header X-Forwarded-Proto is not being passed', function() {
                    delete reqRes.req.headers['x-forwarded-proto'];
                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.equal(reqRes.req.protocol, 'http');
                });

                it('should set client_ip equal to 127.0.0.1 if it is not contained in headers', function() {
                    delete reqRes.req.headers['x-real-ip'];
                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.strictEqual(reqRes.req.env.client_ip, '127.0.0.1');
                });

                it('should extend the response properly', function() {
                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.deepEqual(reqRes.res.data, {});
                    assert.strictEqual(reqRes.res.statusCode, 200);
                    assert.strictEqual(reqRes.res.headers['content-type'], 'text/html');
                });

                it('should start timer', function() {
                    let startTimer = sinon.spy(Debug.prototype, 'startTimer');

                    initMiddleware(reqRes.req, reqRes.res, emptyFunc);

                    assert.isTrue(startTimer.calledOn(reqRes.req.debug));
                    assert.isTrue(startTimer.calledWith(TIMER_ID));
                });

                it('should call next()', function() {
                    assert.callsNextWith(initMiddleware, [reqRes.req, reqRes.res]);
                });
            });
        });

        describe('#_getControllerData()', function() {
            it('should add request and response to ControllerData', function() {
                let ControllerData;

                reqRes.req.susanin = susaninMock(null, {});
                ControllerData = dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.strictEqual(ControllerData.data.req, reqRes.req);
                assert.strictEqual(ControllerData.data.res, reqRes.res);
            });

            it('should set type, directory and action by default', function() {
                let ControllerData;

                reqRes.req.susanin = susaninMock(null, {});

                ControllerData = dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.strictEqual(ControllerData.data.type, dispatcher.__defaults.controller);
                assert.strictEqual(ControllerData.data.directory, dispatcher.__defaults.directory);
                assert.strictEqual(ControllerData.action, dispatcher.__defaults.action);
            });

            it('should get type, directory and action from route', function() {
                let route = {
                    controller: 'my-awesome-controller',
                    directory: 'some-controllers-directory',
                    action: 'superaction',
                };
                let ControllerData;

                reqRes.req.susanin = susaninMock(route, {});

                ControllerData = dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.strictEqual(ControllerData.data.type, route.controller);
                assert.strictEqual(ControllerData.data.directory, route.directory);
                assert.strictEqual(ControllerData.action, route.action);
            });

            it('should get type, directory and action from route params', function() {
                let routeParams = {
                    controller: 'my-awesome-controller',
                    directory: 'some-controllers-directory',
                    action: 'superaction',
                };
                let ControllerData;

                reqRes.req.susanin = susaninMock({}, util.extend({}, routeParams));

                ControllerData = dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.strictEqual(ControllerData.data.type, routeParams.controller);
                assert.strictEqual(ControllerData.data.directory, routeParams.directory);
                assert.strictEqual(ControllerData.action, routeParams.action);
            });

            it('should delete type, directory and action from route params', function() {
                let routeParams = {
                    controller: true,
                    directory: true,
                    action: true,
                };

                reqRes.req.susanin = susaninMock({}, routeParams);
                dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.notProperty(routeParams, 'controller');
                assert.notProperty(routeParams, 'directory');
                assert.notProperty(routeParams, 'action');
            });

            it('should set req.foundRoute', function() {
                reqRes.req.susanin = susaninMock({}, {});

                dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.property(reqRes.req, 'foundRoute');
            });

            it('should set route', function() {
                let ControllerData;

                reqRes.req.susanin = susaninUrlMock;
                reqRes.req.url = '/testcontroller/';
                reqRes.req.method = 'GET';

                ControllerData = dispatcher._getControllerData(reqRes.req, reqRes.res);

                assert.instanceOf(ControllerData.data.route, require('susanin').Route);
            });
        });

        describe('#_callController()', function() {
            it('should return rejected promise if no controller found', function(done) {
                let promise = dispatcher._callController({
                    data: {},
                    action: 'build',
                });

                assert.isTrue(Vow.isPromise(promise));
                promise
                    .always(function() {
                        assert.isTrue(promise.isRejected());
                        assert.instanceOf(promise.valueOf(), AppCoreError);
                    })
                    .done(done);
            });

            it('should call controller action', function(done) {
                let ACTION = 'superaction';
                let callAction = sinon.stub().returns(Vow.resolve());

                dispatcher.__opts.controller = {
                    factory: sinon.stub().returns({ callAction: callAction }),
                };

                dispatcher._callController({
                    data: {},
                    action: ACTION,
                })
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(callAction.calledOnce);
                        assert.isTrue(callAction.calledWith(ACTION));
                    })
                    .done(done);
            });
        });

        describe('#prepareResponse()', function() {
            let responseMiddleware;

            beforeEach(function() {
                responseMiddleware = dispatcher.prepareResponse();
            });

            it('should return a function', function() {
                assert.isFunction(responseMiddleware);
            });

            describe('middleware', function() {
                beforeEach(function() {
                    reqRes.req.susanin = susaninMock({}, {});
                });

                it('should end the response with 200 status code if the request method is HEAD', function() {
                    reqRes.req.method = 'HEAD';
                    responseMiddleware(reqRes.req, reqRes.res);

                    assert.isTrue(reqRes.res.end.calledOnce);
                    assert.strictEqual(reqRes.res.statusCode, 200);
                });

                it('should end the response with 404 status code if the request method is HEAD and controller' +
                    ' not found', function() {
                    reqRes.req.method = 'HEAD';

                    sinon.stub(dispatcher, '_getControllerData').returns({ data: { type: '404' } });

                    responseMiddleware(reqRes.req, reqRes.res);

                    assert.isTrue(reqRes.res.end.calledOnce);
                    assert.strictEqual(reqRes.res.statusCode, 404);
                });

                it('should call _callController method', function() {
                    let _callController = sinon.stub(dispatcher, '_callController').returns(Vow.resolve());

                    responseMiddleware(reqRes.req, reqRes.res);

                    assert.isTrue(_callController.calledOnce);
                });

                it('should end response with the result of the controller', function(done) {
                    let result = 'controller data result';

                    sinon.stub(dispatcher, '_callController').returns(Vow.resolve(result));

                    responseMiddleware(reqRes.req, reqRes.res)
                        .always(function() {
                            assert.isTrue(reqRes.res.end.calledOnce);
                            assert.isTrue(reqRes.res.end.calledWith(result));
                        })
                        .done(done);
                });

                it('should call next with error if controller execution failed', function(done) {
                    let error = AppCoreError.createError();
                    let next = sinon.spy();

                    sinon.stub(dispatcher, '_callController').returns(Vow.reject(error));

                    responseMiddleware(reqRes.req, reqRes.res, next)
                        .always(function() {
                            assert.isTrue(next.calledOnce);
                            assert.isTrue(next.calledWith(error));
                        })
                        .done(done);
                });

                it('should finish timer', function(done) {
                    let finishTimer = sinon.spy(Debug.prototype, 'finishTimer');

                    reqRes.req.body = {};
                    reqRes.req.debug = new Debug();
                    sinon.stub(dispatcher, '_callController').returns(Vow.resolve());

                    responseMiddleware(reqRes.req, reqRes.res, emptyFunc)
                        .always(function() {
                            assert.isTrue(finishTimer.calledOn(reqRes.req.debug));
                            assert.isTrue(finishTimer.calledWith(TIMER_ID));
                        })
                        .done(done);
                });

                it('should log request url and body', function(done) {
                    let consoleLog = sinon.stub(console, 'log');

                    reqRes.req.body = { foo: 'bar' };
                    reqRes.req.debug = new Debug();
                    sinon.stub(dispatcher, '_callController').returns(Vow.resolve());

                    responseMiddleware(reqRes.req, reqRes.res, emptyFunc)
                        .always(function() {
                            assert.isTrue(consoleLog.calledWith('For url ' + reqRes.req.url));
                            assert.isTrue(consoleLog.calledWith('With body ' + JSON.stringify(reqRes.req.body)));

                            consoleLog.restore();
                        })
                        .done(done);
                });
            });
        });

        describe('#handleCookieError()', function() {
            let cookieErrorMiddleware;

            beforeEach(function() {
                cookieErrorMiddleware = dispatcher.handleCookieError();
            });

            it('should return a function', function() {
                assert.isFunction(cookieErrorMiddleware);
            });

            describe('middleware', function() {
                it('should always log the AppCoreError.COOKIE_PARSE_ERROR', function() {
                    assert.logsTerror(AppCoreError, 'COOKIE_PARSE_ERROR', cookieErrorMiddleware,
                        new Error(), reqRes.req, reqRes.res, sinon.stub());
                    assert.logsTerror(AppCoreError, 'COOKIE_PARSE_ERROR', cookieErrorMiddleware,
                        AppCoreError.createError(AppCoreError.CODES.CONTROLLER_NOT_FOUND),
                        reqRes.req, reqRes.res, sinon.stub());
                });

                it('should clean cookies', function() {
                    cookieErrorMiddleware(new Error(), reqRes.req, reqRes.res, sinon.stub());

                    assert.deepEqual(reqRes.req.cookies, {});
                });

                it('should call next()', function() {
                    assert.callsNextWith(cookieErrorMiddleware, [new Error(), reqRes.req, reqRes.res]);
                });
            });
        });

        describe('#safetyEncode', function() {
            /* jshint maxlen:false */
            let redirectUrlsPairs = {
                // retpath уже закодирован, лишнего энкодинга не надо
                'https://passport.yandex.ru/passport?mode=auth&retpath=https%3A%2F%2Fwebmaster.yandex.ru%2Fsites%2F': true,
                // hostId уже закодирован, лишнего энкодинга не надо
                '/sites/add/?hostId=http%3Awww.cplusplus.com%3A80': true,
                // А тут надо закодировать кириллицу
                '/site/http:свадебный-орёл.рф:80/dashboard/':
                    '/site/http:%D1%81%D0%B2%D0%B0%D0%B4%D0%B5%D0%B1%D0%BD%D1%8B%D0%B9-%D0%BE%D1%80%D1%91%D0%BB.%D1%80%D1%84:80/dashboard/',
            };
            /* jshint maxlen:120 */

            it('should encode what is supposed to be encoded and vice versa', function() {
                Object.keys(redirectUrlsPairs).forEach(function(someRedirectUrl) {
                    // Энкодинга не нужно, метод должен вернуть ровно то, что подано на вход
                    if (redirectUrlsPairs[someRedirectUrl] === true) {
                        assert.equal(dispatcher.safetyEncode(someRedirectUrl), someRedirectUrl);
                    } else {
                        // Энкодинг нужен, метод должен вернуть соответствующее ключу значение
                        assert.equal(dispatcher.safetyEncode(someRedirectUrl), redirectUrlsPairs[someRedirectUrl]);
                    }
                });
            });
        });

        describe('#handleRedirect()', function() {
            let redirectMiddleware;

            beforeEach(function() {
                redirectMiddleware = dispatcher.handleRedirect();
            });

            it('should return a function', function() {
                assert.isFunction(redirectMiddleware);
            });

            describe('middleware', function() {
                it('should redirect if redirect error handled', function() {
                    let redirectUrl = 'http://some.site.ru/';
                    let redirectStatus = 301;
                    let error = CoreController.CoreControllerError
                        .createError(CoreController.CoreControllerError.CODES.REDIRECT)
                        .bind({
                            location: redirectUrl,
                            status: redirectStatus,
                        });

                    redirectMiddleware(error, reqRes.req, reqRes.res);
                    assert.strictEqual(reqRes.res.statusCode, redirectStatus);
                    assert.strictEqual(reqRes.res.headers.location, redirectUrl);
                });

                it('should call next(error) if error is not redirect', function() {
                    let error = new Error();

                    error.param = 'foo';

                    assert.callsNextWith(redirectMiddleware, [error, reqRes.req, reqRes.res], error);
                });
            });
        });

        describe('#handleError()', function() {
            let errorMiddleware;

            beforeEach(function() {
                errorMiddleware = dispatcher.handleError();
            });

            it('should return a function', function() {
                assert.isFunction(errorMiddleware);
            });

            describe('middleware', function() {
                it('should log an AppCoreError', function() {
                    assert.logsTerror(AppCoreError, 'UNKNOWN_ERROR', errorMiddleware,
                        new Error(), reqRes.req, reqRes.res);
                });

                it('should set 500 status code', function() {
                    errorMiddleware(new Error(), reqRes.req, reqRes.res, emptyFunc);
                    assert.strictEqual(reqRes.res.statusCode, 500);
                });

                it('should set 404 status code if it\'s DATA_NOT_FOUND error', function() {
                    let notFoundError = CoreController.CoreControllerError
                        .createError(CoreController.CoreControllerError.CODES.DATA_NOT_FOUND);

                    errorMiddleware(notFoundError, reqRes.req, reqRes.res, emptyFunc);
                    assert.strictEqual(reqRes.res.statusCode, 404);
                });

                it('should end the response', function() {
                    errorMiddleware(new Error(), reqRes.req, reqRes.res, emptyFunc);
                    assert.isTrue(reqRes.res.end.calledOnce);
                });
            });
        });

        describe('#setQloudLogger()', function() {
            let originals = {
                log: null,
                error: null,
                warn: null,
                info: null,
            };

            beforeEach(function() {
                Object.keys(originals).forEach(function(method) {
                    originals[method] = console[method];
                    console[method] = sinon.spy(console, method);
                });
            });

            afterEach(function() {
                Object.keys(originals).forEach(function(method) {
                    console[method] = originals[method];
                });
            });

            it('should set qloud logger', function() {
                let originalSetLogger = errorLogger.setLogger;
                let originalQloudLogger = errorLogger.qloudLogger;

                let setLogger = sinon.spy(errorLogger, 'setLogger');

                errorLogger.qloudLogger = sinon.stub().returns(emptyFunc);

                dispatcher.setQloudLogger();

                assert.strictEqual(setLogger.getCall(0).args[0], emptyFunc);

                errorLogger.setLogger = originalSetLogger;
                errorLogger.qloudLogger = originalQloudLogger;
            });

            it('should change console methods', function() {
                let spies = {
                    log: console.log,
                    error: console.error,
                    warn: console.warn,
                    info: console.info,
                };

                let message = 'test';
                let result = JSON.stringify({
                    msg: '[pid:' + process.pid + '] ' + message,
                    '@fields': {
                        pid: process.pid,
                    },
                });

                dispatcher.setQloudLogger();

                console.log(message);
                assert.strictEqual(spies.log.getCall(0).args[0], result);

                console.error(message);
                assert.strictEqual(spies.error.getCall(0).args[0], result);

                console.warn(message);
                assert.strictEqual(spies.warn.getCall(0).args[0], result);

                console.info(message);
                assert.strictEqual(spies.info.getCall(0).args[0], result);
            });
        });

        describe('#setDefaultLogger()', function() {
            it('should set default logger', function() {
                let originalSetLogger = errorLogger.setLogger;
                let originalDebugLogger = errorLogger.debugLogger;

                let setLogger = sinon.spy(errorLogger, 'setLogger');

                errorLogger.debugLogger = sinon.stub().returns(emptyFunc);

                dispatcher.setDefaultLogger();

                assert.strictEqual(setLogger.getCall(0).args[0], emptyFunc);

                errorLogger.setLogger = originalSetLogger;
                errorLogger.debugLogger = originalDebugLogger;
            });
        });
    });
});
