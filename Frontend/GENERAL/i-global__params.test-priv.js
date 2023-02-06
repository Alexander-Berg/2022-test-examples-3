describeBlock('i-global__params-disable-noopener', function(block) {
    var data;

    beforeEach(function() {
        data = stubData('cgi', 'experiments');
    });

    it('should return true for Chromium-based browsers until v54', function() {
        data.reqdata.device_detect.BrowserBase = 'Chromium';
        data.reqdata.device_detect.BrowserBaseVersion = '53.1.0.125';

        assert.isTrue(block(data));
    });

    it('should return false for Chromium-based browsers since v54', function() {
        data.reqdata.device_detect.BrowserBase = 'Chromium';
        data.reqdata.device_detect.BrowserBaseVersion = '54.1.0.125';

        assert.isFalse(block(data));
    });

    it('should return false for not Chromium-based browsers', function() {
        data.reqdata.device_detect.BrowserBase = 'Safari';
        data.reqdata.device_detect.BrowserBaseVersion = '53.1.0.125';

        assert.isFalse(block(data));
    });
});

describeBlock('i-global__params-get-images-query-params-to-proxy', function(block) {
    let glob;
    let data;

    beforeEach(function() {
        data = stubData('cgi');
        data.cgidata = {};
        glob = stubGlobal('RequestCtx');
    });

    afterEach(function() {
        glob.restore();
    });

    it('Возвращает список флагов, если они присутствуют в запросе', function() {
        const params = {
            lr: ['146'],
            exp_flags: ['flag=1', 'flag=2'],
            text: ['котики+картинки']
        };
        RequestCtx.GlobalContext.cgi = {
            has: param => Boolean(params[param]),
            getParam: param => params[param]
        };

        assert.deepEqual(block(data), [['lr', '146'], ['exp_flags', 'flag=1'], ['exp_flags', 'flag=2']]);
    });

    it('Возвращает пустой список, если нет параметров', function() {
        RequestCtx.GlobalContext.cgi = {
            has: () => false,
            getParam: function(param) {
                return undefined;
            }
        };

        assert.deepEqual(block(data), []);
    });

    it('Возвращает расширеный список для ПП', function() {
        const params = {
            lr: ['146'],
            exp_flags: ['flag=1', 'flag=2'],
            text: ['котики+картинки'],
            service: ['web4'],
            ver: ['2'],
            ui: ['searchapp.plugin'],
            app_version: ['v123']
        };
        RequestCtx.GlobalContext.isSearchApp = true;
        RequestCtx.GlobalContext.cgi = {
            has: param => Boolean(params[param]),
            getParam: param => params[param]
        };

        assert.deepEqual(block(data), [
            ['lr', '146'],
            ['exp_flags', 'flag=1'],
            ['exp_flags', 'flag=2'],
            ['app_version', 'v123'],
            ['ver', '2'],
            ['ui', 'searchapp.plugin'],
            ['service', 'images.yandex']
        ]);
    });

    it('Возвращает расширеный список для ПП и при выставленом appsearchHeader', function() {
        const params = {
            lr: ['146'],
            exp_flags: ['flag=1', 'flag=2'],
            text: ['котики+картинки'],
            service: ['web4'],
            ver: ['2'],
            ui: ['searchapp.plugin'],
            app_version: ['v123']
        };
        RequestCtx.GlobalContext.isSearchApp = true;
        RequestCtx.GlobalContext.cgi = {
            has: param => Boolean(params[param]),
            getParam: param => params[param]
        };
        data.isAppSearchHeader = true;

        assert.deepEqual(block(data), [
            ['lr', '146'],
            ['exp_flags', 'flag=1'],
            ['exp_flags', 'flag=2'],
            ['app_version', 'v123'],
            ['ver', '2'],
            ['ui', 'searchapp.plugin'],
            ['service', 'images.yandex'],
            ['appsearch_header', '1']
        ]);
    });

    it('Возвращает lang, даже если его нет в cgi', function() {
        const params = {
            lr: ['146'],
            exp_flags: ['flag=1', 'flag=2']
        };
        RequestCtx.GlobalContext.isSearchApp = true;
        RequestCtx.GlobalContext.cgi = {
            has: param => Boolean(params[param]),
            getParam: param => params[param]
        };
        data.cgidata.text = 'lr=146&exp_flags=flag=1&exp_flags=flag=2&lang=ru-RU';

        assert.deepEqual(block(data), [
            ['lr', '146'],
            ['exp_flags', 'flag=1'],
            ['exp_flags', 'flag=2'],
            ['service', 'images.yandex'],
            ['lang', 'ru-RU']
        ]);
    });
});

describeBlock('i-global__params-get-images-headers-to-proxy', function(block) {
    let data;

    beforeEach(function() {
        data = stubData();
    });

    it('Возвращает список заголовков, если они присутствуют в запросе', function() {
        data.reqdata.headers = {
            foo: '1',
            bar: 'baz',
            'Serp-Web-Verticals': '1',
            'serp-web-verticals': '1'
        };

        assert.deepEqual(block(data), [['Serp-Web-Verticals', '1'], ['serp-web-verticals', '1']]);
    });

    it('Возвращает пустой список заголовков, если нет совпадений', function() {
        data.reqdata.headers = {
            foo: '1',
            bar: 'baz'
        };

        assert.deepEqual(block(data), []);
    });
});
