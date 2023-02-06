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

    it('should return nothing if no errors', function() {
        result = block(data);

        assert.isUndefined(result);
    });

    it('should return error message from error block if err_code != 15', function() {
        blocks['error'].returns('Задан пустой поисковый запрос');
        data.searchdata.err_code = 14;

        result = block(data);

        assert.calledWith(blocks['error'], 14);
        assert.nestedPropertyVal(result, 'content.content', 'Задан пустой поисковый запрос');
    });

    it('should return correct error message if error code == 15', function() {
        data.searchdata.err_code = 15;

        result = block(data);

        assert.nestedPropertyVal(result, 'content.content', 'По вашему запросу ничего не нашлось');
    });
});
