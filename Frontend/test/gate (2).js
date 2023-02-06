/* global describe, it, beforeEach, afterEach */

let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');
let util = require('../../nodules-libs').util;

describe('CoreGate', function() {
    let app = require('../app');
    let CoreGate = app.Gate;
    let nodulesControllers = app.NodulesControllers;
    let Resource = app.Resource;
    let createMockUser = require('./mocks/user');
    let createMockParams = require('./mocks/controller_params');
    let disableDebugOutput = require('../../nodules-controllers/test/lib/disable_debug_output');

    CoreGate.GateError.setLogger(sinon.stub());

    it('should be an inheritor of nodules-controllers.Gate ' +
        'with mixed CoreController, CoreProvidersMixin and ResourceResponseProcessorMixin', function() {
        assert.isTrue(nodulesControllers.Gate.isParentOf(CoreGate));

        assert.isMixed(CoreGate, app.Controller, 'CoreController',
            ['name', 'prototype', 'super_', 'create', '__super', 'action', 'ClientError'],
            ['constructor']);

        assert.isMixed(CoreGate, app.ProvidersMixin, 'CoreProvidersMixin',
            ['name', 'prototype'],
            ['constructor']);

        assert.isMixed(CoreGate, app.ResourceResponseProcessorMixin, 'ResourceResponseProcessorMixin',
            ['name', 'prototype'],
            ['constructor', 'processUnexpectedError']);
    });

    it('should handle redirect error and push redirect task to the response', function(done) {
        let TestGate = CoreGate.create('TestGate');
        let testGate;
        let actionName = 'test';
        let redirectUrl = 'redir';
        let pushTask;

        TestGate.dataProviderDecl('redirect', function() {
            this.throwRedirectError(redirectUrl);
        });

        TestGate.action({
            name: actionName,
            signed: false,
        });

        testGate = new TestGate(createMockParams());

        disableDebugOutput(testGate);

        pushTask = sinon.spy(testGate, 'pushTask');

        testGate.callAction(actionName)
            .always(function() {
                assert.isTrue(pushTask.called);
                assert.isTrue(pushTask.calledWith('redirect', { location: redirectUrl }));
            })
            .done(done);
    });

    it('should normalize body.params', function() {
        let gate;
        let params = createMockParams({
            params: {
                param1: 'true',
            },
            req: {
                method: 'POST',
                body: {
                    param2: true,
                    params: {
                        param3: 10,
                    },
                },
            },
        });

        gate = new CoreGate(params);

        assert.deepEqual(gate.getParams(), { param1: 'true', param3: 10 });
    });

    describe('method', function() {
        let TestGate;

        beforeEach(function() {
            TestGate = CoreGate.create('TestGate');
        });

        describe('.setEnv()', function() {
            it('should set __resource property if correct opts.resource was passed', function() {
                let ResourceExecutor = Resource.create();
                let setResourceExecutor = sinon.spy(TestGate, 'setResourceExecutor');

                TestGate.setEnv({ resource: ResourceExecutor });

                assert.isTrue(setResourceExecutor.calledWith(ResourceExecutor));

                setResourceExecutor.restore();
            });

            it('should return itself', function() {
                assert.strictEqual(TestGate.setEnv({}), TestGate);
            });
        });

        describe('.action()', function() {
            it('should get data for blocks before execute the action', function(done) {
                let actionName = 'test';
                let testGate;
                let providerCallback = sinon.spy();
                let actionCallback = sinon.spy(function(data) { return data });

                TestGate.dataProviderDecl('b-block', providerCallback);

                TestGate.action({
                    name: actionName,
                    signed: false,
                    fn: actionCallback,
                });

                testGate = new TestGate(createMockParams());

                disableDebugOutput(testGate);

                testGate.callAction(actionName)
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(providerCallback.calledOnce);
                        assert.isTrue(actionCallback.calledAfter(providerCallback));
                    })
                    .done(done);
            });
        });

        describe('.normalizeAction', function() {
            it('should create action declaration from a string with an action name', function() {
                let actionName = 'someAction';
                let normalizedAction = TestGate.normalizeAction(actionName);

                assert.deepEqual(normalizedAction, {
                    name: actionName,
                    blocks: actionName,
                });
            });

            it('should extend action declaration with `blocks` property equal to action name', function() {
                let actionName = 'someAction';
                let actionDeclaration = {
                    name: actionName,
                };
                let normalizedAction = TestGate.normalizeAction(actionDeclaration);

                assert.equal(normalizedAction.blocks, actionName);
            });
        });

        describe('.singleProviderAction', function() {
            let actionName = 'someAction';

            beforeEach(function() {
                sinon.spy(TestGate, 'action');
                sinon.spy(TestGate, 'singleProviderAction');
                sinon.spy(TestGate, 'normalizeAction');
                sinon.spy(TestGate, 'arrayBlocks');
                sinon.spy(TestGate, 'dataProviderDecl');
            });

            afterEach(function() {
                TestGate.action.restore();
                TestGate.singleProviderAction.restore();
                TestGate.normalizeAction.restore();
                TestGate.arrayBlocks.restore();
                TestGate.dataProviderDecl.restore();
            });

            it('should normalize action', function() {
                TestGate.singleProviderAction(actionName);
                assert.isTrue(TestGate.normalizeAction.calledWithExactly(actionName));
            });

            it('should array gate blocks', function() {
                TestGate.singleProviderAction(actionName);
                assert.isTrue(TestGate.arrayBlocks.calledOnce);
            });

            it('should push to blocks a new one with the same `data`, `block` and `actions` props', function() {
                TestGate.singleProviderAction(actionName);

                assert.include(TestGate.blocks, {
                    data: actionName,
                    block: actionName,
                    actions: actionName,
                });
            });

            it('should not declare a dataprovider if a function for the one is not passed', function() {
                TestGate.singleProviderAction(actionName);

                assert.isFalse(TestGate.dataProviderDecl.called);
            });

            it('should declare a dataprovider if a function for the one is passed', function() {
                let dataProvider = function() {};

                TestGate.singleProviderAction(actionName, dataProvider);

                assert.isTrue(TestGate.dataProviderDecl.calledWithExactly(actionName, dataProvider));
            });

            it('should call CoreGate.action with normalizedAction', function() {
                let normalizedAction = TestGate.normalizeAction(actionName);

                TestGate.singleProviderAction(actionName);

                assert.deepEqual(normalizedAction, TestGate.action.getCall(0).args[0]);
            });

            it('should return CoreGate inheritor', function() {
                let singleProviderActionRetValue = TestGate.singleProviderAction(actionName);
                assert.equal(singleProviderActionRetValue, TestGate);
            });
        });

        describe('#validateSignature()', function() {
            it('should call user.auth.checkCRC method', function() {
                let coreGate = new CoreGate(createMockParams());
                let crc = Math.floor(Math.random() * 100000);

                coreGate.user = createMockUser();
                coreGate.validateSignature(crc);

                assert.isTrue(coreGate.user.auth.checkCRC.calledOnce);
                assert.isTrue(coreGate.user.auth.checkCRC.calledWith(crc));
            });
        });

        describe('#processUnexpectedError', function() {
            it('should always throw CoreGate.GateError.UNEXPECTED_ERROR', function() {
                let coreGate = new CoreGate(createMockParams());

                assert.throw(coreGate.processUnexpectedError, CoreGate.GateError, /Unexpected error/);
            });
        });

        describe('#serializeError()', function() {
            it('should take `code`, `codeName`, `message` and `data.customData` fields from error object', function() {
                let coreGateError = {
                    code: 'SOME_SPECIAL_CODE',
                    codeName: 'SOME_SPECIAL_CODENAME',
                    message: 'AND_MESSAGE',
                    data: {
                        customData: {
                            SOME: 'DATA',
                        },
                    },
                };
                let serializationResult = CoreGate.prototype.serializeError(coreGateError);
                let expectedDeserializedResult = util.extend({}, coreGateError, {
                    data: coreGateError.data.customData,
                });
                let actualDeserializedResult;

                assert.doesNotThrow(function() {
                    actualDeserializedResult = JSON.parse(serializationResult);
                });

                assert.deepEqual(expectedDeserializedResult, actualDeserializedResult);
            });
        });

        describe('#processActionError()', function() {
            let coreGate;

            function bindTerrorLog(TerrorClass) {
                ['bind', 'log'].forEach(function(method) {
                    TerrorClass.prototype[method] = sinon.spy(TerrorClass.prototype[method]);
                });
            }

            beforeEach(function() {
                coreGate = new CoreGate(createMockParams());
            });

            it('should return original error for instance coreGate.ClientError', function() {
                let ClientError = coreGate.constructor.ClientError;
                let clientError = ClientError.createError(null, 'test message');

                bindTerrorLog(ClientError);

                assert.strictEqual(clientError, coreGate.processActionError(clientError),
                    '#processActionError result are not equal gateError');

                assert.isTrue(ClientError.prototype.bind.calledOnce);
                assert.isTrue(ClientError.prototype.log.calledOnce);
            });

            it('should return instance of GateError#UNEXPECTED_ERROR if object is simple', function() {
                let gateError;
                let GateError = coreGate.constructor.GateError;
                let originalError = new Error('SIMPLE_ERROR_CODE');

                bindTerrorLog(GateError);

                gateError = coreGate.processActionError(originalError);

                assert.instanceOf(gateError, GateError);
                assert.equal(gateError.codeName, 'UNEXPECTED_ERROR');
                assert.equal(gateError.originalError, originalError);

                assert.isTrue(GateError.prototype.bind.calledOnce);
                assert.isTrue(GateError.prototype.log.calledOnce);
            });

            it('should return original error for instance coreGate.GateError', function() {
                let GateError = coreGate.constructor.GateError;
                let originalGateError = GateError.createError(null, 'test message');

                bindTerrorLog(GateError);

                assert.strictEqual(coreGate.processActionError(originalGateError), originalGateError);

                assert.isTrue(GateError.prototype.bind.calledOnce);
                assert.isTrue(GateError.prototype.log.calledOnce);
            });
        });
    });
});
