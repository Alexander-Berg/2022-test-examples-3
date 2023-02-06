describe('adapter-companies__ajax', () => {
    let context;
    let geoData;
    let state;
    let params;

    stubBlocks(
        'adapter-companies__prime',
        'adapter-companies__bcard-items',
        'adapter-companies__card',
        'adapter-companies__reviews'
    );

    beforeEach(() => {
        geoData = {
            features: [{}]
        };
        context = {
            expFlags: stubData('experiments'),
            defaultCounter: {}

        };
        state = {
            activeTab: 'reviews',
            urls: {},
            reviews: 123
        };
        params = {};

        sinon.stub(blocks, 'adapter-companies__prepare-state').callsFake(() => state);
        sinon.stub(blocks, 'adapter-companies__prepare-state-org-one').callsFake(() => state);
        sinon.stub(blocks, 'adapter-companies__ajax-prepare-data').callsFake(() => ({}));
        sinon.stub(blocks, 'adapter-companies__ajax-points-list-drag').callsFake(() => ({}));
        sinon.stub(blocks, 'adapter-companies__tabs').callsFake((_, __, tabsData) => tabsData);
        sinon.stub(blocks, 'construct__context-inherit').returns(context);
    });

    afterEach(() => {
        blocks['adapter-companies__prepare-state'].restore();
        blocks['adapter-companies__prepare-state-org-one'].restore();
        blocks['adapter-companies__ajax-prepare-data'].restore();
        blocks['adapter-companies__ajax-points-list-drag'].restore();
        blocks['adapter-companies__tabs'].restore();
        blocks['construct__context-inherit'].restore();
    });

    describeBlock('adapter-companies__ajax-points', block => {
        it('should return undefined if geo-answer is empty', () => {
            assert.isUndefined(block(context, null, params));
        });

        it('shouldn\'t return undefined if geo-answer isn\'t empty', () => {
            assert.isDefined(block(context, geoData, params));
        });
    });

    describeBlock('adapter-companies__ajax-bcard', block => {
        it('should set active state for reviews tab', () => {
            const reviewsTab = block(context, geoData, params).content.items.find(item => item.tabName === 'reviews');
            assert.isTrue(reviewsTab.active);
        });

        it('should not exist reviews tab fo foreign language', () => {
            context.isForeign = true;

            const reviewsTab = block(context, geoData, params).content.items.find(item => item.tabName === 'reviews');
            assert.notExists(reviewsTab);
        });
    });
});
