specs({
    feature: 'LcSlider',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    it('Отображение следующего изображения по клику', function() {
        return this.browser
            .url('/turbo?stub=lcslider/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.page());
    });
});
