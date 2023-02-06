describe('desktop:serp-request', function() {
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

    describe('request with issues', function() {
        var channel;

        beforeEach(function() {
            channel = test.createSerpRequest();
        });

        it('should emit an error event', function(done) {
            var timeout = request.DEBOUNCE_TIME * 2,
                errTimeout = setTimeout(function() {
                    done(Error('Error is not emitted'));
                }, timeout);

            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 500 }));
            });

            channel.onFirst('error', function() {
                clearTimeout(errTimeout);
                done();
            });
            channel.trigger('request', { key: 'blockId' });
        });

        it('should call an error callback on server error', function(done) {
            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 500 }));
            });

            channel.trigger('request', {
                key: 'blockId',
                error: function(err) {
                    assert.propertyVal(err, 'type', request.ERROR_AJAX);
                    assert.nestedPropertyVal(err, 'data.status', 500);
                    done();
                }
            });
        });

        it('should call an error callback with server error param', function(done) {
            test.stubGetMethod(function(params, onSuccess) {
                onSuccess(test.createResponse({ error: 'foo' }));
            });

            channel.trigger('request', {
                key: 'blockId',
                error: function(err) {
                    assert.propertyVal(err, 'type', request.ERROR_SERVER);
                    assert.propertyVal(err, 'data', 'foo');
                    done();
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

            test.stubGetMethod(function(params, onSuccess, onError) {
                onError(test.createResponse({ status: 500 }));
            });

            channel.trigger('request', {
                key: 'blockId',
                error: function() {
                    throw Error('Test error');
                }
            });
        });
    });
});
