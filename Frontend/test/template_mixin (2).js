/* global describe, it, beforeEach */

let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');

describe('CoreTemplateMixin', function() {
    let app = require('../app');
    let CoreController = app.Controller;
    let CoreTemplateMixin = app.TemplateMixin;
    let createMockParams = require('./mocks/controller_params');
    let createMockUser = require('./mocks/user');
    let TestController;

    beforeEach(function() {
        TestController = CoreController.create();
    });

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin() properly`', function() {
            assert.canBeMixed(TestController, CoreTemplateMixin, 'CoreTemplateMixin',
                ['constructor', 'prototype', 'super_', 'mixin', '__super', 'create'],
                ['constructor']);
        });
    });

    describe('method', function() {
        let TestController = CoreController
            .create()
            .mixin(CoreTemplateMixin);

        describe('.setConfig()', function() {
            it('should set __templateConfig property', function() {
                let config = { foo: 'bar' };

                TestController.setConfig(config);

                assert.strictEqual(TestController.__templateConfig, config);
            });
        });

        describe('#prepareData()', function() {
            it('should call BEMTemplateMixin#prepareData()', function() {
                let prepareData = sinon.spy(app.NodulesControllers.BEMTemplateMixin.prototype, 'prepareData');
                let data = {};
                let testController = new TestController(createMockParams());

                testController.user = createMockUser();
                testController.prepareData(data);

                assert.isTrue(prepareData.calledOn(testController));
                assert.isTrue(prepareData.calledWith(data));
            });

            it('should extend data', function() {
                let config = { foo: 'bar' };
                let data = {};
                let resultData;
                let testController;

                TestController.setConfig(config);

                testController = new TestController(createMockParams());
                testController.user = createMockUser();
                testController.req.urlHelper = {};

                testController.getCurrentRouteName = sinon.stub().returns(Math.random());
                testController.getPublicParams = sinon.stub().returns({});

                resultData = testController.prepareData(data);

                assert.strictEqual(resultData.url, testController.req.urlHelper);
                assert.strictEqual(resultData.user, testController.user);
                assert.strictEqual(resultData.lang, testController.user.l10n.lang);
                assert.strictEqual(resultData.config, config);
                assert.strictEqual(resultData.BEMTemplateError, TestController.BEMTemplateError);

                assert.strictEqual(resultData.routeName, testController.getCurrentRouteName());
                assert.strictEqual(resultData.routeParams, testController.getParams());
                assert.strictEqual(resultData.routePublicParams, testController.getPublicParams());
            });
        });
    });
});
