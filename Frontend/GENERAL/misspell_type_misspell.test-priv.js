describeBlock('misspell_type_misspell', function(block) {
    var data, wzrd, formatQuery;

    stubBlocks(
        'misspell__mark-corrections',
        'misspell__button',
        'misspell__message'
    );

    beforeEach(function() {
        data = stubData('cgi', 'counters');
        wzrd = {};
        formatQuery = '';
    });

    it('should be undefined without items', function() {
        assert.isUndefined(block(data, wzrd, formatQuery));
    });
});
