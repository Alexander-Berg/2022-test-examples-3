describeBlock('adapter-companies__tabs', function(block) {
    let context;
    let state;

    stubBlocks('RequestCtx', 'adapter-companies__tabs-main');

    beforeEach(function() {
        context = {
            expFlags: stubData('experiments'),
            reqid: '1234567890'
        };
        state = {
            reviews: [],
            activeTab: 'reviews'
        };
        sinon.stub(blocks, 'adapter-companies__prepare-state').callsFake(() => ({ activeTab: 'reviews', urls: {} }));
        sinon.stub(blocks, 'adapter-companies__ajax-prepare-data').callsFake(() => ({ }));
        sinon.stub(blocks, 'construct__context-inherit').returns(context);
    });

    afterEach(function() {
        blocks['adapter-companies__prepare-state'].restore();
        blocks['adapter-companies__ajax-prepare-data'].restore();
        blocks['construct__context-inherit'].restore();
    });

    it('should set active state for reviews tab', function() {
        const reviewsTab = block(context, state).items.find(item => item.tabName === 'reviews');
        assert.isTrue(reviewsTab.active);
    });
});
