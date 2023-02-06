describeBlock('adapter-companies__get-pretty-url', function(block) {
    it('should remove trailing slash', function() {
        assert.strictEqual(block('yandex.ru/'), 'yandex.ru');
    });

    it('should remove http/https protocol', function() {
        assert.strictEqual(block('http://yandex.ru'), 'yandex.ru');
        assert.strictEqual(block('https://yandex.ru'), 'yandex.ru');
    });

    it('should remove www prefix', function() {
        assert.strictEqual(block('www.yandex.ru'), 'yandex.ru');
    });

    it('should remove http protocol with www prefix', function() {
        assert.strictEqual(block('http://www.yandex.ru'), 'yandex.ru');
    });

    it('should not remove not-http/https protocol', function() {
        assert.strictEqual(block('ftp://yandex.ru'), 'ftp://yandex.ru');
    });

    it('should not remove not-www prefix', function() {
        assert.strictEqual(block('www1.yandex.ru'), 'www1.yandex.ru');
    });

    it('should decode url path', function() {
        assert.strictEqual(block('yandex.ru/%D0%A3%D1%81%D0%B0%D0%B4%D1%8C%D0%B1%D0%B0'), 'yandex.ru/Усадьба');
    });

    it('should return undefined if url path has not-utf-8 encoding', function() {
        assert.isUndefined(block('yandex.ru/%D3%F1%E0%E4%FC%E1%E0'));
    });

    it('should correctly remove utm if he is the only one', function() {
        assert.strictEqual(block('https://easyclean24.ru/?utm_campaign=spec&utm_source=yandex'), 'easyclean24.ru');
    });

    it('should correctly remove utm and leave other cgi parameters', function() {
        assert.strictEqual(block('https://easyclean24.ru/?utm_campaign=spec&cgi1=value1&utm_source=yandex&cgi2=value2'), 'easyclean24.ru/?cgi1=value1&cgi2=value2');
        assert.strictEqual(block('https://yandex.ru/?ostutm_page=contacts'), 'yandex.ru/?ostutm_page=contacts');
    });

    it('should correctly remove utm with dot in value', function() {
        assert.strictEqual(block('https://easyclean24.ru/?utm_medium=display&utm_source=yandex.sprav&utm_term=moskva'), 'easyclean24.ru');
    });

    it('should correctly remove utm with hyphen in value', function() {
        assert.strictEqual(block('https://www.mvideo.ru/?utm_campaign=ipr_Flight_Msk_YandexGeo_Priority_01.02-21-30.04.21&utm_content=main&utm_medium=cpm&utm_source=Yandex_maps'), 'mvideo.ru');
    });
});
