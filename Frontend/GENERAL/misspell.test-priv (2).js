describeBlock('misspell__message', function(block) {
    var data, params;

    stubBlocks(
        'misspell__button'
    );

    beforeEach(function() {
        data = stubData();
        params = {
            messageText: 'Яндекс'
        };
    });

    it('should have content equal to messageText if messagePrefix not defined', function() {
        var content = block(data, params)[0].content;

        assert.equal(content, params.messageText);
    });

    it('should have content containing messagePrefix and messageText if messagePrefix defined', function() {
        params.messagePrefix = 'Опечатка';
        var content = block(data, params)[0].content;

        assert.equal(content, params.messagePrefix);
    });
});
