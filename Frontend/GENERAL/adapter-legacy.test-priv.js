describeBlock('adapter-legacy__favicon-short-url', block => {
    it('returns shortened url from long string', () => {
        const url = 'https://ru.wikipedia.org/wiki/%D0%AF%D0%BD%D0%B4%D0%B5%D0%BA%D1%81.%D0%94%D1%80%D0%B0%D0%B9%D0%B2';

        const urlShort = block(url);

        assert.equal(urlShort, 'https://ru.wikipedia.org/wiki/');
    });

    it('returns shortened url from long string with one params', () => {
        const url = 'https://play.google.com/store/apps/details?id=com.yandex.mobile.drive|ru.yandex.mobile.drive';

        const urlShort = block(url);

        assert.equal(urlShort, 'https://play.google.com/store/apps/details');
    });

    it('returns shortened url from long string with several params', () => {
        const url = 'https://play.google.com/store/apps/details?id=com.yandex.mobile.drive&hl=ru&test1=testing&test2=testing';

        const urlShort = block(url);

        assert.equal(urlShort, 'https://play.google.com/store/apps/details?id=com.yandex.mobile.drive&hl=ru');
    });

    it('returns shortened url from string equal max length', () => {
        const url = 'https://ru.wikipedia.org/wiki/test-test/test-test/test-test/test-test/test-test/test-11/';

        const urlShort = block(url);

        assert.equal(urlShort, 'https://ru.wikipedia.org/wiki/test-test/test-test/test-test/test-test/test-test/test-11/');
    });

    it('returns shortened url from short string', () => {
        const url = 'https://ru.wikipedia.org/wiki/';

        const urlShort = block(url);

        assert.equal(urlShort, 'https://ru.wikipedia.org/wiki/');
    });
});
