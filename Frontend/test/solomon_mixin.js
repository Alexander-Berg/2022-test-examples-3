/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    sinon = require('sinon');

describe('SolomonMixin', function() {
    var app = require('../app'),
        CoreController = app.Controller,
        CoreSolomonMixin = app.SolomonMixin,
        Solomon = require('../app/lib/solomon'),
        solomonInstance = new Solomon({}),
        createMockParams = require('./mocks/controller_params');

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin() properly`', function() {
            var TestController = CoreController.create();

            assert.canBeMixed(TestController, CoreSolomonMixin, 'CoreSolomonMixin',
                [ 'constructor', 'prototype', 'super_', 'mixin', 'name', '__super', 'create' ],
                [ 'constructor' ]);
        });

        it('should add afterActionSolomon function to after chain', function() {
            var TestController = CoreController.create();
            var after = sinon.spy(TestController, 'after');

            TestController.mixin(CoreSolomonMixin);

            assert.deepEqual(after.getCall(0).args[1], { reverse: true });

            var testController = new TestController(createMockParams());
            var afterActionSolomon = sinon.spy(testController, 'afterActionSolomon');
            var afterChain = TestController.prototype._afterChain;
            var afterArgs = [ {}, 'ololo' ];

            afterChain[0].apply(testController, afterArgs);

            assert.strictEqual(afterActionSolomon.getCall(0).args[0], afterArgs[0]);
            assert.strictEqual(afterActionSolomon.getCall(0).args[1], afterArgs[1]);
        });

        it('should add #saveResourceErrorToSolomon() to #processUnexpectedError()', function() {
            var TestController = CoreController.create();

            var processUnexpectedError = TestController.prototype.processUnexpectedError = sinon.stub();

            TestController.mixin(CoreSolomonMixin);

            var testController = new TestController(createMockParams());
            var saveResourceErrorToSolomon = sinon.spy(testController, 'saveResourceErrorToSolomon');
            var error = {};

            testController.processUnexpectedError(error);

            assert.strictEqual(processUnexpectedError.getCall(0).args[0], error);
            assert.strictEqual(saveResourceErrorToSolomon.getCall(0).args[0], error);
        });
    });

    describe('method', function() {
        var TestController, testController;

        beforeEach(function() {
            TestController = CoreController
                .create()
                .mixin(CoreSolomonMixin);

            TestController.CONTROLLER_TYPE = 'gate';

            testController = new TestController(createMockParams());
            testController.req.solomon = solomonInstance;
        });

        describe('#afterActionSolomon()', function() {

            it('should send sensor to solomon if 5xx error occurred', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor');

                testController.type = 'indexing';
                testController.shouldSend5xxToSolomon = true;

                testController.afterActionSolomon();

                assert.deepEqual(saveSensor.getCall(0).args[0], {
                    name: '5xx_gate_indexing',
                    value: 1
                });

                saveSensor.restore();
            });

            it('should not send sensor to solomon if no 5xx error occured', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor');

                testController.type = 'indexing';
                testController.shouldSend5xxToSolomon = false;

                testController.afterActionSolomon();

                assert.strictEqual(saveSensor.callCount, 0);

                saveSensor.restore();
            });

            it('should return last argument', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor'),
                    foo = {},
                    bar = {},
                    baz = {};

                testController.shouldSend5xxToSolomon = true;

                var result = testController.afterActionSolomon(foo, bar, baz);

                assert.strictEqual(result, baz);

                saveSensor.restore();
            });

        });

        describe('#saveResourceErrorToSolomon', function() {

            it('should send resource error sensor to solomon', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor');

                testController.saveResourceErrorToSolomon({
                    resource: 'sites/list'
                });

                assert.deepEqual(saveSensor.getCall(0).args[0], {
                    name: 'resource_error_sites_list',
                    value: 1
                });

                saveSensor.restore();
            });

            it('should not send resource error sensor to solomon if the error has no resource', function() {
                var saveSensor = sinon.spy(solomonInstance, 'saveSensor');

                testController.saveResourceErrorToSolomon({});

                assert.strictEqual(saveSensor.callCount, 0);

                saveSensor.restore();
            });

        });
    });
});
