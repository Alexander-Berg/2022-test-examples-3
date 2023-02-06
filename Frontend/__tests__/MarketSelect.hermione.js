specs({
    feature: 'marketSelect',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketselect/default.json')
            .yaWaitForVisible(PO.pageJsInited(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.marketSelect());
    });

    hermione.only.notIn('safari13');
    it('Выбор значения', function() {
        return this.browser
            .url('/turbo?stub=marketselect/default.json')
            .yaWaitForVisible(PO.pageJsInited())
            .click(PO.blocks.marketSelect())
            .click(PO.blocks.marketSelect.optionSecond())
            .assertView('short-value', PO.blocks.marketSelect())
            .click(PO.blocks.marketSelect())
            .click(PO.blocks.marketSelect.optionThird())
            .assertView('new-value1', PO.blocks.marketSelect());
    });
});
