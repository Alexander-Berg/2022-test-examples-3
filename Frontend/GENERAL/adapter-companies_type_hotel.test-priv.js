describeBlock('adapter-companies_type_hotel__rooms-tab', function(block) {
    let context;
    let state;
    let params;

    beforeEach(function() {
        context = {
            expFlags: {}
        };
        state = {};
        params = {};
    });

    stubBlocks([
        'adapter-companies_type_hotel__ajax-params'
    ]);

    it('should return result if isTravelSubtype is defined', function() {
        state.isTravelSubtype = true;
        assert.isDefined(block(context, state, params));
    });

    it('shouldn\`t return result if isTravelSubtype is not defined', function() {
        state.isTravelSubtype = false;
        assert.isUndefined(block(context, state, params));
    });
});
