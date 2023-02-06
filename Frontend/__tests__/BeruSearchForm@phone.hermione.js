specs({
    feature: 'beru-search-form',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=berusearchform/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.beruSearchForm());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока в заполненном состоянии и в фокусе', function() {
        return this.browser.url('/turbo?stub=berusearchform/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .setValue(PO.blocks.beruSearchForm.searchField(), 'phone')
            .assertView('filled-focused-form', PO.blocks.beruSearchForm());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока в заполненном состоянии без фокуса', function() {
        return this.browser.url('/turbo?stub=berusearchform/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .setValue(PO.blocks.beruSearchForm.searchField(), 'fgfdg')
            .click('.beru-fake-block')
            .assertView('filled-form', PO.blocks.beruSearchForm());
    });

    hermione.only.notIn('safari13');
    it('Проверка функциональности поля ввода', function() {
        return this.browser
            .url('/turbo?stub=berusearchform/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .setValue(PO.blocks.beruSearchForm.searchField(), 'phone')
            .yaWaitForVisible(PO.blocks.beruSearchForm.clearButton(), 'Кнопка "clear" не видна')
            .click(PO.blocks.beruSearchForm.clearButton())
            .getValue(PO.blocks.beruSearchForm.searchField())
            .then(text => {
                assert.equal(text, '', 'Поле ввода не пусто');
            })
            .yaWaitForHidden(PO.blocks.beruSearchForm.clearButton(), 'Кнопка "clear" видна')

            .setValue(PO.blocks.beruSearchForm.searchField(), ['ph', 'Backspace'])
            .yaWaitForVisible(PO.blocks.beruSearchForm.clearButton(), 'Кнопка "clear" не видна')
            .setValue(PO.blocks.beruSearchForm.searchField(), ['p', 'Backspace'])
            .yaWaitForHidden(PO.blocks.beruSearchForm.clearButton(), 'Кнопка "clear" видна');
    });
});
