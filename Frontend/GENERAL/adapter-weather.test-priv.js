describeBlock('adapter-weather__extended-url', function(block) {
    it('should add query params', function() {
        const result = block('https://ya.ru/foo', { params: ['bar', 'baz'] });

        assert.strictEqual(result, 'https://ya.ru/foo?utm_source=serp&utm_campaign=helper&bar=baz');
    });

    it('should add path', function() {
        const result = block('https://ya.ru/foo', { path: '/bar/baz' });

        assert.strictEqual(result, 'https://ya.ru/foo/bar/baz?utm_source=serp&utm_campaign=helper');
    });

    it('should add path and deduplicate slashes', function() {
        const result = block('https://ya.ru//foo/', { path: '/bar/baz' });

        assert.strictEqual(result, 'https://ya.ru/foo/bar/baz?utm_source=serp&utm_campaign=helper');
    });

    it('should add path and params', function() {
        const result = block('https://yandex.ru/pogoda/?lat=52&lon=37', { path: '/details', params: ['ski', '1'] });

        assert.strictEqual(result, 'https://yandex.ru/pogoda/details?lat=52&lon=37&utm_source=serp&utm_campaign=helper&ski=1');
    });

    it('should add or replace hash', function() {
        let result = block('https://yandex.ru/pogoda/?lat=52&lon=37', { hash: 'test' });

        assert.strictEqual(result, 'https://yandex.ru/pogoda/?lat=52&lon=37&utm_source=serp&utm_campaign=helper#test');

        result = block('https://yandex.ru/pogoda/?lat=52&lon=37#baz', { hash: 'foobar' });
        assert.strictEqual(result, 'https://yandex.ru/pogoda/?lat=52&lon=37&utm_source=serp&utm_campaign=helper#foobar');
    });

    it('should add path and params and hash', function() {
        const result = block('https://yandex.ru/pogoda/?lat=52&lon=37', { path: '/details', params: ['ski', '1'], hash: 'ololo' });

        assert.strictEqual(result, 'https://yandex.ru/pogoda/details?lat=52&lon=37&utm_source=serp&utm_campaign=helper&ski=1#ololo');
    });
});
