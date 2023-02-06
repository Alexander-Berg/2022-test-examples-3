specs({
    feature: 'LcSlider',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=lcslider/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSection());
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока c длинным описанием', function() {
        return this.browser
            .url('/turbo?stub=lcslider/longDescription.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcSection());
    });
});
