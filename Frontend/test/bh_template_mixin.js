/* global describe, it, beforeEach */

let path = require('path');
let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');

describe('CoreBhTemplateMixin', function() {
    let app = require('../app');
    let CoreController = app.Controller;
    let CoreBhTemplateMixin = app.BhTemplateMixin;
    let createMockParams = require('./mocks/controller_params');
    let createMockUser = require('./mocks/user');
    let bhBundlePath = path.resolve(__dirname, 'mocks/bh');

    describe('Mixin', function() {
        it('should be mixed using `CoreBhController.mixin() properly`', function() {
            let TestController = CoreController.create();

            assert.canBeMixed(TestController, CoreBhTemplateMixin, 'CoreBhTemplateMixin',
                ['constructor', 'prototype', 'super_', 'mixin', '__super', 'create'],
                ['constructor']);
        });
    });

    describe('method', function() {
        let TestController;

        beforeEach(function() {
            TestController = CoreController.create().mixin(CoreBhTemplateMixin);
        });

        describe('.setBhBundle()', function() {
            it('should set #_bhBundle', function() {
                TestController.setBhBundle(bhBundlePath);

                assert.strictEqual(TestController.prototype._bhBundle, require(bhBundlePath));
            });

            it('should throw BH_EXECUTION_FAILED error if require failed', function() {
                TestController.BhTemplateError.setLogger(function() {});

                assert.throwTerror(function() {
                    TestController.setBhBundle('aaa');
                }, CoreBhTemplateMixin.BhTemplateError, 'BH_REQUIRE_FAILED');
            });
        });

        describe('#setBhBundle()', function() {
            let testController;

            beforeEach(function() {
                testController = new TestController(createMockParams());
            });

            it('should set _bhBundle', function() {
                testController.setBhBundle(bhBundlePath);

                assert.strictEqual(testController._bhBundle, require(bhBundlePath));
            });

            it('should throw BH_EXECUTION_FAILED error if require failed', function() {
                TestController.BhTemplateError.setLogger(function() {});

                assert.throwTerror(function() {
                    testController.setBhBundle('aaa');
                }, CoreBhTemplateMixin.BhTemplateError, 'BH_REQUIRE_FAILED');
            });
        });

        describe('#getHTML()', function() {
            let testController;
            let bundle = require(bhBundlePath);

            beforeEach(function() {
                testController = new TestController(createMockParams());
                testController._bhBundle = bundle();
                testController.user = createMockUser();
            });

            it('should return html of block', function() {
                let html = testController.getHTML('lalala', {});

                assert.strictEqual(html, testController._bhBundle.apply.returnValues[0]);
            });

            it('should setData before applying templates', function() {
                testController.getHTML('lalala', {});

                assert.isTrue(testController._bhBundle.apply.calledAfter(testController._bhBundle.setData));
            });

            it('should set prepared data', function() {
                let data = {};
                let preparedData;

                testController.prepareData = sinon.spy(testController, 'prepareData');
                testController.getHTML('lalala', data);

                preparedData = testController.prepareData.returnValues[0];

                assert.isTrue(testController.prepareData.calledWith(data));
                assert.isTrue(testController._bhBundle.setData.calledWith(preparedData));
            });

            it('should throw an error if applying failed', function() {
                assert.logsTerror(CoreBhTemplateMixin.BhTemplateError, 'BH_EXECUTION_FAILED', function() {
                    testController.getHTML('error', {});
                });
            });

            it('should additionally render iDataBlock if block being templated is in the according list', function() {
                let html;
                let withData = 'templated-block';

                testController.blocksWithIData = [withData];
                testController.iDataBlock = 'i-data-block';

                html = testController.getHTML(withData, {});

                assert.strictEqual(
                    html,
                    [
                        testController._bhBundle.apply({ block: withData }),
                        testController._bhBundle.apply({ block: testController.iDataBlock }),
                    ].join(''),
                );
            });
        });
    });
});
