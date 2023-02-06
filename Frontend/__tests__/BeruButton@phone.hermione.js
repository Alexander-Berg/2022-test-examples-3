specs({
    feature: 'beruButton',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=berubutton/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока с различными размерами', function() {
        return this.browser
            .url('/turbo?stub=berubutton/sizes.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('sizes', PO.page());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид кнопки в состоянии "загрузки" во всех возможных размерах', function() {
        return this.browser
            .url('/turbo?stub=berubutton/load.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('load', PO.page());
    });
});
