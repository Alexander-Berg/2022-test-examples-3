specs({
    feature: 'suggest',
    type: 'ecom',
}, () => {
    hermione.only.notIn('safari13');
    it('Сабмит по крестику в сложенном состоянии', function() {
        return this.browser
            .url('/turbo?stub=suggest/type-ecom.json')
            .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
            .click(PO.suggest.miniSuggest.inputClear())
            .yaWaitUntil('Урл страницы ведет на /turbo?text=about', () =>
                this.browser
                    .getUrl()
                    .then(url => url.includes('text=about'))
            )
            .getUrl()
            .then(url => {
                assert.include(url, 'text=about&param=param1&param=param2&query=&');
            });
    });

    hermione.only.notIn('safari13');
    it('Сабмит по Найти со значением', function() {
        return this.browser
            .url('/turbo?stub=suggest/type-ecom.json')
            .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
            .click(PO.suggest.miniSuggest.opener())
            .click(PO.suggest.miniSuggest.searchButton())
            .yaWaitUntil('Урл страницы ведет на /turbo?text=about', () =>
                this.browser
                    .getUrl()
                    .then(url => url.includes('text=about'))
            )
            .getUrl()
            .then(url => {
                assert.include(url, 'text=about&param=param1&param=param2&query=iphone');
            });
    });

    hermione.only.notIn('safari13');
    it('Сабмит по Найти без значения', function() {
        return this.browser
            .url('/turbo?stub=suggest/type-ecom.json')
            .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
            .click(PO.suggest.miniSuggest.opener())
            .touch(PO.suggest.miniSuggest.inputClear())
            .click(PO.suggest.miniSuggest.searchButton())
            .yaWaitUntil('Урл страницы ведет на /turbo?text=about', () =>
                this.browser
                    .getUrl()
                    .then(url => url.includes('text=about'))
            )
            .getUrl()
            .then(url => {
                assert.include(url, 'text=about&param=param1&param=param2&query=');
            });
    });

    describe('Цели', function() {
        hermione.only.notIn('safari13');
        it('Сабмит по крестику в сложенном состоянии', function() {
            return this.browser
                .url('/turbo?stub=suggest/type-ecom-blank.json&exp_flags=analytics-disabled=0')
                .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
                .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
                .touch(PO.suggest.miniSuggest.inputClear())
                .yaCheckMetrikaGoals({
                    '11111111[1]': ['products-search-submit2', 'products-search-submit'],
                    '22222222[1]': ['products-search-submit'],
                });
        });

        hermione.only.notIn('safari13');
        it('Сабмит по Найти со значением', function() {
            return this.browser
                .url('/turbo?stub=suggest/type-ecom-blank.json&exp_flags=analytics-disabled=0')
                .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
                .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
                .click(PO.suggest.miniSuggest.opener())
                .click(PO.suggest.miniSuggest.searchButton())
                .yaCheckMetrikaGoals({
                    '11111111[1]': ['products-search-submit2', 'products-search-submit'],
                    '22222222[1]': ['products-search-submit'],
                });
        });

        hermione.only.notIn('safari13');
        it('Сабмит по Найти без значения', function() {
            return this.browser
                .url('/turbo?stub=suggest/type-ecom-blank.json&exp_flags=analytics-disabled=0')
                .yaWaitForVisible(PO.suggest(), 'Блок suggest не появился на странице')
                .yaWaitForVisible('.metrika_loaded', 'Не загрузилась метрика')
                .click(PO.suggest.miniSuggest.opener())
                .touch(PO.suggest.miniSuggest.inputClear())
                .click(PO.suggest.miniSuggest.searchButton())
                .yaCheckMetrikaGoals({
                    '11111111[1]': ['products-search-submit2', 'products-search-submit'],
                    '22222222[1]': ['products-search-submit'],
                });
        });
    });
});
