describeBlock('main__ajax', function(block) {
    var data;

    stubBlocks('main__ajax-response-content');

    beforeEach(function() {
        data = stubData();
    });

    it('should call main__ajax-response-content', function() {
        block(data);

        assert.calledOnce(blocks['main__ajax-response-content']);
    });
});
