describe('i-cgi', function() {
    var TEST_URL = 'https://yandex.ru/search/?text=cats',
        TEST_URL_SEARCHAPP = 'https://yandex.ru/search/touch/?text=cats';

    stubBlocks('Util');

    describeBlock('i-cgi', function(block) {
        var data;

        beforeEach(function() {
            data = stubData('experiments');
            data.cgidata = {
                args: { text: ['cats'] },
                scheme: 'https',
                path: 'search/',
                hostname: 'yandex.ru',
                order: ['text']
            };
        });

        it('should provide correct url from data.cgidata', function() {
            var result = block(data);
            assert.isObject(result);
            assert.equal(result.url(), TEST_URL);
        });

        it('should hide internal params for pumpkin mode', function() {
            data.reqdata.flags.pumpkin = 1;
            data.cgidata.args['no-tests'] = ['1'];
            data.cgidata.order.push('no-tests');

            var result = block(data);
            assert.equal(result.url(), TEST_URL);
        });

        it('should hide prefetch param', function() {
            data.cgidata.args['prefetch'] = ['1'];
            data.cgidata.order.push('prefetch');

            var result = block(data);
            assert.equal(result.url(), TEST_URL);
        });

        it('should hide serp-reload-from=self-promo param', function() {
            data.cgidata.args['serp-reload-from'] = ['self-promo'];
            data.cgidata.order.push('serp-reload-from');

            var result = block(data);
            assert.equal(result.url(), TEST_URL);
        });

        it('should hide internal params for voice input on load', function() {
            data.cgidata.args.promo = ['show-voice-input', 'nomooa'];
            data.cgidata.order.push('promo');

            var result = block(data);
            assert.equal(result.url(), TEST_URL + '&promo=nomooa');
        });

        it('should hide internal params for voice queries', function() {
            data.cgidata.args.query_source = ['voice'];
            data.cgidata.order.push('query_source');

            var result = block(data);
            assert.equal(result.url(), TEST_URL);
        });

        it('should not use domain from X-Original-URL for external network', function() {
            var proxyUrl = 'http://crowdtest.yandex.ru:3456/?text=test';
            data.reqdata.headers = { 'X-Original-URL': proxyUrl };

            var result = block(data);
            assert.isObject(result);
            assert.equal(result.url(), TEST_URL);
        });

        it('should provide domain `crowdtest.yandex.ru` from `X-Original-URL` for internal network', function() {
            var proxyUrl = 'http://crowdtest.yandex.ru:3456/?text=test';
            data.reqdata.headers = { 'X-Original-URL': proxyUrl };
            data.reqdata.is_yandex_net = 1;

            var result = block(data);
            assert.isObject(result);
            assert.equal(result.url(), TEST_URL.replace('yandex.ru', 'crowdtest.yandex.ru'));
        });

        it('should provide correct url for searchapp', function() {
            data.reqdata.flags['is_searchapp'] = 1;
            data.cgidata.path = 'search/touch/';

            var result = block(data);
            assert.isObject(result);
            assert.equal(result.url(), TEST_URL_SEARCHAPP);
        });
    });

    describeBlock('i-cgi__url', function(block) {
        it('should provide correct url from data.cgidata', function() {
            var data = stubData();

            data.cgidata = {
                args: {
                    text: ['cats']
                },
                scheme: 'https',
                path: 'search/',
                hostname: 'yandex.ru',
                order: ['text']
            };

            var result = block(data);
            assert.isObject(result);
            assert.equal(result.url(), TEST_URL);
        });

        it('should replace legacy path', function() {
            var data = stubData();

            data.cgidata = {
                args: {
                    text: ['cats']
                },
                scheme: 'https',
                path: 'yandsearch',
                hostname: 'yandex.ru',
                order: ['text']
            };

            var paths = [
                { path: 'yandsearch', expected: 'search/' },
                { path: 'padsearch', expected: 'search/pad/' },
                { path: 'touchsearch', expected: 'search/touch/' },
                { path: 'search/entity/touch', expected: 'search/touch/' },
                { path: 'msearch', expected: 'search/smart/' },
                { path: 'storeclick', expected: 'search/storeclick' },
                { path: 'geoanswer', expected: 'search/geoanswer' },
                { path: 'poll-stations', expected: 'search/poll-stations' },
                { path: 'post-indexes', expected: 'search/post-indexes' },
                { path: 'promo-app', expected: 'search/promo_app' },
                { path: '', expected: '', message: 'should not change empty path' },
                { path: 'search/', expected: 'search/', message: 'should not change non-legacy path' }
            ];

            paths.forEach(({ path, expected, message }) => {
                data.cgidata.path = path;
                var result = block(data);
                assert.equal(result.url(), `https://yandex.ru/${expected}?text=cats`, message);
            });
        });
    });

    describeBlock('i-cgi__get-url', function(block) {
        var urlData;

        beforeEach(function() {
            urlData = {
                args: {},
                order: [],
                scheme: 'http',
                path: 'search/',
                hostname: 'yandex.ru'
            };
        });

        it('should provide result if called with a string parameter', function() {
            assert.equal(block(TEST_URL), TEST_URL);
        });

        it('should provide result if called with a transformed url object', function() {
            var transformed = {
                url: function() {
                    return TEST_URL;
                }
            };

            assert.equal(block(transformed), TEST_URL);
        });

        it('should correctly return url with provided scheme', function() {
            urlData.scheme = 'https';

            assert.equal(
                RequestCtx.url(block(urlData)).scheme(),
                'https'
            );
        });

        it('should correctly return url with default scheme', function() {
            urlData.scheme = null;

            assert.equal(
                RequestCtx.url(block(urlData)).scheme(),
                'http'
            );
        });

        it('should correctly return url with provided hostname', function() {
            urlData.hostname = 'google.com';

            assert.equal(
                RequestCtx.url(block(urlData)).hostname(),
                'google.com'
            );
        });

        it('should correctly return url with default hostname', function() {
            urlData.hostname = null;

            assert.equal(
                RequestCtx.url(block(urlData)).hostname(),
                'yandex.ru'
            );
        });

        it('should correctly return url with provided path', function() {
            urlData.path = 'search/infected';

            assert.equal(
                RequestCtx.url(block(urlData)).path(),
                'search/infected'
            );
        });

        it('should return "null" if path is not provided, to track such cases in functional tests', function() {
            urlData.path = null;

            assert.equal(
                RequestCtx.url(block(urlData)).path(),
                'null'
            );
        });

        it('should create querystring with correct order of parameters', function() {
            urlData.args = {
                text: ['cats'],
                lr: [213],
                baz: ['foo']
            };
            urlData.order = ['baz', 'lr', 'text'];

            assert.equal(
                RequestCtx.url(block(urlData)).queryString(),
                'baz=foo&lr=213&text=cats'
            );
        });

        it('should escape query parameters names', function() {
            urlData.args = {
                'Blah, (\'blah\'), blah!': ['cats']
            };
            urlData.order = ['Blah, (\'blah\'), blah!'];

            assert.include(
                block(urlData),
                'Blah%2C%20%28%27blah%27%29%2C%20blah%21=cats'
            );
        });

        it('should escape query parameters values', function() {
            urlData.args = {
                text: ['Blah, (\'blah\'), blah!']
            };
            urlData.order = ['text'];

            assert.equal(
                RequestCtx.url(block(urlData)).queryString(),
                'text=Blah%2C%20%28%27blah%27%29%2C%20blah%21'
            );
        });

        it('should escape whitespace with "+" signs if asked for', function() {
            urlData.args = {
                text: ['cats and dogs']
            };
            urlData.order = ['text'];
            urlData.pluses = { text: [0] };

            assert.equal(
                RequestCtx.url(block(urlData)).queryString(),
                'text=cats+and+dogs'
            );
        });

        it('should not include undefined parameters', function() {
            urlData.args = {
                text: undefined
            };
            urlData.order = ['text'];

            assert.equal(
                RequestCtx.url(block(urlData)).queryString(),
                ''
            );
        });

        it('should not include parameters not available in "order" field', function() {
            urlData.args = {
                text: 'cats'
            };
            urlData.order = [];

            assert.equal(
                RequestCtx.url(block(urlData)).queryString(),
                ''
            );
        });
    });
});
