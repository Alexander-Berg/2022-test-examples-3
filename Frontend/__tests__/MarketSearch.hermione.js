specs({
    feature: 'marketSearch',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketsearch/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.marketSection());
    });

    hermione.only.notIn('safari13');
    it('Проверка функциональности поля ввода', function() {
        return this.browser
            .url('/turbo?stub=marketsearch/default.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .setValue(PO.blocks.marketSearch.searchField(), 'phone')
            .yaWaitForVisible(PO.blocks.marketSearch.clearButton(), 'Кнопка "clear" не видна')
            .click(PO.blocks.marketSearch.clearButton())
            .getValue(PO.blocks.marketSearch.searchField())
            .then(text => {
                assert.equal(text, '', 'Поле ввода не пусто');
            })
            .yaWaitForHidden(PO.blocks.marketSearch.clearButton(), 'Кнопка "clear" видна')

            .setValue(PO.blocks.marketSearch.searchField(), ['ph', 'Backspace'])
            .yaWaitForVisible(PO.blocks.marketSearch.clearButton(), 'Кнопка "clear" не видна')
            .setValue(PO.blocks.marketSearch.searchField(), ['p', 'Backspace'])
            .yaWaitForHidden(PO.blocks.marketSearch.clearButton(), 'Кнопка "clear" видна');
    });

    hermione.only.notIn('safari13');
    it('Проверка отправки формы', function() {
        return this.browser
            .url('/turbo?stub=marketsearch/default.json')
            .yaCheckFormSubmit(
                PO.blocks.marketSearch(),
                () => this.browser.setValue(PO.blocks.marketSearch.searchField(), ['phone', 'Enter']),
                { target: '_blank' }
            )
            .then(url => {
                assert.equal(url.protocol, 'https:', 'Неверный протокол');
                assert.equal(url.host, 'm.market.yandex.ru', 'Неверный хост');
                assert.equal(url.pathname, '/search', 'Неверный хост');
                assert.deepEqual(url.query, { cvredirect: '1', clid: '927', text: 'phone' }, 'Неверные параметры');
            });
    });
});
