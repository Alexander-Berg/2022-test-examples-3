/* global describe, it, beforeEach */

let qs = require('querystring');
let assert = require('chai')
    .use(require('chai-nodules-helpers'))
    .assert;
let sinon = require('sinon');
let util = require('../../nodules-libs').util;
let Url = require('../../nodules-libs').Url;

describe('CorePaginatorMixin', function() {
    let app = require('../app');
    let CoreController = app.Controller;
    let CorePaginatorMixin = app.PaginatorMixin;
    let createMockParams = require('./mocks/controller_params');
    let TestController;

    beforeEach(function() {
        TestController = CoreController.create();
    });

    describe('Mixin', function() {
        it('should be mixed using `CoreController.mixin() properly`', function() {
            assert.canBeMixed(TestController, CorePaginatorMixin, 'CorePaginatorMixin',
                ['constructor', 'prototype', 'super_', 'mixin', '__super', 'create', 'name'],
                ['constructor']);
        });
    });

    describe('method', function() {
        let TestController;

        beforeEach(function() {
            TestController = CoreController
                .create()
                .mixin(CorePaginatorMixin);
        });

        describe('#getCurrentPageNumber()', function() {
            it('should call getNumericParam for param PAGE_PARAM_NAME with default value equals 1', function() {
                let testController;
                let getNumericParam;
                let pageParam = 'foobar';
                let params = {};

                params[pageParam] = 100500;

                TestController.PAGE_PARAM_NAME = pageParam;
                testController = new TestController(createMockParams({ params: params }));

                getNumericParam = sinon.spy(testController, 'getNumericParam');

                testController.getCurrentPageNumber();

                assert.isTrue(getNumericParam.calledWith(pageParam, 1));
            });

            it('should return current page', function() {
                let page = Math.ceil(Math.random() * 100);
                let params = { page: page };
                let testController = new TestController(createMockParams({ params: params }));

                assert.strictEqual(testController.getCurrentPageNumber(), page);
            });

            it('should return 1 if page is undefined', function() {
                let testController = new TestController(createMockParams());

                assert.strictEqual(testController.getCurrentPageNumber(), 1);
            });
        });

        describe('#getPageSize()', function() {
            it('should return DEFAULT_PAGE_SIZE', function() {
                let testController;

                TestController.DEFAULT_PAGE_SIZE = Math.ceil(Math.random() * 100);
                testController = new TestController(createMockParams());

                assert.strictEqual(testController.getPageSize(), TestController.DEFAULT_PAGE_SIZE);
            });
        });

        describe('#getPagerParams()', function() {
            it('should return params for pager properly', function() {
                let pageSize = Math.ceil(Math.random() * 10);
                let pageParam = 'fasdfasdf';
                let currentPage = Math.ceil(Math.random() * 100);
                let totalCount = pageSize * Math.ceil(Math.random() * 10) + Math.ceil(Math.random() * 10);
                let params = {};
                let testController;

                TestController.DEFAULT_PAGE_SIZE = pageSize;
                TestController.PAGE_PARAM_NAME = pageParam;

                params[pageParam] = currentPage;

                testController = new TestController(createMockParams({ params: params }));

                assert.deepEqual(testController.getPagerParams(totalCount), {
                    pageParam: pageParam,
                    page: currentPage,
                    perpageCount: pageSize,
                    totalCount: totalCount,
                });
            });
        });

        describe('#checkPageNumberOverflow()', function() {
            /* jshint maxlen:false */
            it('should call processPageNumberOverflow when current page is greater than last available page', function() {
                let lastPage = 1 + Math.ceil(Math.random() * 100);
                let totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE;
                let currentPage = lastPage + 1;
                let processPageNumberOverflow;
                let testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage },
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isTrue(processPageNumberOverflow.calledWith(lastPage));
            });

            it('shouldn’t call processPageNumberOverflow when current page is less than available page', function() {
                let lastPage = 1 + Math.ceil(Math.random() * 100);
                let totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE;
                let currentPage = lastPage - 1;
                let processPageNumberOverflow;
                let testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage },
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isFalse(processPageNumberOverflow.called);
            });

            it('shouldn’t call processPageNumberOverflow when current page equals to available page', function() {
                let lastPage = 1 + Math.ceil(Math.random() * 100);
                let totalCount = lastPage * TestController.DEFAULT_PAGE_SIZE;
                let currentPage = lastPage;
                let processPageNumberOverflow;
                let testController;

                testController = new TestController(createMockParams({
                    params: { page: currentPage },
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(totalCount);

                assert.isFalse(processPageNumberOverflow.called);
            });

            it('should set last available page to 1 if total count is 0', function() {
                let processPageNumberOverflow;
                let testController;

                testController = new TestController(createMockParams({
                    params: { page: 5 },
                }));

                processPageNumberOverflow = sinon.stub(testController, 'processPageNumberOverflow');

                testController.checkPageNumberOverflow(0);

                assert.isTrue(processPageNumberOverflow.calledWith(1));
            });
        });

        describe('#processPageNumberOverflow()', function() {
            let params = {
                foo: 1,
                bar: 2,
                baz: 3,
                page: 6,
            };
            let routeName = 'paginator';
            let testController;

            beforeEach(function() {
                testController = new TestController(createMockParams({
                    req: {
                        urlHelper: new Url({
                            url: 'http://ya.ru/paginator?' + qs.stringify(params),
                            routers: {
                                desktop: require('./mocks/susanin-for-make-url'),
                            },
                        }),
                    },
                }));

                testController.getRoute = sinon.stub().returns({
                    getName: sinon.stub().returns(routeName),
                });

                testController.getParams = sinon.stub().returns(util.extend({}, params));
            });

            it('should call throwRedirectError', function() {
                let throwRedirectError = sinon.stub(testController, 'throwRedirectError');

                testController.processPageNumberOverflow(2);
                assert.isTrue(throwRedirectError.calledOnce);
            });

            it('should redirect to current url with page param equals to last available page', function() {
                let link = sinon.stub();
                let pageType = 'paginator';
                let lastPage = Math.floor(Math.random() * 100);
                let linkArgs;

                sinon.stub(testController, 'throwRedirectError');
                testController.req.urlHelper = { link: link };
                testController.type = pageType;

                testController.processPageNumberOverflow(lastPage);

                linkArgs = link.args[0];

                assert.strictEqual(linkArgs[0], 'paginator');
                assert.deepEqual(linkArgs[1], util.extend({}, params, {
                    controller: pageType,
                    page: lastPage,
                }));
            });
        });
    });
});
