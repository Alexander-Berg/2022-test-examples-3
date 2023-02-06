describeBlock('adapter-entity-card__is-feature-disabled', function(block) {
    let glob;

    beforeEach(function() {
        glob = stubGlobal('RequestCtx');
        RequestCtx.GlobalContext = { expFlags: {} };
    });

    afterEach(function() {
        glob.restore();
    });

    it('should return true if flag value is featureName', function() {
        RequestCtx.GlobalContext.expFlags.OO_disable_entity_feature = 'soap';

        assert.isTrue(block('soap'), 'returned false for soap');
        assert.isFalse(block('so'), 'returned true for so');
        assert.isFalse(block('figures'), 'returned true for figures');
    });

    it('should return true if flag value contains featureName', function() {
        RequestCtx.GlobalContext.expFlags.OO_disable_entity_feature = 'soap,offers';

        assert.isTrue(block('soap'), 'returned false for soap');
        assert.isTrue(block('offers'), 'returned false for offers');
        assert.isFalse(block('figures'), 'returned true for figures');
        assert.isFalse(block('offer'), 'returned true for offer');
    });
});
