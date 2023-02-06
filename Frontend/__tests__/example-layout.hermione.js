specs({
    feature: 'example.layout.ru',
}, () => {
    hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Брендирование', function() {
        return this.browser
            .url('?stub=example.layout.ru%2Ffor-brand.json&brand=example.layout.ru')
            .yaWaitForVisible(PO.page())
            .assertView('plain', PO.page())
            .execute(function() {
                return document.querySelector('link[rel="shortcut icon"]').getAttribute('href');
            })
            .then(({ value: faviconUrl }) => {
                assert.equal(faviconUrl, 'https://yastatic.net/q/wiki-front-intranet-desktop/v10.63.0/_m/favicon-64.png');
            });
    });

    hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Переопределение presearch', function() {
        return this.browser
            .url('/turbo?text=https://example.layout.ru&mode=presearch&no-assets=1')
            .execute(() => document.querySelector('link[data-id="example-presearch"]').getAttribute('href'))
            .then(({ value: href }) => assert.strictEqual(href, 'https://example.layout.ru/'));
    });

    hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Брендирование с залипающей шапкой (залипание шапки отключено)', function() {
        return this.browser
            .url('?stub=example.layout.ru%2Fsticky-header.json&brand=example.layout.ru')
            .yaWaitForVisible(PO.page())
            .execute(function(headerSelector, stickySelector) {
                var element = document.querySelector(headerSelector);
                return !element.classList.contains(stickySelector);
            }, PO.header(), 'header_sticky-fixed')
            .then(({ value }) => assert(value, 'Шапка залипает, хотя брендирование должно было отключить это'));
    });

    hermione.only.in('iphone', 'Ускоряем браузеронезависимые тесты');
    hermione.only.notIn('safari13');
    it('Брендирование с залипающей шапкой (залипание шапки включено)', function() {
        return this.browser
            .url('?stub=example.layout.ru%2Fsticky-header.json&brand=example.layout.ru&header_fixed=1')
            .yaWaitForVisible(PO.page())
            .execute(function(headerSelector, stickySelector) {
                var element = document.querySelector(headerSelector);
                return element.classList.contains(stickySelector);
            }, PO.header(), 'header_sticky-fixed')
            .then(({ value }) => assert(value, 'Шапка не залипает, хотя брендирование должно было включить залипание'));
    });
});
