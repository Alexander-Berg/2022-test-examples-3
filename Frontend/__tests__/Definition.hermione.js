specs({
    feature: 'definition',
}, () => {
    hermione.only.notIn('safari13');
    it('Мультисписок пар термин-описание', function() {
        return this.browser
            .url('/turbo?stub=definition/multi.json')
            .yaWaitForVisible(PO.blocks.definition(), 'Мультисписок не появился на странице')
            .assertView('multi', PO.blocks.definition());
    });

    hermione.only.notIn('safari13');
    it('Одиночная пара термин-описание', function() {
        return this.browser
            .url('/turbo?stub=definition/default.json')
            .yaWaitForVisible(PO.blocks.definition(), 'Пара термин-описание не появились на странице')
            .assertView('default', PO.blocks.definition());
    });

    hermione.only.notIn('safari13');
    it('Отступ от основного контента', function() {
        return this.browser
            .url('/turbo?stub=definition/indent.json')
            .yaWaitForVisible(PO.page.result(), 'Страница не появились')
            .assertView('indent', PO.page.content());
    });
});
