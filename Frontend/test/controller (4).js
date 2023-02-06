/* global describe, it, beforeEach */

var assert = require('chai').assert,
    util = require('nodules-libs').util,
    sinon = require('sinon');

describe('CoreController', function() {
    var app = require('../app'),
        CoreController = app.Controller,
        nodulesControllers = app.NodulesControllers,
        createMockParams = require('./mocks/controller_params');

    it('should be an inheritor of Nodules Controller', function() {
        assert.isTrue(nodulesControllers.Controller.isParentOf(CoreController));
    });

    describe('method', function() {
        var CoreControllerError = CoreController.CoreControllerError,
            coreController;

        beforeEach(function() {
            coreController = new CoreController(createMockParams());
        });

        describe('#throwRedirectError()', function() {
            var redirectUrl = '/redirect',
                redirectStatusCode = 301;

            it('should throw a CoreControllerError with code REDIRECT, passed location and status code', function() {
                try {
                    coreController.throwRedirectError(redirectUrl, redirectStatusCode);
                }
                catch (error) {
                    assert.instanceOf(error, CoreControllerError);
                    assert.strictEqual(error.code, CoreControllerError.CODES.REDIRECT);
                    assert.strictEqual(error.data.location, redirectUrl);
                    assert.strictEqual(error.data.status, redirectStatusCode);
                }
            });
        });

        describe('#rethrowRedirectError()', function() {
            var redirectUrl = '/redirect';

            it('shouldn\'t do anything when passed non-redirect error', function() {
                assert.doesNotThrow(coreController.rethrowRedirectError);
            });

            it('should rethrow passed redirect error', function() {
                try {
                    coreController.throwRedirectError(redirectUrl);
                }
                catch (originalError) {
                    try {
                        coreController.rethrowRedirectError(originalError);
                    }
                    catch (rethrownError) {
                        assert.equal(originalError, rethrownError);
                    }
                }
            });
        });

        describe('#isRedirectError()', function() {
            it('should return true if redirect error was passed', function() {
                var error = CoreControllerError
                    .createError(CoreControllerError.CODES.REDIRECT)
                    .bind({ location: '/' });

                assert.isTrue(coreController.isRedirectError(error));
            });

            it('should return false if error without location was passed', function() {
                var error = CoreControllerError
                    .createError(CoreControllerError.CODES.REDIRECT);

                assert.isFalse(coreController.isRedirectError(error));
            });

            it('should return false if error code is not REDIRECT', function() {
                var error = CoreControllerError.createError('SOME_CODE');

                error.data = { location: '/' };

                assert.isFalse(coreController.isRedirectError(error));
            });

            it('should return false if passed error is not instance of CoreControllerError', function() {
                var error = new Error();

                error.code = CoreControllerError.CODES.REDIRECT;
                error.data = { location: '/' };

                assert.isFalse(coreController.isRedirectError(error));
            });
        });

        describe('#throw404Error()', function() {
            it('should throw a CoreControllerError with code DATA_NOT_FOUND', function() {
                try {
                    coreController.throw404Error();
                }
                catch (error) {
                    assert.instanceOf(error, CoreControllerError);
                    assert.strictEqual(error.code, CoreControllerError.CODES.DATA_NOT_FOUND);
                }
            });
        });

        describe('#is404Error()', function() {
            it('should return true if redirect error was passed', function() {
                var error = CoreControllerError
                    .createError(CoreControllerError.CODES.DATA_NOT_FOUND);

                assert.isTrue(coreController.is404Error(error));
            });

            it('should return false if error code is not REDIRECT', function() {
                var error = CoreControllerError.createError('SOME_CODE');

                assert.isFalse(coreController.is404Error(error));
            });

            it('should return false if passed error is not instance of CoreControllerError', function() {
                var error = new Error();

                error.code = CoreControllerError.CODES.DATA_NOT_FOUND;

                assert.isFalse(coreController.is404Error(error));
            });
        });

        describe('#getPublicParams()', function() {
            it('should return all params without params started with _', function() {
                var publicParams = {
                        foo: 1,
                        bar: 'abc',
                        baz: true
                    };

                coreController._params = util.extend({
                    _privateFoo: 2,
                    _privateBar: 'def',
                    _privateBaz: false
                }, publicParams);

                assert.deepEqual(coreController.getPublicParams(), publicParams);
            });
        });

        describe('#getCurrentRouteName()', function() {
            var routeName = 'fasdfasdf',
                getName = sinon.stub().returns(routeName);

            it('should call #getRoute() and Route#getName() and return routeName', function() {
                var coreController,
                    getRoute,
                    resRouteName;

                coreController = new CoreController(util.extend(true, {
                    route: {
                        getName: getName
                    }
                }, createMockParams()));

                getRoute = sinon.spy(coreController, 'getRoute');

                resRouteName = coreController.getCurrentRouteName();

                assert.isTrue(getRoute.calledOnce);
                assert.isTrue(getName.calledOnce);
                assert.strictEqual(resRouteName, routeName);
            });

            it('should return an emtpy string if there is no route', function() {
                assert.strictEqual(coreController.getCurrentRouteName(), '');
            });
        });
    });
});
