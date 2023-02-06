describe('i-show-feedback', function() {
    var serpRequestChannel = BEM.channel('serp-request'),
        feedbackChannel = BEM.channel('feedback'),
        block;

    beforeEach(function() {
        block = BEM.create('i-show-feedback');

        sinon.stub(serpRequestChannel, 'trigger');
        sinon.stub(feedbackChannel, 'trigger');

        sinon.spy(block, 'loadPopup');
        sinon.spy(block, 'openPopup');
    });

    afterEach(function() {
        var self = block.__self;

        block.loadPopup.restore();
        block.openPopup.restore();

        block = null;

        self.clickedParams = null;
        self.feedbackTypesRequested = {};
        self.feedbackTypesLoaded = {};
        self.currentRequestedType = null;

        serpRequestChannel.trigger.restore();
        feedbackChannel.trigger.restore();
    });

    describe('before load', function() {
        it('should initially call loadPopup on show', function() {
            block.show({ type: 'default' });
            block.show({ type: 'default' });

            assert.calledOnce(block.loadPopup);
            assert.notCalled(block.openPopup);
        });
    });

    describe('loadPopup.success', function() {
        beforeEach(function() {
            serpRequestChannel.trigger.yieldsTo('success', { html: 'test', params: { type: 'default' } });

            block.show({ type: 'default' });

            Ya.asyncQueue.executeSync();
        });

        it('should call openPopup after successful loading', function() {
            assert.calledOnce(block.loadPopup);
            assert.calledOnce(block.openPopup);
        });

        it('should call openPopup when popup is already loaded', function() {
            block.show({ type: 'default' });

            assert.calledOnce(block.loadPopup);
            assert.calledTwice(block.openPopup);
        });
    });

    describe('loadPopup.error', function() {
        beforeEach(function() {
            serpRequestChannel.trigger.yieldsTo('error');

            block.show({ type: 'default' });
        });

        it('should not call openPopup after error', function() {
            assert.calledOnce(block.loadPopup);
            assert.notCalled(block.openPopup);
        });

        it('should allow to retry loading after error', function() {
            block.show({ type: 'default' });

            assert.calledOnce(block.loadPopup);
        });
    });

    describe('feedback params', function() {
        it('should trigger feedback "show" with last taken params', function() {
            block.show('p1');
            block.show('p2');

            // просто вызываем руками
            block.openPopup();

            assert.calledWith(feedbackChannel.trigger, 'show', 'p2');
        });
    });
});
