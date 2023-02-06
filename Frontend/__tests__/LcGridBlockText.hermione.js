hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGridBlockText',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=lcgridblocktext/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с кастомным выравниванием', function() {
        return this.browser
            .url('/turbo?stub=lcgridblocktext/with-custom-align.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });
});
