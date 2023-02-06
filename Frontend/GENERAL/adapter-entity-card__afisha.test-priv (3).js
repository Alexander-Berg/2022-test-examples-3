describeBlock('adapter-entity-card__afisha-filters', function(block) {
    let context;
    let filters;
    let variables;

    stubBlocks([
        'adapter-entity-card__afisha-filter-date',
        'adapter-entity-card__afisha-filter-price',
        'adapter-entity-card__afisha-filter-format',
        'adapter-entity-card__afisha-filter-daytime'
    ]);

    beforeEach(function() {
        context = {};
        filters = {};
        variables = {};
    });

    it('should return an Array without falsy values', function() {
        blocks['adapter-entity-card__afisha-filter-date'].returns(null);
        blocks['adapter-entity-card__afisha-filter-price'].returns({});
        blocks['adapter-entity-card__afisha-filter-format'].returns({});
        blocks['adapter-entity-card__afisha-filter-daytime'].returns({});
        assert.deepEqual(block(context, filters, variables), [{}, {}, {}]);
    });
});

describeBlock('adapter-entity-card__afisha-filter-date', function(block) {
    let context;
    let dates;

    stubBlocks('adapter-entity-card__afisha-date');

    beforeEach(function() {
        context = {};
        dates = [];
    });

    every([null, false, undefined], 'should return undefined if dates is not an Array', function(dates) {
        assert.isUndefined(block(context, dates));
    });

    it('should return Object if dates is an Array', function() {
        assert.isObject(block(context, dates));
    });
});
