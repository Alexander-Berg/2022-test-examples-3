describeBlock('b-page__title', function(block) {
    var data;

    stubBlocks('RequestCtx');

    beforeEach(function() {
        data = {};
    });

    it('should returns page title', function() {
        assert.equal(block(data), 'Яндекс');
    });

    it('should returns `en` page title for foreign domains', function() {
        RequestCtx.GlobalContext.isForeign = true;
        assert.equal(block(data), 'Yandex');
    });

    it('should not returns page title for Yandex Browser in pre-search', function() {
        data.isPreSearch = true;
        RequestCtx.GlobalContext.device = { BrowserName: 'YandexBrowser' };
        assert.equal(block(data), '');
    });

    it('should not returns page title for YandexSearchBrowser (aka searchapp browser) in pre-search', function() {
        data.isPreSearch = true;
        RequestCtx.GlobalContext.isYandexSearchBrowser = true;
        assert.equal(block(data), '');
    });
});
