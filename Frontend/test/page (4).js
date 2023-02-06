/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    sinon = require('sinon'),
    path = require('path');

describe('CorePage', function() {
    var app = require('../app'),
        CorePage = app.Page,
        Resource = app.Resource,
        nodulesControllers = app.NodulesControllers,
        fakeCookies = { req: { cookies: {}}},
        createMockUser = require('./mocks/user'),
        createMockParams = require('./mocks/controller_params'),
        privPath = path.resolve(__dirname, 'mocks/priv'),
        disableDebugOutput = require('nodules-controllers/test/lib/disable_debug_output');

    it('should be an inheritor of nodules-controllers.Page', function() {
        assert.isTrue(nodulesControllers.Page.isParentOf(CorePage));
    });

    it('should mix CoreController', function() {
        assert.isMixed(CorePage, app.Controller, 'CoreController',
            [ 'name', 'prototype', 'super_', 'create', '__super', '__objexOnMixing', 'action', 'ClientError' ],
            [ 'constructor' ]);
    });

    it('should mix ResourceResponseProcessorMixin', function() {
        assert.isMixed(CorePage, app.ResourceResponseProcessorMixin, 'ResourceResponseProcessorMixin',
            [ 'name', 'prototype' ],
            [ 'constructor', 'processClientError' ]);
    });

    it('should mix CoreProvidersMixin', function() {
        assert.isMixed(CorePage, app.ProvidersMixin, 'CoreProvidersMixin',
            [ 'name', 'prototype' ],
            [ 'constructor', 'getData' ]);
    });

    it('should mix TemplateMixin', function() {
        assert.isMixed(CorePage, app.TemplateMixin, 'CoreTemplateMixin',
            [ 'name', 'prototype', 'super_', 'create', '__super', '__objexOnMixing' ],
            [ 'constructor' ]);
    });

    it('should mix ForceAuthMixin', function() {
        assert.isMixed(CorePage, app.ForceAuthMixin, 'ForceAuthMixin',
            [ 'name', 'prototype', '__objexOnMixing' ],
            [ 'constructor' ]);
    });

    describe('method', function() {
        var TestPage;

        beforeEach(function() {
            TestPage = CorePage.create('TestPage');
        });

        describe('.setEnv()', function() {
            it('should set __templateConfig property if opts.config passed', function() {
                var config = require('./fakes/config'),
                    setConfig = sinon.spy(TestPage, 'setConfig');

                TestPage.setEnv({ config: config });

                assert.isTrue(setConfig.calledWith(config));

                setConfig.restore();
            });

            it('should set __resource property if opts.resource was passed', function() {
                var ResourceExecutor = Resource.create(),
                    setResourceExecutor = sinon.spy(TestPage, 'setResourceExecutor');

                TestPage.setEnv({ resource: ResourceExecutor });

                assert.isTrue(setResourceExecutor.calledWith(ResourceExecutor));

                setResourceExecutor.restore();
            });

            it('should set _templatePriv property if opts.priv and opts.config were passed', function() {
                var setPriv = sinon.spy(TestPage, 'setPriv');

                TestPage.setEnv({
                    config: { debug: true },
                    priv: privPath
                });

                assert.isTrue(setPriv.calledWith(privPath));

                setPriv.restore();
            });
        });

        describe('.action()', function() {
            var actionName = 'test',
                pageEnv = {
                    config: { debug: true },
                    priv: privPath
                };

            beforeEach(function() {
                TestPage.setEnv(pageEnv);

                TestPage.prototype['force-auth'] = false;
            });

            it('should put i-debug block to renderBlocks if config.debug is true', function(done) {
                var testPage,
                    isDebugRender;

                TestPage.action({
                    name: actionName,
                    render: true,
                    blocks: 'b-page',
                    fn: function(data) {
                        if (data.renderBlocks.indexOf('i-debug') !== -1) {
                            isDebugRender = true;
                        }

                        return data;
                    }
                });

                testPage = new TestPage(createMockParams(fakeCookies));
                testPage.user = createMockUser();

                disableDebugOutput(testPage);

                testPage.callAction(actionName)
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(isDebugRender);
                    })
                    .done(done);
            });

            it('shouldn’t put i-debug block to renderBlocks if config.debug is false', function(done) {
                var testPage,
                    isDebugRender;

                TestPage.setConfig({ debug: false });

                TestPage.action({
                    name: actionName,
                    render: true,
                    blocks: 'b-page',
                    fn: function(data) {
                        if (data.renderBlocks.indexOf('i-debug') === -1) {
                            isDebugRender = false;
                        }

                        return data;
                    }
                });

                testPage = new TestPage(createMockParams(fakeCookies));
                testPage.user = createMockUser();

                disableDebugOutput(testPage);

                testPage.callAction(actionName)
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isFalse(isDebugRender);
                    })
                    .done(done);
            });

            it('callback should return data', function() {
                var testPage,
                    pageAction,
                    actionResult,
                    superAction = CorePage.__super.action,
                    data = { key: 'value' };

                CorePage.__super.action = function(action) {
                    pageAction = action.fn;
                };

                TestPage.action({
                    name: actionName,
                    render: false,
                    blocks: 'b-page'
                });

                testPage = new TestPage(createMockParams(fakeCookies));

                actionResult = pageAction.call(testPage, data);
                assert.isTrue(actionResult === data);
                CorePage.__super.action = superAction;
            });
        });

        describe('#getData()', function() {
            var testPage;

            beforeEach(function() {
                testPage = new TestPage(createMockParams(fakeCookies));
            });

            it('should call CoreProvidersMixin#getData() with passed blocks and return its result', function(done) {
                var blocks = [ 'b-page', 'some-block' ],
                    getData = sinon.spy(app.ProvidersMixin.prototype, 'getData');

                testPage.getData(blocks)
                    .always(function(promise) {
                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(getData.calledWith(blocks));
                        assert.strictEqual(getData.returnValues[0].valueOf(), promise.valueOf());

                        getData.restore();
                    })
                    .done(done);
            });

            it('should set data.errors and data.notifications', function(done) {
                var notifications = {
                        local: 'some_notification'
                    },
                    getNotificationsData = sinon.stub(testPage.notifications, 'getData').returns(notifications);

                testPage._clientErrors = [ 'OOOPS SOME ERROR' ];

                testPage.getData()
                    .always(function(promise) {
                        var data;

                        assert.isTrue(promise.isFulfilled());
                        assert.isTrue(getNotificationsData.calledOnce);

                        data = promise.valueOf();
                        assert.strictEqual(data.notifications, notifications);
                        assert.strictEqual(data.errors, testPage._clientErrors);
                    })
                    .done(done);
            });
        });

        describe('#processClientError()', function() {
            var testPage;

            beforeEach(function() {
                testPage = new TestPage(createMockParams(fakeCookies));
            });

            it('should push client error to _clientErrors', function() {
                var error = new Error(),
                    code = 'WRONG_USER_NAME',
                    paramName = 'username',
                    paramValue = 'wrong_user_name',
                    params = {
                        param: paramName,
                        value: paramValue
                    };

                error.code = code;
                error.params = params;

                testPage.processClientError(error);

                assert.isArray(testPage._clientErrors[paramName]);
                assert.include(testPage._clientErrors[paramName], {
                    code: code,
                    params: params
                });
            });
        });

        describe('#onGetDataError()', function() {
            var testPage;

            beforeEach(function() {
                testPage = new TestPage(createMockParams(fakeCookies));
            });

            it('should throw redirect error to next handler', function() {
                var error = TestPage.CoreControllerError
                    .createError(TestPage.CoreControllerError.CODES.REDIRECT)
                    .bind({ location: '/' });

                assert.throwTerror(function() {
                    testPage.onGetDataError(error);
                }, TestPage.CoreControllerError, 'REDIRECT');
            });

            it('should throw DATA_NOT_FOUND erorr to the next handler', function() {
                var error = TestPage.CoreControllerError
                    .createError(TestPage.CoreControllerError.CODES.DATA_NOT_FOUND);

                assert.throwTerror(function() {
                    testPage.onGetDataError(error);
                }, TestPage.CoreControllerError, 'DATA_NOT_FOUND');
            });

            /* jshint maxlen:false */
            it('should set 500 status code and return data for notification page if unexpected error occurred', function() {
                var error = new Error(),
                    data,
                    expectedPageData,
                    addKey = sinon.spy(testPage.notifications, 'addKey');

                error.codeName = 'SOME_ERROR';

                expectedPageData = {
                    pageType: 'notification',
                    notifications: {
                        local: [{
                            type: 'error',
                            key: error.codeName,
                            params: {},
                            unique: false,
                            escape: true
                        }]
                    }
                };

                data = testPage.onGetDataError(error);

                assert.deepEqual(data, expectedPageData);
                assert.strictEqual(testPage.res.statusCode, 500);

                assert.strictEqual(addKey.getCall(0).args[0], 'SOME_ERROR');
                assert.deepEqual(addKey.getCall(0).args[1], {
                    type: 'error',
                    place: 'local'
                });
            });
        });

        describe('#saveNotifications()', function() {
            var testPage;

            beforeEach(function() {
                testPage = new TestPage(createMockParams(fakeCookies));
            });

            it('should call notifications.save', function() {
                var save = sinon.spy(testPage.notifications, 'save');

                testPage.saveNotifications();

                assert.isTrue(save.calledOnce);
            });

            it('should throw error to next handler', function() {
                var error = TestPage.CoreControllerError.createError();

                assert.throwTerror(function() {
                    testPage.saveNotifications(error);
                }, TestPage.CoreControllerError);
            });

            it('should return actionResult', function() {
                var actionResult = {},
                    methodResult = testPage.saveNotifications(null, actionResult);

                assert.strictEqual(actionResult, methodResult);
            });

            it('should be called by the last function in after chain', function() {
                var after = TestPage.prototype._afterChain,
                    saveNotifications = sinon.stub(testPage, 'saveNotifications'),
                    error = new Error(),
                    actionResult = {};

                after[after.length - 1].apply(testPage, [ error, actionResult ]);
                assert.isTrue(saveNotifications.calledWith(error, actionResult));
            });
        });
    });
});
