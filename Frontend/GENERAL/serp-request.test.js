describe('common:serp-request', function() {
    var request, test, sandbox;

    before(function() {
        request = BEM.blocks['serp-request'];
        test = BEM.blocks['serp-request-test'];
    });

    beforeEach(function() {
        sandbox = sinon.createSandbox();
        test.stubGlobals();
    });

    afterEach(function() {
        sandbox.restore();
        test.resetState();
        test.clearSerpRequest();
    });

    describe('initialization', function() {
        var initSpy, reqStubs;

        beforeEach(function() {
            initSpy = sinon.spy();
            reqStubs = stubModHandlers('i-request_type_ajax', { onSetMod: { js: initSpy } });

            reqStubs.init();

            test.createSerpRequest();
        });

        afterEach(function() {
            reqStubs.restore();
        });

        it('should init i-request_type_ajax on create', function() {
            assert.calledOnce(initSpy);
        });

        it('should throw an error when created without getInstance()', function(done) {
            sandbox.stub(window, 'logSerpJsError').callsFake(function() {
                assert.calledOnce(window.logSerpJsError);

                var err = window.logSerpJsError.args[0][0];
                assert.propertyVal(err, 'message', 'serp-request: multiple initialization');

                done();
            });

            BEM.create.call(BEM, 'serp-request');
        });
    });

    describe('i-request_type_ajax params', function() {
        var params, reqStubs;

        beforeEach(function() {
            reqStubs = stubModHandlers('i-request_type_ajax', {
                onSetMod: {
                    js: function() {
                        params = this.params;
                    }
                }
            });
            reqStubs.init();
            test.createSerpRequest();
        });

        afterEach(function() {
            reqStubs.restore();
        });

        it('should pass a correct AJAX URL', function() {
            assert.propertyVal(params, 'url', test.LOCATION_STATE.path);
        });

        it('should pass a correct Yandex UID', function() {
            assert.nestedPropertyVal(params, 'data.yu', test.GLOBAL_PARAMS.yandexuid);
        });

        it('should pass a correct static version', function() {
            assert.nestedPropertyVal(params, 'data.st', 'STATIC_VERSION');
        });
    });

    describe('successful request', function() {
        var response, channel, getSpy;

        beforeEach(function() {
            getSpy = sinon.spy();

            response = test.createResponse();
            test.stubGetMethod(function(params, onSuccess) {
                getSpy.apply(this, arguments);
                onSuccess(response);
            });

            channel = test.createSerpRequest();
        });

        afterEach(function() {
            getSpy.resetHistory();
        });

        it('should emit a success event', function(done) {
            var timeout = request.DEBOUNCE_TIME * 2,
                errTimeout = setTimeout(function() {
                    done(Error('Success is not emitted'));
                }, timeout);

            channel.onFirst('success', function() {
                clearTimeout(errTimeout);
                done();
            });
            channel.trigger('request', { key: 'blockId' });
        });

        it('should call a success callback', function(done) {
            channel.trigger('request', {
                key: 'blockId',
                success: function(data) {
                    assert.propertyVal(response, 'blockId', data);
                    done();
                },
                error: function() {
                    done(Error('Error callback called'));
                }
            });
        });

        it('should log an exception in the callback', function(done) {
            sandbox.stub(window, 'logSerpJsError').callsFake(function() {
                assert.calledOnce(window.logSerpJsError);

                var err = window.logSerpJsError.args[0][0];
                assert.propertyVal(err, 'message', 'Test error');
                assert.propertyVal(err, 'catchType', 'ajax');

                done();
            });

            channel.trigger('request', {
                key: 'blockId',
                success: function() {
                    throw Error('Test error');
                }
            });
        });

        it('should pass location params', function(done) {
            test.makeRequest(done, function() {
                var params = getRequestParams();
                assert.includeMembers(Object.keys(params), Object.keys(test.LOCATION_STATE.params));
            });
        });

        it('should pass user params', function(done) {
            test.makeRequest(done, function() {
                var params = getRequestParams();
                assert.property(params, 'z');
            }, { params: { z: 3 } });
        });

        it('should pass an AJAX data', function(done) {
            var data = { z: 3 },
                expected = JSON.stringify({ blockId: data, log: {} });

            test.makeRequest(done, function() {
                var params = getRequestParams();
                assert.propertyVal(params, 'ajax', expected);
            }, { data: data });
        });

        it('should pass an extra global params', function(done) {
            test.makeRequest(done, function() {
                var params = getRequestParams();
                assert.propertyVal(params, 'extraParam', test.GLOBAL_PARAMS.serpRequestExtraParams.extraParam);
            });
        });

        it('should pass reload reason', function(done) {
            channel.trigger($.Event('request', { reloadReason: '2' }), {
                key: 'blockId',
                success: function() {
                    var params = getRequestParams();
                    assert.propertyVal(params, 'reload', '2');
                    done();
                }
            });
        });

        it('should set new global params', function(done) {
            var respParams = response.serp.params,
                expectedParams = {
                    clckId: undefined,
                    reqid: respParams.reqid,

                    // TODO: how to check Ya.clck ?
                    // clck: respParams.clck,
                    query: respParams.query,
                    sk: respParams.sk,
                    serpRequestExtraParams: respParams.extraParams
                },

                paramsSpy = sinon.spy(),
                stub = stubBlockStaticMethods('i-global', { setParams: paramsSpy });

            stub.init();
            test.addStubToRestore(stub);

            test.makeRequest(done, function() {
                assert.calledWith(paramsSpy, expectedParams);
            });
        });

        it('should delete suggest_reqid from global', function(done) {
            var paramsSpy = sinon.spy(),
                stub = stubBlockStaticMethods('i-global', { deleteParam: paramsSpy });

            stub.init();
            test.addStubToRestore(stub);

            test.makeRequest(done, function() {
                assert.calledWith(paramsSpy, 'suggest_reqid');
            });
        });

        it('should group bouncing requests', function(done) {
            var REQUEST_COUNT = 5;

            channel.onFirst('success', function() {
                var params = getRequestParams(),
                    ajax = JSON.parse(params.ajax);

                for (var i = 0; i < REQUEST_COUNT; ++i) {
                    assert.nestedPropertyVal(ajax, 'blockId' + i + '.foo', 'bar' + i);
                }
                done();
            });

            for (var i = 0; i < REQUEST_COUNT; ++i) {
                channel.trigger('request', {
                    key: 'blockId' + i,
                    data: { foo: 'bar' + i }
                });
            }
        });

        it('should switch URL with preSearch setting', function(done) {
            test.makeRequest(done, function() {
                assert.nestedPropertyVal(getSpy.firstCall.args, '3.url', test.LOCATION_STATE.path + 'zero/');
            }, { settings: { preSearch: true } });
        });

        describe('with dontUpdateGlobal setting', function() {
            it('should add a "dug" param', function(done) {
                var data = { z: 3 },
                    expected = JSON.stringify({ blockId: data, dug: 1, log: {} });

                test.makeRequest(done, function() {
                    var params = getRequestParams();
                    assert.propertyVal(params, 'ajax', expected);
                }, {
                    data: data,
                    settings: { dontUpdateGlobal: true }
                });
            });

            it('should not set new global params', function(done) {
                var paramsSpy = sinon.spy(),
                    stub = stubBlockStaticMethods('i-global', { setParams: paramsSpy });

                stub.init();
                test.addStubToRestore(stub);

                test.makeRequest(done, function() {
                    var args = getRequestParams();

                    assert.notProperty(args, 'reqid');
                    // TODO: how to check Ya.clck ?
                    // assert.notProperty(args, 'clck');
                    assert.notProperty(args, 'query');
                    assert.notProperty(args, 'sk');
                    assert.notProperty(args, 'serpRequestExtraParams');
                }, { settings: { dontUpdateGlobal: true } });
            });
        });

        function getRequestParams() {
            assert.calledOnce(getSpy);
            return getSpy.firstCall.args[0];
        }
    });

    describe('request with issues', function() {
        var channel;

        beforeEach(function() {
            channel = test.createSerpRequest();
        });

        it('should reload page with invalid static host', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ 'static-host': 'http://invalid-static-host.ya.ru/web7/' }));
            });

            channel.trigger('request', { key: 'blockId' });
        });

        it('should reload a page with invalid response', function(done) {
            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 409 }));
            });

            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            channel.trigger('request', { key: 'blockId' });
        });

        it('should redirect to captcha when server sends it', function(done) {
            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ captcha: { 'captcha-page': 'http://captcha.ya.ru' } }));
            });

            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.equal(url, 'http://captcha.ya.ru');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            channel.trigger('request', { key: 'blockId' });
        });

        it('should reload page with invalid static host and reload=1', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=1');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ 'static-host': 'http://invalid-static-host.ya.ru/web7/' }));
            });

            channel.trigger('request', { key: 'blockId' });
        });

        it('should add load-blocks url param with invalid static host and renderOnStaticFail', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=1');
                    assert.include(url, 'load-blocks=blockId');

                    done();
                }
            });

            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ 'static-host': 'http://invalid-static-host.ya.ru/web7/' }));
            });
            stubs.init();
            test.addStubToRestore(stubs);

            channel.trigger('request', {
                key: 'blockId',
                settings: { renderOnStaticFail: true }
            });
        });
    });

    describe('cache', function() {
        var requestParams;

        it('should be enabled by default', function() {
            stubRequestInit();
            test.createSerpRequest();

            assert.isTrue(requestParams.cache);
        });

        it('should prevent from calling _onSuccess on cache hit', function(done) {
            stubAjax();
            dropCache();

            var onSuccessSpy = sandbox.spy(BEM.DOM.blocks['i-request_type_ajax'].prototype, '_onSuccess'),
                triggerRequest = createRequestTrigger(done);

            triggerRequest(function() {
                assert.calledOnce(onSuccessSpy);
                triggerRequest(function() {
                    assert.calledOnce(onSuccessSpy);
                    done();
                });
            });
        });

        it('should not prevent from updating assets on cache hit', function(done) {
            stubAjax();
            dropCache();

            var onSuccessSpy = sandbox.spy(BEM.DOM.blocks['i-request_type_ajax'].prototype, '_onSuccess'),
                updateAssetsStub = sandbox.stub(BEM.DOM.blocks['i-request_type_ajax'].prototype, '_updateAssets'),
                triggerRequest = createRequestTrigger(done);

            triggerRequest(function() {
                assert.calledOnce(onSuccessSpy);
                assert.calledOnce(updateAssetsStub);
                triggerRequest(function() {
                    assert.calledOnce(onSuccessSpy);
                    assert.calledTwice(updateAssetsStub);
                    done();
                });
            });
        });

        function stubAjax() {
            sandbox.stub($, 'ajax').callsFake(function() {
                return {
                    done: function(fn) {
                        setTimeout(fn.bind(null, test.createResponse()), 1);
                        return {
                            fail: $.noop
                        };
                    }
                };
            });
        }

        function createRequestTrigger(done) {
            var channel = test.createSerpRequest(),
                error = function() {
                    done(Error('Error callback called'));
                };

            return function(success) {
                channel.trigger('request', {
                    key: 'blockId',
                    success: function() {
                        // т.к. коллбэки перехватывают все ошибки и пишут в лог,
                        // мы самостоятельно ловим assertion error и фейлим тест через done(e)
                        try {
                            success();
                        } catch (e) {
                            done(e);
                        }
                    },
                    error: error
                });
            };
        }

        function dropCache() {
            var serpRequest = BEM.blocks['serp-request'].getInstance();
            serpRequest._getAjax().dropCache();
        }

        function stubRequestInit() {
            var stubs = stubModHandlers('i-request_type_ajax', {
                onSetMod: {
                    js: function() {
                        requestParams = this.params;
                    }
                }
            });

            stubs.init();
            test.addStubToRestore(stubs);
        }
    });
});

/**
 * Блок-хелпер для тестирования блоков serp-request и serp
 */
BEM.decl('serp-request-test', {}, {
    LOCATION_STATE: {
        url: 'http://ya.ru/serp/',
        path: '/search/',
        referer: 'http://ya.ru/',
        params: { x: 1, y: 2, text: ['query'], exp_flags: ['load-blocks=blockId'] }
    },

    GLOBAL_PARAMS: {
        yandexuid: 487,
        // Пример static-host: //web.yastatic.net/web4/0x9403384/,
        // где 9403384 - хеш коммита в web4
        'static-host': 'http://fake-static.ya.ru/web4/STATIC_VERSION/',
        serpRequestExtraParams: { extraParam: 'blockId' },
        sk: 'u456',
        ajaxUrl: '/search/'
    },

    _stubsToRestore: [],

    /**
     * Застабить глобальные объекты:
     *     * i-global
     *     * location
     * Создает шпиона this.globalCountersSpy
     */
    stubGlobals: function() {
        var globalParams = this.GLOBAL_PARAMS,
            locationState = this.LOCATION_STATE;

        this._globalStub = stubBlockStaticMethods('i-global', {
            param: function(name) {
                return globalParams[name];
            }
        });
        this._globalStub.init();

        this._locationStubs = stubBlockPrototype('location', {
            getState: function() {
                return locationState;
            }
        });
        this._locationStubs.init();
    },

    /**
     * Восстановить состояние застабленных глобальных объектов.
     * Необходимо вызвать этот метод в хуке after после всех тестов
     */
    resetState: function() {
        if (this._globalStub) {
            this._globalStub.restore();
            this._globalStub = null;
        }

        if (this._locationStubs) {
            this._locationStubs.restore();
            this._locationStubs = null;
        }

        while (this._stubsToRestore.length) {
            this._stubsToRestore.shift().restore();
        }
    },

    /**
     * Создать инстанс блока serp-request
     * @returns {$.observable|undefined}
     */
    createSerpRequest: function() {
        var serpRequest = BEM.blocks['serp-request'].getInstance();
        serpRequest.setRetryCount(0);
        return BEM.channel('serp-request');
    },

    /**
     * Очистить инстанс блока serp-request
     */
    clearSerpRequest: function() {
        BEM.blocks['serp-request'].clearInstance();
    },

    /**
     * Создать ответ сервера
     * @param {Object} [respData]
     * @returns {Object} Ответ с правдоподобной структурой
     */
    createResponse: function(respData) {
        respData = respData || {};
        respData.blockId = respData.blockId || 'data';

        return stubAjaxResponse(respData);
    },

    /**
     * Застабить метод get в i-request_type_ajax
     * @param {Function} [func] Функция, которая будет вызвана вместо метода get
     */
    stubGetMethod: function(func) {
        func = func || function(params, onSuccess) {
            onSuccess(this.createResponse());
        }.bind(this);

        var stubs = stubBlockPrototype('i-request_type_ajax', { get: func });
        stubs.init();

        this.addStubToRestore(stubs);
    },

    /**
     * Застабить метод _doRequest
     * @param {Function} [func]
     */
    stubRequest: function(func) {
        func = func || $.noop;

        var stubs = stubBlockPrototype('serp-request', { _doRequest: func });
        stubs.init();

        this.addStubToRestore(stubs);
    },

    /**
     * Добавить объект stub для автоматического восстановления после теста
     * @param {Object} stub
     */
    addStubToRestore: function(stub) {
        this._stubsToRestore.push(stub);
    },

    /**
     * Произвести запрос с помощью блока serp-request
     * @param {Function} done Колбэк завершения
     * @param {Function} [onSuccess] Колбэк успешного запроса
     * @param {Object} [userParams] Пользовательские параметры запроса
     */
    makeRequest: function(done, onSuccess, userParams) {
        onSuccess = onSuccess || $.noop;
        userParams = userParams || {};

        this.createSerpRequest().trigger('request', {
            key: 'blockId',
            params: userParams.params,
            data: userParams.data,
            settings: userParams.settings,

            success: function(data) {
                try {
                    onSuccess(data);
                    done();
                } catch (e) {
                    done(e);
                }
            }
        });
    }
});
