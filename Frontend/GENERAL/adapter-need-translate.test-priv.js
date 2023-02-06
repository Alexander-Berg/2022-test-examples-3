describeBlock('adapter-need-translate__url', function(block) {
    var context, snippet, doc, result;

    stubBlocks('adapter-legacy__extralinks');

    beforeEach(function() {
        context = {};
        snippet = {
            doc_lang: 'rus',
            type: 'need_translate',
            user_lang: 'tur'
        };
        doc = { url: 'http://example.com' };
    });

    it('should return correct URL for Turkey', function() {
        context.tld = 'com.tr';

        result = block(context, snippet, doc);

        assert.strictEqual(result,
            '//ceviri.yandex.com.tr/translate?srv=yasearch&url=http%3A%2F%2Fexample.com&lang=rus-tur');
    });

    every(['ru', 'ua', 'kz', 'by', 'com'], 'should return correct URL for KUBR and com', function(tld) {
        context.tld = tld;

        result = block(context, snippet, doc);

        assert.strictEqual(result,
            '//translate.yandex.' + tld + '/translate?srv=yasearch&url=http%3A%2F%2Fexample.com&lang=rus-tur&ui=tur');
    });

    it('should return correct URL for unknown domains', function() {
        context.tld = 'kz';

        result = block(context, snippet, doc);

        assert.strictEqual(result,
            '//translate.yandex.kz/translate?srv=yasearch&url=http%3A%2F%2Fexample.com&lang=rus-tur&ui=tur');
    });
});
