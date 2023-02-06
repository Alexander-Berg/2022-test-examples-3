/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    util = require('nodules-libs').util;

describe('CoreProvidersMixin', function() {
    var app = require('../app'),
        CoreController = app.Controller,
        CoreProvidersMixin = app.ProvidersMixin,
        nodulesControllers = app.NodulesControllers,
        createMockParams = require('./mocks/controller_params'),
        TestController;

    beforeEach(function() {
        TestController = CoreController.create();
    });

    describe('Mixin', function() {

        it('should mix ProvidersMixin from nodules-controllers', function() {
            TestController.mixin(CoreProvidersMixin);

            assert.isMixed(TestController, nodulesControllers.ProvidersMixin, 'ProvidersMixin',
                [ 'constructor', 'prototype', 'super_', 'mixin', '__super', 'name' ],
                [ 'constructor' ]);
        });

        it('should be mixed using `CoreController.mixin()` properly', function() {
            assert.canBeMixed(TestController, CoreProvidersMixin, 'CoreProvidersMixin',
                [ 'constructor', 'prototype' ],
                [ 'constructor' ]);
        });

    });

    describe('method', function() {
        var TestController = CoreController
                .create()
                .mixin(CoreProvidersMixin),
            testController;

        CoreProvidersMixin.CoreProvidersError.setLogger(function() {});

        beforeEach(function() {
            testController = new TestController(createMockParams());
        });

        describe('#getMandatoryProperty()', function() {
            var response = {
                data: {
                    str: 'some value',
                    langs: [ 'ru', 'uk', 'en', 'be', 'kk' ],
                    data: { foo: 'bar' },
                    bool: true,
                    num: 1234,
                    zero: null
                }
            };

            function assertExpectedParamValue(value, prop, type) {
                assert.strictEqual(testController.getMandatoryProperty(response, prop, type), value);
            }

            function assertUnexpectedParamValue(prop, type) {
                assert.throwTerror(function() {
                    testController.getMandatoryProperty(response, prop, type);
                }, CoreProvidersMixin.CoreProvidersError, 'UNEXPECTED_PARAM_VALUE');
            }

            function assertSpecifiedType(prop, type) {
                var value = response.data[prop];

                assertExpectedParamValue(value, 'data.' + prop, type);
                Object.keys(response.data)
                    .filter(function(item) { return item !== prop; })
                    .forEach(function(item) {
                        assertUnexpectedParamValue('data.' + item, type);
                    });
            }

            it('should return property value if property exists', function() {
                assertExpectedParamValue(response.data.data, 'data.data');
            });

            it('should throw an error if property does not exists', function() {
                assertUnexpectedParamValue('data.otherprop');
            });

            it('should support [type=array] param', function() {
                assertSpecifiedType('langs', 'array');
            });

            it('should support [type=boolean] param', function() {
                assertSpecifiedType('bool', 'boolean');
            });

            it('should support [type=number] param', function() {
                assertSpecifiedType('num', 'number');
            });

            it('should support [type=RegExp] param', function() {
                assertSpecifiedType('str', /[a-z]/);
            });
        });

        describe('.arrayBlocks()', function() {
            it('should init Controller.blocks with an empty array if `blocks` are missing', function() {
                TestController.arrayBlocks();
                assert.deepEqual([], TestController.blocks);
            });

            it('should not change Controller.blocks if `blocks` are an array', function() {
                var beforeBlocks = [{ block: 'myblock', data: 'somedata' }];

                TestController.blocks = beforeBlocks.slice();
                TestController.arrayBlocks();

                assert.deepEqual(beforeBlocks, TestController.blocks);
            });

            it('should wrap Controller.blocks with an array if `blocks` is an object', function() {
                var beforeBlocks = {
                    block: 'block-wrapper',
                    content: [
                        { block: 'myblock', data: 'somedata' }
                    ]
                };

                TestController.blocks = util.extend(true, {}, beforeBlocks);
                TestController.arrayBlocks();

                assert.deepEqual([ beforeBlocks ], TestController.blocks);
            });
        });
    });
});
