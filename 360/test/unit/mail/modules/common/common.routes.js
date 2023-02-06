describe('common.routes.js.', function() {
    beforeEach(function() {
        var mFolders = ns.Model.get('folders');
        setModelByMock(mFolders);

        ns.router.buildFoldersUrls();

        setModelsByMock('message');

        ns.router.init();
    });

    var TESTS = [
        {
            url: '#attachments',
            expect: {
                page: 'messages',
                params: {
                    attachments: 'attachments',
                    goto: 'all',
                    extra_cond: 'only_atta'
                }
            }
        },
        {
            url: '#attachments/123',
            expect: {
                page: 'messages',
                params: {
                    attachments: 'attachments',
                    goto: 'all',
                    extra_cond: 'only_atta',
                    ids: '123'
                }
            }
        },
        {
            url: '#unread',
            expect: {
                page: 'messages',
                params: {
                    unread: 'unread',
                    goto: 'all',
                    extra_cond: 'only_new'
                }
            }
        },
        {
            url: '#unread/123',
            expect: {
                page: 'messages',
                params: {
                    unread: 'unread',
                    goto: 'all',
                    extra_cond: 'only_new',
                    ids: '123'
                }
            }
        },
        {
            url: '#inbox',
            expect: {
                page: 'messages',
                params: {
                    current_folder: '1'
                }
            }
        },
        {
            url: '#inbox/message/2',
            expect: {
                page: 'messages',
                params: {
                    current_folder: '1',
                    ids: '2'
                }
            }
        },
        {
            url: '#inbox/thread/t3',
            expect: {
                page: 'messages',
                params: {
                    current_folder: '1',
                    thread_id: 't3'
                }
            }
        },
        {
            url: '#thread/t1',
            expect: {
                page: 'messages',
                params: {
                    thread_id: 't1'
                }
            }
        },
        {
            url: '#thread/t1/message/1',
            expect: {
                page: 'messages',
                params: {
                    ids: '1',
                    thread_id: 't1'
                }
            }
        }
    ];

    describe('Parse.', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane').returns(false);
            this.sinon.stub(Daria, 'is2pane').returns(true);
        });

        TESTS.forEach(function(test) {
            it(test.url, function() {
                expect(ns.router(test.url)).to.be.eql(test.expect);
            });
        });
    });

    describe('Generate.', function() {
        TESTS.forEach(function(test) {
            it(test.url, function() {
                var res = _.extend({}, test.expect);
                expect(ns.router.generateUrl(res.page, res.params)).to.be.equal(test.url);
            });
        });
    });

    describe('tabs (on)', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
            this.sinon.stub(Daria, 'is3pane').returns(false);
            this.sinon.stub(Daria, 'is2pane').returns(true);
        });
        const TESTS = [
            {
                url: '#tabs/news?extra_cond=only_new&current_folder=1',
                expect: {
                    page: 'messages',
                    params: {
                        extra_cond: 'only_new',
                        tabId: 'news',
                        current_folder: '1'
                    }
                }
            },
            {
                url: '#tabs/news/message/123?extra_cond=only_new&current_folder=1',
                expect: {
                    page: 'messages',
                    params: {
                        extra_cond: 'only_new',
                        ids: '123',
                        tabId: 'news',
                        current_folder: '1'
                    }
                }
            },
            {
                url: '#tabs/relevant/message/2?current_folder=1',
                expect: {
                    page: 'messages',
                    params: {
                        current_folder: '1',
                        ids: '2',
                        tabId: 'relevant'
                    }
                }
            },
            {
                url: '#tabs/social/thread/t3?current_folder=1',
                expect: {
                    page: 'messages',
                    params: {
                        current_folder: '1',
                        thread_id: 't3',
                        tabId: 'social'
                    }
                }
            }
        ];

        TESTS.forEach(function(test) {
            it(test.url, function() {
                expect(ns.router(test.url)).to.be.eql(test.expect);
            });
        });
    });

    describe('redirect tabs (tabs off)', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(false);
        });

        const TESTS = [
            {
                url: '#tabs/default',
                expect: '#inbox'
            },
            {
                url: '#tabs/relevant',
                expect: '#inbox'
            },
            {
                url: '#tabs/social/message/1234',
                expect: '#inbox/message/1234'
            },
            {
                url: '#tabs/news/thread/t1234',
                expect: '#inbox/thread/t1234'
            }
        ];

        TESTS.forEach(function(test) {
            it(test.url, function() {
                this.sinon.stub(ns.page, 'getCurrentUrl').returns(test.url);

                expect(ns.router(test.url)).to.be.eql({
                    page: ns.R.REDIRECT,
                    params: {},
                    redirect: test.expect
                });
            });
        });
    });

    describe('redirect tabs (tabs on)', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
        });

        const TESTS = [
            {
                url: '#tabs/default',
                expect: '#tabs/relevant'
            },
            {
                url: '#inbox',
                expect: '#tabs/relevant'
            },
            {
                url: '#inbox/message/111',
                expect: '#tabs/social/message/111',
                hasTabId: true,
                isThread: false
            },
            {
                url: '#inbox/thread/t111',
                expect: '#tabs/social/thread/t111',
                hasTabId: true,
                isThread: true
            }
        ];

        TESTS.forEach(function(test) {
            it(test.url, function() {
                this.sinon.stub(ns.page, 'getCurrentUrl').returns(test.url);

                if (test.hasTabId) {
                    const mMessage = ns.Model.get('message', { ids: test.isThread? 't111' : '111' });
                    this.sinon.stub(mMessage, 'getTabId').returns('social');
                }

                expect(ns.router(test.url)).to.be.eql({
                    page: ns.R.REDIRECT,
                    params: {},
                    redirect: test.expect
                });
            });
        });
    });

    describe('opens inbox links when tabs is on, but no param tabId', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'areFoldersTabsEnabled').returns(true);
        });

        it('#inbox/message/111 -> #inbox/message/111', function() {
            const url = '#inbox/message/111';

            this.sinon.stub(ns.page, 'getCurrentUrl').returns(url);

            expect(ns.router(url)).to.be.eql({
                page: 'messages',
                params: {
                    current_folder: '1',
                    ids: '111'
                }
            });
        });

        it('#inbox/thread/t111 -> #inbox/thread/t111', function() {
            const url = '#inbox/thread/t111';

            this.sinon.stub(ns.page, 'getCurrentUrl').returns(url);

            expect(ns.router(url)).to.be.eql({
                page: 'messages',
                params: {
                    current_folder: '1',
                    thread_id: 't111'
                }
            });
        });
    });

    describe('redirect 3pane.', function() {
        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(Daria, 'is2pane').returns(false);
        });

        var TESTS = [
            {
                url: '#inbox/thread/123',
                expect: '#inbox/thread/t123'
            },
            {
                url: '#folder/321/thread/123',
                expect: '#folder/321/thread/t123'
            },
            {
                url: '#thread/123',
                expect: '#thread/t123'
            },
            {
                url: '#message/5',
                expect: '#spam/message/5'
            },
            {
                url: '#message/5/links',
                expect: '#spam/message/5'
            },
            {
                // нет инфы о письме
                url: '#message/12345',
                expect: '#inbox/message/12345'
            }
        ];

        TESTS.forEach(function(test) {
            it(test.url, function() {
                this.sinon.stub(ns.page, 'getCurrentUrl').returns(test.url);

                expect(ns.router(test.url)).to.be.eql({
                    page: ns.R.REDIRECT,
                    params: {},
                    redirect: test.expect
                });
            });
        });
    });

    /*
    describe('redirect 3pane LOM.', function() {

        beforeEach(function() {
            this.sinon.stub(Daria, 'is3pane').returns(true);
            this.sinon.stub(Daria, 'is2pane').returns(false);
            this.params = {current_folder: '1'};

            ns.Model.get('messages', this.params).setData({
                message: [
                    {mid: '1', tid: 't1'},
                    {mid: 't2', tid: 't2'}
                ]
            });
            this.mSettings = ns.Model.get('settings');

            ns.router.init();
        });

        it('должен сделать редирект на письмо, если есть LOM и есть в кеше (тест для папки)', function() {
            this.sinon.stub(Daria.lom, 'getByParams').returns('1');

            expect(ns.router('#folder/1')).to.be.eql({
                page: ns.R.REDIRECT,
                params: {},
                redirect: '#inbox/message/1'
            });
        });

        it('должен сделать редирект на письмо, если есть LOM и есть в кеше (тест для symbol)', function() {
            this.sinon.stub(Daria.lom, 'getByParams').returns('1');

            expect(ns.router('#inbox')).to.be.eql({
                page: ns.R.REDIRECT,
                params: {},
                redirect: '#inbox/message/1'
            });
        });

        it('должен сделать редирект на тред, если есть LOM и есть в кеше (тест для папки)', function() {
            this.sinon.stub(Daria.lom, 'getByParams').returns('t2');

            expect(ns.router('#folder/1')).to.be.eql({
                page: ns.R.REDIRECT,
                params: {},
                redirect: '#inbox/thread/t2'
            });
        });

        it('должен сделать редирект на тред, если есть LOM и есть в кеше (тест для symbol)', function() {
            this.sinon.stub(Daria.lom, 'getByParams').returns('t2');

            expect(ns.router('#inbox')).to.be.eql({
                page: ns.R.REDIRECT,
                params: {},
                redirect: '#inbox/thread/t2'
            });
        });

        it('не должен сделать редирект на тред, если нет LOM', function() {
            this.sinon.stub(Daria.lom, 'getByParams').returns('t3');

            expect(ns.router('#folder/1')).to.be.eql({
                page: 'messages',
                params: {
                    current_folder: '1'
                }
            });
        });

    });
    */
});
