specs({
    feature: 'LcFooterColumns',
}, () => {
    it('С двумя уровнями', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcFooterColumns());
    });

    it('Без нижнего уровня', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/no-bottom-level.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('no-bottom-level', PO.lcFooterColumns());
    });

    it('Без верхнего уровня', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/no-top-level.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('no-top-level', PO.lcFooterColumns());
    });

    it('Без заголовка в первой колонке', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/no-header-in-first-column.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('no-header-in-first-column', PO.lcFooterColumns());
    });

    it('С шириной заданной вручную', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/with-manual-width.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('with-manual-width', PO.lcFooterColumns());
    });
});
