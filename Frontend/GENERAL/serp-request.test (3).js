describe('touch-phone:serp-request', function() {
    var test, sandbox;

    before(function() {
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

    describe('request with issues', function() {
        var channel;

        beforeEach(function() {
            channel = test.createSerpRequest();
        });

        it('should call an error callback when request timeout occurred on pressed "more" button', function(done) {
            var clock = sandbox.useFakeTimers();
            var request = BEM.blocks['serp-request'];
            channel.trigger('request', {
                key: 'more',
                error: function(err) {
                    assert.propertyVal(err, 'type', request.ERROR_AJAX);
                    assert.nestedPropertyVal(err, 'data.statusText', 'timeout');
                    done();
                }
            });

            clock.tick(request.REQUEST_TIMEOUT + 500); // Провоцируем таймаут
        });

        it('should log an exception in the callback on pressed "more" button', function(done) {
            sandbox.stub(window, 'logSerpJsError').callsFake(function() {
                assert.calledOnce(window.logSerpJsError);

                var err = window.logSerpJsError.args[0][0];
                assert.propertyVal(err, 'message', 'Test error');
                assert.propertyVal(err, 'catchType', 'ajax');

                done();
            });

            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 500 }));
            });

            channel.trigger('request', {
                key: 'more',
                error: function() {
                    throw Error('Test error');
                }
            });
        });

        it('should reload a page and reload=3 on server error', function(done) {
            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 500 }));
            });

            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=3');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            channel.trigger('request', { key: 'serp' });
        });

        it('should reload a page with invalid response and reload=3', function(done) {
            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 409 }));
            });

            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=3');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            channel.trigger('request', { key: 'serp' });
        });

        it('should reload a page with server error and reload=5', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=5');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ error: 'server error' }));
            });

            channel.trigger('request', { key: 'serp' });
        });

        it('should reload a page with invalid ajax params and reload=6', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=6');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ serp: null }));
            });

            channel.trigger('request', { key: 'serp' });
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

            channel.trigger('request', { key: 'serp' });
        });

        it('should reload page on offline and reload=3', function(done) {
            var stubs = stubBlockStaticMethods('serp-request', {
                changeLocation: function(url) {
                    assert.include(url, test.LOCATION_STATE.url);
                    assert.include(url, 'reload=3');
                    done();
                }
            });
            stubs.init();
            test.addStubToRestore(stubs);

            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 0 }));
            });

            channel.trigger('request', { key: 'serp' });
        });
    });
});
