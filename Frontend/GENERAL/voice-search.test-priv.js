describeBlock('voice-search__has-support', function(block) {
    var data;

    stubBlocks(
        'RequestCtx',
        'voice-search__browsers'
    );

    beforeEach(function() {
        blocks['voice-search__browsers'].returns({ YandexBrowser: 14 });

        data = {};
    });

    it('should skip ajax', function() {
        assert.notOk(block({ ajax: true }));
    });

    it('should skip direct page', function() {
        RequestCtx.GlobalContext.isDirectPage = true;

        assert.notOk(block(data));
    });

    it('should skip YaBro for "en" translation', function() {
        RequestCtx.GlobalContext.device = { BrowserName: 'YandexBrowser', BrowserVersionRaw: 'v14.0002' };
        RequestCtx.GlobalContext.language = 'en';

        assert.notOk(block(data));
    });

    it('should skip old YaBro', function() {
        RequestCtx.GlobalContext.device = { BrowserName: 'YandexBrowser', BrowserVersionRaw: 'v10.0001' };

        assert.notOk(block(data));
    });

    it('should check browser version correctly', function() {
        RequestCtx.GlobalContext.device = { BrowserName: 'YandexBrowser', BrowserVersionRaw: 'v14.0002' };

        assert.ok(block(data));

        RequestCtx.GlobalContext.tld = 'en';

        assert.ok(block(data));
    });

    it('should not work on iOS', function() {
        RequestCtx.GlobalContext.language = 'ru';

        RequestCtx.GlobalContext.device = {
            OSFamily: 'iOS',
            OSVersionRaw: '11',
            BrowserName: 'YandexBrowser',
            BrowserVersionRaw: '20'
        };

        assert.notOk(block(data));
    });
});
