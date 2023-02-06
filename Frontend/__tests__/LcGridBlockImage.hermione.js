hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGridBlockImage',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockimage/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с отступами', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockimage/with-paddings.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с кастомными размерами', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockimage/with-custom-sizes.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с кастомным позиционированием', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockimage/with-custom-align.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid());
    });

    // yaCheckLinkOpener не отрабатывает корректно в FF
    hermione.only.in(['chrome-desktop']);
    hermione.only.notIn('safari13');
    it('Открытие заданной ссылки по клику', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockimage/with-link-url.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .yaCheckLinkOpener(
                PO.lcLink(),
                'Ссылка не открылась в новой вкладке',
                { target: '_blank' }
            );
    });
});
