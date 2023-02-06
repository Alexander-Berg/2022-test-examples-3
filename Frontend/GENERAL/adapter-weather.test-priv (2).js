describeBlock('adapter-weather__wrapp', function(block) {
    let context;
    let snippet;
    let isSupportedByBrowser;
    let glob;

    beforeEach(function() {
        context = {
            expFlags: {},
            device: {
                OSFamily: 'Android',
                OSVersionRaw: '10',
                BrowserName: 'YandexBrowser',
                BrowserVersionRaw: '19.12.1.121.00'
            },
            templatePlatform: 'touch-phone'
        };
        snippet = {
            weather_link: 'https://yandex.ru/pogoda/opochka',
            subtype: 'today'
        };

        glob = stubGlobal('RequestCtx');
        isSupportedByBrowser = sinon.stub(RequestCtx.TurboParams, 'isSupportedByBrowser').returns(true);
    });

    afterEach(function() {
        isSupportedByBrowser.restore();
        glob.restore();
    });

    it('should enrich sideblock_cgi_url', function() {
        snippet.sideblock_cgi_url = 'https://yandex.ru/pogoda/opochka';
        const json = block(context, snippet, {}, []);
        assert.strictEqual(json.url.url, 'https://yandex.ru/pogoda/opochka?utm_source=serp&utm_campaign=helper&utm_medium=touch&utm_content=helper_today&utm_term=title');
    });

    it('should set external link to title_link when it is specified', function() {
        snippet.title_link = 'https://yandex.ru/pogoda/opochka/maps/nowcast';
        const json = block(context, snippet, {}, []);
        assert.strictEqual(json.url.url, 'https://yandex.ru/pogoda/opochka/maps/nowcast?utm_source=serp&utm_campaign=helper&utm_medium=touch&utm_content=helper_today&utm_term=title');
    });

    it('should set external link to weather_link when flag exists', function() {
        snippet.sideblock_cgi_url = 'https://yandex.ru/pogoda/opochka';
        snippet.turbo_link = 'https://yandex.ru/turbo?text=https%3A%2F%2Fyandex.ru%2Fpogoda%2Fopochka';
        context.expFlags['GEO_wizweather_turbo_tab'] = 1;
        const json = block(context, snippet, {}, []);
        assert.strictEqual(json.url.url, 'https://yandex.ru/pogoda/opochka?utm_source=serp&utm_campaign=helper&utm_medium=touch&utm_content=helper_today&utm_term=title');
        assert.isUndefined(json.url.turbo);
    });

    it('should degrade to weather_link when no nor sideblock_cgi_url nor turbo_link', function() {
        const json = block(context, snippet, {}, []);
        assert.strictEqual(json.url.url, 'https://yandex.ru/pogoda/opochka?utm_source=serp&utm_campaign=helper&utm_medium=touch&utm_content=helper_today&utm_term=title');
        assert.isUndefined(json.url.turbo);
    });
});
