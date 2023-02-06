specs({
    feature: 'LcSlider',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    hermione.only.notIn('safari13');
    it('Отображение следующего изображения по свайпу', function() {
        return this.browser
            .url('/turbo?stub=lcslider/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
