/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    sinon = require('sinon');

describe('CoreTemplateMixin', function() {
    var app = require('../app'),
        CoreController = app.Controller,
        CoreTemplateMixin = app.TemplateMixin,
        createMockParams = require('./mocks/controller_params'),
        createMockUser = require('./mocks/user'),
        TestController;

    beforeEach(function() {
        TestController = CoreController.create();
    });

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin() properly`', function() {
            assert.canBeMixed(TestController, CoreTemplateMixin, 'CoreTemplateMixin',
                [ 'constructor', 'prototype', 'super_', 'mixin', '__super', 'create' ],
                [ 'constructor' ]);
        });
    });

    describe('method', function() {
        var TestController = CoreController
                .create()
                .mixin(CoreTemplateMixin);

        describe('.setConfig()', function() {

            it('should set __templateConfig property', function() {
                var config = { foo: 'bar' };

                TestController.setConfig(config);

                assert.strictEqual(TestController.__templateConfig, config);
            });
        });

        describe('#prepareData()', function() {
            it('should call BEMTemplateMixin#prepareData()', function() {
                var prepareData = sinon.spy(app.NodulesControllers.BEMTemplateMixin.prototype, 'prepareData'),
                    data = {},
                    testController = new TestController(createMockParams());

                testController.user = createMockUser();
                testController.prepareData(data);

                assert.isTrue(prepareData.calledOn(testController));
                assert.isTrue(prepareData.calledWith(data));
            });

            it('should extend data', function() {
                var config = { foo: 'bar' },
                    data = {},
                    resultData,
                    testController;

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
