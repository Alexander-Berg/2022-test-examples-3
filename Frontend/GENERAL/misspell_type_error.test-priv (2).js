describeBlock('misspell_type_error', function(block) {
    var data,
        result;

    stubBlocks(
        'RequestCtx',
        'error'
    );

    beforeEach(function() {
        data = stubData('searchdata');
    });

    it('should show message if blogs', function() {
        data.searchdata.err_code = 2;
        data.reqdata.path = 'blogs';

        RequestCtx.GlobalContext.report = 'blogs';

        result = block(data);

        assert.isDefined(result.content);
    });
});
