specs({
    feature: 'LcFooterColumns',
}, () => {
    it('Плитка', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/tile.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('tile', PO.lcFooterColumns());
    });

    it('Расхлоп', function() {
        return this.browser
            .url('/turbo?stub=lcfootercolumns/with-spoilers.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('spoilers', PO.lcFooterColumns());
    });
});
