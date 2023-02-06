/* global describe, it, beforeEach */

/* eslint-disable import/no-extraneous-dependencies */

let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');
let Vow = require('vow');

describe('ResourceResponseProcessorMixin', function() {
    let app = require('../app');
    let Controller = app.Controller;
    let Resource = app.Resource;
    let Terror = require('terror');
    let ResourceResponseProcessorMixin = app.ResourceResponseProcessorMixin;
    let ResourceResponseProcessorError = ResourceResponseProcessorMixin.ResourceResponseProcessorError;
    let createMockParams = require('./mocks/controller_params');
    let resourceName = 'resource-name';
    let ProvidersError = require('../../nodules-controllers').ProvidersMixin.ProvidersError;

    ResourceResponseProcessorMixin.ResourceResponseProcessorError.setLogger(sinon.stub());

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin()` properly', function() {
            assert.canBeMixed(Controller.create(), ResourceResponseProcessorMixin,
                'ResourceResponseProcessorMixin', ['name', 'prototype'], ['constructor']);
        });
    });

    describe('method', function() {
        let TestController = Controller
            .create()
            .mixin(ResourceResponseProcessorMixin);
        let testController;
        let specificClientErrorHandlerResult = { data: 'OK' };

        TestController.ClientError.extendCodes({
            SOME_CLIENT_ERROR: ['ServiceClientError', 'Some client error'],
            SOME_CLIENT_ERROR_2: ['ServiceClientError_2', 'Some other client error'],
        });

        beforeEach(function() {
            TestController.ClientError.SOME_CLIENT_ERROR_2 = sinon
                .stub()
                .returns(specificClientErrorHandlerResult);

            testController = new TestController(createMockParams());
        });

        describe('.setResourceExecutor()', function() {
            let ResourceExecutor = Resource.create();

            it('should set __resource property if inheritor of Resource was passed', function() {
                TestController.setResourceExecutor(ResourceExecutor);
                assert.strictEqual(TestController.__resource, ResourceExecutor);
            });

            it('should return this', function() {
                assert.strictEqual(TestController.setResourceExecutor(ResourceExecutor), TestController);
            });

            it('should throw an error when executor is not inheritor of Resource', function() {
                assert.throwTerror(function() {
                    TestController.setResourceExecutor({});
                }, ResourceResponseProcessorMixin.ResourceResponseProcessorError, 'INVALID_RESOURCE');
            });
        });

        describe('#isClientError()', function() {
            it('should return true when error with code from ClientError was passed', function() {
                let error = { code: 'SOME_CLIENT_ERROR' };

                assert.isTrue(testController.isClientError(error));
            });

            it('should return false when error with unknown code was passed', function() {
                let error = { code: 'ANOTHER_ERROR' };

                assert.isFalse(testController.isClientError(error));
            });

            it('should return false when error without code was passed', function() {
                let error = {};

                assert.isFalse(testController.isClientError(error));
            });
        });

        describe('#processClientError()', function() {
            it('should throw a ClientError with code from passed error', function() {
                let error = { code: 'SOME_CLIENT_ERROR' };

                assert.throwTerror(function() {
                    testController.processClientError(error);
                }, TestController.ClientError, 'SOME_CLIENT_ERROR');
            });

            it('should return result of #processSpecificClientError() if not undefined', function() {
                let error = { code: 'SOME_CLIENT_ERROR_2' };
                let errorProcessingResult;

                testController.processSpecificClientError = sinon.stub().returns(specificClientErrorHandlerResult);

                errorProcessingResult = testController.processClientError(error);

                assert.isTrue(testController.processSpecificClientError.calledWith(error));
                assert.equal(errorProcessingResult, specificClientErrorHandlerResult);
            });
        });

        describe('#processSpecificClientError()', function() {
            it('should pass an error to specific handler (if exists) and return result', function() {
                let error = { code: 'SOME_CLIENT_ERROR_2' };
                let errorProcessingResult;

                TestController.ClientError.SOME_CLIENT_ERROR_2 = sinon
                    .stub()
                    .returns(specificClientErrorHandlerResult);

                errorProcessingResult = testController.processSpecificClientError(error);

                assert.isTrue(TestController.ClientError.SOME_CLIENT_ERROR_2.calledWith(error));
                assert.equal(errorProcessingResult, specificClientErrorHandlerResult);
            });
        });

        describe('#processResponse()', function() {
            let error = { code: 'SOME_CLIENT_ERROR' };

            beforeEach(function() {
                sinon.stub(testController, 'processResponseError');
            });

            it('should process response errors with `processResponseError` method', function() {
                let response = { errors: [error] };

                testController.processResponse(resourceName, response);

                assert.strictEqual(testController.processResponseError.firstCall.args[0], resourceName);
                assert.strictEqual(testController.processResponseError.firstCall.args[1], error);
            });

            it('should process non-array value in errors property the same way', function() {
                let response = { errors: error };

                testController.processResponse(resourceName, response);

                assert.strictEqual(testController.processResponseError.firstCall.args[0], resourceName);
                assert.strictEqual(testController.processResponseError.firstCall.args[1], error);
            });
        });

        describe('#processResponseError', function() {
            it('should check errors with `isClientError` method', function() {
                let error = { code: 'SOME_CLIENT_ERROR' };

                sinon.stub(testController, 'isClientError');
                sinon.stub(testController, 'processUnexpectedError');

                testController.processResponseError(resourceName, error);

                assert.isTrue(testController.isClientError.calledWith(error));
            });

            it('should process client errors with `processClientError` method', function() {
                let error = { code: 'SOME_CLIENT_ERROR' };

                sinon.stub(testController, 'isClientError').returns(true);
                sinon.stub(testController, 'processClientError');

                testController.processResponseError(resourceName, error);

                assert.isTrue(testController.processClientError.calledWith(error));
            });

            it('should process unexpected errors with `processUnexpectedError` method', function() {
                let error = { code: 'SOME_ERROR' };

                sinon.stub(testController, 'isClientError').returns(false);
                sinon.stub(testController, 'processUnexpectedError');

                testController.processResponseError(resourceName, error);

                assert.strictEqual(testController.processUnexpectedError.firstCall.args[0], error);
            });

            it('should call #logUnexpectedResponseError() method if error is unexpected', function() {
                let error = { code: 'SOME_ERROR' };

                sinon.stub(testController, 'isClientError').returns(false);
                sinon.stub(testController, 'logUnexpectedResponseError');
                sinon.stub(testController, 'processUnexpectedError'); // Чтобы ошибка не выбрасывалась

                testController.processResponseError(resourceName, error);

                assert.strictEqual(testController.logUnexpectedResponseError.firstCall.args[0], resourceName);
                assert.strictEqual(testController.logUnexpectedResponseError.firstCall.args[1], error);
            });
        });

        describe('#processUnexpectedError()', function() {
            let spec;

            beforeEach(function() {
                spec = /^Unexpected error in the providers chain in the ProvidersMixin#getData\(\)/gi;
            });

            it('should throw a ProvidersError.ERROR_GETTING_DATA if error is not ProvidersError', function() {
                assert.throwTerror(function() {
                    testController.processUnexpectedError({ code: 'OTHER_ERROR' });
                }, ProvidersError, spec);
            });

            it('should throw a ProvidersError.ERROR_GETTING_DATA if error is ProvidersError', function() {
                assert.throwTerror(function() {
                    testController.processUnexpectedError(
                        ProvidersError.createError(ProvidersError.CODES.UNRESOLVED_PROVIDERS_DEPENDENCY),
                    );
                }, ProvidersError, spec);
            });
        });

        describe('#logUnexpectedResponseError()', function() {
            it('should log an error if it is error from backend', function() {
                assert.logsTerror(ResourceResponseProcessorError, 'UNEXPECTED_ERROR_IN_RESPONSE', function() {
                    testController.logUnexpectedResponseError(resourceName, {});
                });
            });

            it('should not log already logged error', function() {
                let error = Terror.createError('err').log();
                let logger = error.log = sinon.spy();

                testController.logUnexpectedResponseError(resourceName, error);
                assert.strictEqual(logger.callCount, 0);
            });

            it('should log unlogged terror', function() {
                let error = Terror.createError('err');

                assert.logsTerror(ResourceResponseProcessorError, 'UNEXPECTED_ERROR_IN_RESOURCE_CALL', function() {
                    testController.logUnexpectedResponseError(resourceName, error);
                });
            });

            it('should log other errors', function() {
                let error = new Error('123');

                assert.logsTerror(ResourceResponseProcessorError, 'UNEXPECTED_ERROR_IN_RESOURCE_CALL', function() {
                    testController.logUnexpectedResponseError(resourceName, error);
                });
            });
        });

        describe('#callResource()', function() {
            let resourceMock = require('./mocks/resource');
            let resource;

            beforeEach(function() {
                resource = resourceMock();

                TestController.__resource = resource;
            });

            it('should pass all arguments to the resource', function() {
                let resourceName = 'myresource';
                let params = { foo: 'bar' };
                let opts = { timeout: 200 };

                testController.callResource(resourceName, params, opts);

                assert.isTrue(resource.callMethod.calledWith(resourceName, params, opts));
            });

            it('should call processResponse after resource execution', function(done) {
                let processResponse = sinon.spy(testController, 'processResponse');

                testController.callResource()
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(processResponse.calledAfter(resource.callMethod));
                        assert.strictEqual(processResponse.firstCall.args[1], resource.response);
                    })
                    .done(done);
            });

            it('should pass resource response without any processing if it\'s null', function(done) {
                resource.callMethod = sinon.stub().returns(Vow.resolve(null));

                testController.callResource()
                    .then(function(result) {
                        assert.equal(result, null);
                        done();
                    });
            });

            it('should process resource rejection errors with `#processResponseError`', function(done) {
                let resourceError = new Error();
                let processResponseError = sinon.spy(testController, 'processResponseError');

                resource.callMethod = sinon.stub().returns(Vow.reject(resourceError));

                testController.callResource()
                    .always(function() {
                        assert.strictEqual(processResponseError.firstCall.args[1], resourceError);
                        done();
                    });
            });
        });
    });
});
