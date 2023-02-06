describeBlock('service-path', function(block) {
    var context = {
        tld: 'ru',
        query: {},
        reportData: {
            language: 'ru',
            prefs: {},
            reqdata: {
                user_region: {}
            }
        }
    };

    it('return path by passed id', function() {
        assert.deepEqual(block(context, { id: 'music' }), {
            id: 'music',
            path: {
                block: 'bold',
                text: 'Яндекс.Музыка'
            }
        });
    });

    it('return bold path by passed path', function() {
        assert.deepEqual(block(context, { path: 'Яндекс.Музыка' }), {
            path: {
                block: 'bold',
                text: 'Яндекс.Музыка'
            }
        });
    });

    every([
        'music.yandex.ru/genre',
        'https://yandex.ru/music/touch/search/bla/bla/bla',
        '//m.music.yandex.ru',
        'http://music.yandex.ru',
        'https://www.mobile.yandex.ru/music',
        'https://app.mobile.yandex.ru/music',
        'https://www.app.mobile.yandex.ru/music',
        'https://mobile.yandex.ru/music'
    ], 'return path by passed url', function(url) {
        assert.deepEqual(block(context, { url }), {
            id: 'music',
            path: {
                block: 'bold',
                text: 'Яндекс.Музыка'
            }
        });
    });
});

describeBlock('service-path__id-to-text', function(block) {
    every(['adv', 'news', 'xml'], 'should return service name for known services', function(id) {
        assert.isString(block(id));
    });

    every(['ololo', 'constructor', 'toString'], 'should return undefined for unknown services', function(id) {
        assert.isUndefined(block(id));
    });
});

describeBlock('service-path__process', function(block) {
    it('return path by passed url', function() {
        assert.deepEqual(block('https://www.yandex.ru/search/direct?'), 'direct');
    });
});
