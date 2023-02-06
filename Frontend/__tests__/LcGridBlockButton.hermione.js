hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGridBlockButton',
}, () => {
    hermione.only.notIn('safari13');
    it('Секция LcGridBlockButton', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockbutton/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Секция LcGridBlockButton c кастомной темой', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockbutton/with-custom-theme.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Секция LcGridBlockButton c кастомным выравниванием', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockbutton/with-custom-align.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Секция LcGridBlockButton c кастомными отступами', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockbutton/with-custom-text-paddings.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Секция LcGridBlockButton c кастомными скруглениями углов', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockbutton/with-custom-borders.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });
});
