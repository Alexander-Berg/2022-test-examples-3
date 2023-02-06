describe('user-ready', function() {
    var sandbox, clock, block;

    beforeEach(function() {
        sandbox = sinon.createSandbox();
        clock = sandbox.useFakeTimers();

        block = BEM.blocks['user-ready'].getInstance();
    });

    afterEach(function() {
        sandbox.restore();

        BEM.blocks['user-ready']._clearInstance();
    });

    describe('#onUserReady()', function() {
        describe('when init', function() {
            it('should call back on timeout', function(done) {
                var cb = sinon.spy(check);

                block.onUserReady(cb);
                clock.tick(block.READY_TIMEOUT + 100);

                function check() {
                    assert.calledOnce(cb);
                    done();
                }
            });

            it('should call back on #setUserReady() call', function(done) {
                var cb = sinon.spy(check);

                block.onUserReady(cb);
                block.setUserReady();

                function check() {
                    assert.calledOnce(cb);
                    done();
                }
            });

            it('should call back with context', function(done) {
                var cb = sinon.spy(check),
                    ctx = {};

                block.onUserReady(cb, ctx);
                block.setUserReady();

                function check() {
                    assert.calledOnce(cb);
                    assert.calledOn(cb, ctx);
                    done();
                }
            });

            it('should call back exactly once', function(done) {
                var cb = sinon.spy(check);

                block.onUserReady(cb);

                block.setUserReady();
                block.setUserReady();

                function check() {
                    assert.calledOnce(cb);
                    done();
                }
            });
        });

        describe('when ready', function() {
            it('should call back on next tick without conditions', function(done) {
                makeReady(testCallImmediately);

                function makeReady(onReady) {
                    var cb = sinon.spy(check);

                    block.onUserReady(cb);
                    clock.tick(block.READY_TIMEOUT + 100);

                    function check() {
                        assert.calledOnce(cb);
                        onReady();
                    }
                }

                function testCallImmediately() {
                    var cb = sinon.spy(check);
                    block.onUserReady(cb);

                    function check() {
                        assert.calledOnce(cb);
                        done();
                    }
                }
            });
        });
    });
});
