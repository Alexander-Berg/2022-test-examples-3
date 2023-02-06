describeBlock('yandex-apps-api__backend-early-start', block => {
    let data;

    beforeEach(() => {
        RequestCtx.GlobalContext.isSearchApp = true;
        RequestCtx.GlobalContext.platform = 'android';
        data = stubData();
        data = {
            config: { staticHost: '' },
            reqdata: { headers: {} }
        };
    });

    it('should add "webview-intercepted" script when serp-bropp-script header exists', () => {
        data.reqdata.headers = { 'serp-bropp-script': true };

        assert.include(block(data).url, 'webview-intercepted');
    });

    it('should add "webview-intercepted" script when Serp-Bropp-Script header exists', () => {
        data.reqdata.headers = { 'Serp-Bropp-Script': true };

        assert.include(block(data).url, 'webview-intercepted');
    });

    it('should not add "webview-intercepted" script when serp-bropp-script header does not exist and not BroPP', () => {
        RequestCtx.GlobalContext.device.BrowserVersionRaw = 6;

        assert.include(block(data).url, 'webview-intercepted');
    });

    it('should add "webview-intercepted" script when serp-bropp-script header does not exist and is BroPP', () => {
        RequestCtx.GlobalContext.ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.60 YaApp_Android/20.112 YaSearchBrowser/20.112 BroPP/2.5 Mobile Safari/537.36';

        assert.isUndefined(block(data));
    });

    it('should add "webview-intercepted" script when serp-bropp-script header does not exist and is old YandexSearch', () => {
        RequestCtx.GlobalContext.ua = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.60 YaApp_Android/20.112 YaSearchBrowser/20.112 Mobile Safari/537.36';

        assert.notExists(block(data).url);
    });
});
