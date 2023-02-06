describeBlock('adapter-entity-card__ugc-reviews', function(block) {
    let context;
    let snpData;

    beforeEach(function() {
        context = {
            expFlags: {}
        };
        snpData = {};
    });

    it('should return undefined if has show_reviews=0', function() {
        snpData.display_options = {
            show_reviews: 0
        };
        assert.isUndefined(block(context, snpData));
    });
});
