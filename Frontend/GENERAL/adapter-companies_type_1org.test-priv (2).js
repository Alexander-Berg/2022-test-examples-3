describeBlock('adapter-companies_type_1org__items-head', function(block) {
    let context;
    let state;

    stubBlocks(
        'adapter-companies__prime'
    );

    beforeEach(function() {
        context = stubData('experiments');
        state = {
            activeTab: 'reviews'
        };
        sinon.stub(blocks, 'adapter-companies__reviews-available').returns(true);
    });

    afterEach(function() {
        blocks['adapter-companies__reviews-available'].restore();
    });

    it('should ignore state.activeTab and set active state for main tab', function() {
        const mainTab = block(context, state).data.items.find(item => item.tabName === 'about');
        assert.isTrue(mainTab.active);
    });
});
