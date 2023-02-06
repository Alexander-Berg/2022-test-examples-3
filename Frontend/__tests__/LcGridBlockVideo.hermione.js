hermione.only.in(['chrome-desktop', 'firefox']);
specs({
    feature: 'LcGridBlockVideo',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockvideo/default.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid(), {
                ignoreElements: ['.lc-video-block__video-object'],
            });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с отступами', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockvideo/with-paddings.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid(), {
                ignoreElements: ['.lc-video-block__video-object'],
            });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид с кастомным позиционированием', function() {
        return this.browser
            .url('/turbo?stub=lcgridblockvideo/with-custom-align.json')
            .yaWaitForVisible(PO.lcGridPattern(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGrid(), {
                ignoreElements: ['.lc-video-block__video-object'],
            });
    });
});
