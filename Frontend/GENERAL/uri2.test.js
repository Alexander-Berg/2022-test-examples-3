describe('uri', function() {
    var block = BEM.blocks.uri;

    it('should remove multiple amp symbols', function() {
        var url = block.parse('https://yandex.ru/search/?text=test&&p=1');
        var urlStr = url.toString();

        assert.equal(urlStr, 'https://yandex.ru/search/?text=test&p=1');
    });

    it('should drop trailing amp symbol', function() {
        var url = block.parse('https://yandex.ru/search/?text=test&');
        var urlStr = url.toString();

        assert.equal(urlStr, 'https://yandex.ru/search/?text=test');
    });
});
