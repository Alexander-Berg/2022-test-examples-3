describeBlock('i-global__page', function(block) {
    var data;

    stubBlocks(
        'RequestCtx',
        'extra-content-initialize',
        'serp-request',
        'i-global__params',
        'assets-tests',
        'b-page'
    );

    beforeEach(function() {
        data = stubData('counters');
        blocks['i-yabro-api'] && sinon.stub(blocks, 'i-yabro-api');
    });

    afterEach(function() {
        blocks['i-yabro-api'] && blocks['i-yabro-api'].restore();
    });

    it('should call pushbundle-initialize and assets-initialize', function() {
        block(data);

        assert.calledOnce(RequestCtx.GlobalContext.initBundles);

        assert.isTrue(RequestCtx.GlobalContext.initBundles.calledBefore(blocks['b-page']));
    });

    it('should call extra-content-initialize', function() {
        block(data);

        assert.calledOnce(blocks['extra-content-initialize']);
        assert.isTrue(blocks['extra-content-initialize'].calledBefore(blocks['b-page']));
    });

    it('should call "serp-request" block if data.ajax is true', function() {
        data.ajax = true;

        block(data);

        assert.calledOnce(blocks['serp-request']);
        assert.notCalled(blocks['i-global__params']);
        assert.notCalled(blocks['b-page']);
    });
});
