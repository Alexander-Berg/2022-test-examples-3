specs({
    feature: 'LcMenuBlock',
}, () => {
    describe('Количество колонок', function() {
        hermione.only.notIn('safari13');
        it('одна', function() {
            return this.browser
                .url('/turbo?stub=lcmenublock/columns-1.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('columns-1', PO.lcMenuBlock());
        });
        hermione.only.notIn('safari13');
        it('две', function() {
            return this.browser
                .url('/turbo?stub=lcmenublock/columns-2.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('columns-2', PO.lcMenuBlock());
        });
        hermione.only.notIn('safari13');
        it('три', function() {
            return this.browser
                .url('/turbo?stub=lcmenublock/columns-3.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('columns-3', PO.lcMenuBlock());
        });
        hermione.only.notIn('safari13');
        it('четыре', function() {
            return this.browser
                .url('/turbo?stub=lcmenublock/columns-4.json')
                .yaWaitForVisible(PO.page(), 'Блок не загрузился')
                .assertView('columns-4', PO.lcMenuBlock());
        });
    });

    hermione.only.notIn('safari13');
    it('Отображаются разделители и стрелки', function() {
        return this.browser
            .url('/turbo?stub=lcmenublock/default.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('default', PO.lcMenuBlock());
    });

    hermione.only.notIn('safari13');
    it('Шрифт YS Display', function() {
        return this.browser
            .url('/turbo?stub=lcmenublock/with-display-font.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('with-display-font', PO.lcMenuBlock());
    });

    hermione.only.notIn('safari13');
    it('Ссылка с пункта меню', function() {
        return this.browser
            .url('/turbo?stub=lcmenublock/with-link.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('with-link', PO.lcMenuBlock());
    });

    hermione.only.notIn('safari13');
    it('Без иконок', function() {
        return this.browser
            .url('/turbo?stub=lcmenublock/empty-icon.json')
            .yaWaitForVisible(PO.page(), 'Блок не загрузился')
            .assertView('empty-icon', PO.lcMenuBlock());
    });
});
