describeBlock('adapter-entity-card__ajax_type_soap-more', function(block) {
    let context;

    beforeEach(function() {
        context = {};
    });

    it('should return undefined if snippet.data is undefined', function() {
        assert.isUndefined(block(context, { app_host: {} }));
    });
});

describeBlock('adapter-entity-card__ajax_type_soap-season', function(block) {
    let context;

    beforeEach(function() {
        context = {};
    });

    it('should return undefined if snippet.data is undefined', function() {
        assert.isUndefined(block(context, { app_host: {} }));
    });
});
