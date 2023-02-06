/* global describe, it, beforeEach */

var assert = require('chai')
        .use(require('chai-nodules-helpers'))
        .assert,
    sinon = require('sinon'),
    qs = require('querystring'),
    util = require('nodules-libs').util,
    Url = require('nodules-libs').Url;

describe('CorePaginatorMixin', function() {
    var app = require('../app'),
        CoreController = app.Controller,
        CorePaginatorMixin = app.PaginatorMixin,
        createMockParams = require('./mocks/controller_params'),
        TestController;

    beforeEach(function() {
        TestController = CoreController.create();
    });

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin() properly`', function() {
            assert.canBeMixed(TestController, CorePaginatorMixin, 'CorePaginatorMixin',
                [ 'constructor', 'prototype', 'super_', 'mixin', '__super', 'create', 'name' ],
                [ 'constructor' ]);
        });
    });

    describe('method', function() {
        var TestController;

        beforeEach(function() {
            TestController = CoreController
                .create()
                .mixin(CorePaginatorMixin);
        });

        describe('#getCurrentPageNumber()', function() {
            it('should call getNumericParam for param PAGE_PARAM_NAME with default value equals 1', function() {
                var testController,
                    getNumericParam,
                    pageParam = 'foobar',
                    params = {};

                params[pageParam] = 100500;

                TestController.PAGE_PARAM_NAME = pageParam;
                testController = new TestController(createMockParams({ params: params }));

                getNumericParam = sinon.spy(testController, 'getNumericParam');

                testController.getCurrentPageNumber();

                assert.isTrue(getNumericParam.calledWith(pageParam, 1));
            });

            it('should return current page', function() {
                var page = Math.ceil(Math.random() * 100),
                    params = { page: page },
                    testController = new TestController(createMockParams({ params: params }));

                assert.strictEqual(testController.getCurrentPageNumber(), page);
            });

            it('should return 1 if page is undefined', function() {
                var testController = new TestController(createMockParams());

                assert.strictEqual(testController.getCurrentPageNumber(), 1);
            });
        });

        describe('#getPageSize()', function() {
            it('should return DEFAULT_PAGE_SIZE', function() {
                var testController;

                TestController.DEFAULT_PAGE_SIZE = Math.ceil(Math.random() * 100);
                testController = new TestController(createMockParams());

                assert.strictEqual(testController.getPageSize(), TestController.DEFAULT_PAGE_SIZE);
            });
        });

        describe('#getPagerParams()', function() {
            it('should return params for pager properly', function() {
                var pageSize = Math.ceil(Math.random() * 10),
                    pageParam = 'fasdfasdf',
                    currentPage = Math.ceil(Math.random() * 100),
                    totalCount = pageSize * Math.ceil(Math.random() * 10) + Math.ceil(Math.random() * 10),
                    params = {},
                    testController;

                TestController.DEFAULT_PAGE_SIZE = pageSize;
                TestController.PAGE_PARAM_NAME = pageParam;

                params[pageParam] = currentPage;

                testController = new TestController(createMockParams({ params: params }));

                assert.deepEqual(testController.getPagerParams(totalCount), {
                    pageParam: pageParam,
                    page: currentPage,
                    perpageCount: pageSize,
                    totalCount: totalCount
                });
            });
        });

        describe('#checkPageNumberOverflow()', function() {
            /* jshint maxlen:false */
            it('should call processPageNumberOverflow when current page is greater than last available page', function() {
                var lastPage = 1 + Math.ceil(Math.random() * 100),
                    totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE,
                    currentPage = lastPage + 1,
                    processPageNumberOverflow,
                    testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage }
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isTrue(processPageNumberOverflow.calledWith(lastPage));
            });

            it('shouldn’t call processPageNumberOverflow when current page is less than available page', function() {
                var lastPage = 1 + Math.ceil(Math.random() * 100),
                    totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE,
                    currentPage = lastPage - 1,
                    processPageNumberOverflow,
                    testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage }
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isFalse(processPageNumberOverflow.called);
            });

            it('shouldn’t call processPageNumberOverflow when current page equals to available page', function() {
                var lastPage = 1 + Math.ceil(Math.random() * 100),
                    totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE,
                    currentPage = lastPage,
                    processPageNumberOverflow,
                    testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage }
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isFalse(processPageNumberOverflow.called);
            });

            it('should set last available page to 1 if total count is 0', function() {
                var processPageNumberOverflow,
                    testController;

                testController = new TestController(createMockParams({
                    params: { page: 5 }
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(0);

                assert.isTrue(processPageNumberOverflow.calledWith(1));
            });
        });

        describe('#processPageNumberOverflow()', function() {
            var params = {
                    foo: 1,
                    bar: 2,
                    baz: 3,
                    page: 6
                },
                routeName = 'paginator',
                testController;

            beforeEach(function() {
                testController = new TestController(createMockParams({
                    req: {
                        urlHelper: new Url({
                            url: 'http://ya.ru/paginator?' + qs.stringify(params),
                            routers: {
                                desktop: require('./mocks/susanin-for-make-url')
                            }
                        })
                    }
                }));

                testController.getRoute = sinon.stub().returns({
                    getName: sinon.stub().returns(routeName)
                });

                testController.getParams = sinon.stub().returns(util.extend({}, params));
            });

            it('should call throwRedirectError', function() {
                var throwRedirectError = sinon.stub(testController, 'throwRedirectError');

                testController.processPageNumberOverflow(2);
                assert.isTrue(throwRedirectError.calledOnce);
            });

            it('should redirect to current url with page param equals to last available page', function() {
                var link = sinon.stub(),
                    pageType = 'paginator',
                    lastPage = Math.floor(Math.random() * 100),
                    linkArgs;

                sinon.stub(testController, 'throwRedirectError');
                testController.req.urlHelper = { link: link };
                testController.type = pageType;

                testController.processPageNumberOverflow(lastPage);

                linkArgs = link.args[0];

                assert.strictEqual(linkArgs[0], 'paginator');
                assert.deepEqual(linkArgs[1], util.extend({}, params, {
                    controller: pageType,
                    page: lastPage
                }));
            });
        });
    });
});
