specs({
    feature: 'LcGallery',
}, () => {
    hermione.only.in(['chrome-phone', 'iphone', 'searchapp', 'chrome-desktop', 'firefox']);
    it('Отображение следующего изображения по клику', function() {
        return this.browser
            .url('/turbo?stub=lcgallery/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGallery());
    });

    it('Отображение одной тумбы по центру', function() {
        return this.browser
            .url('/turbo?stub=lcgallery/oneItem.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGallery());
    });

    it('Отображение двух тумб по центру', function() {
        return this.browser
            .url('/turbo?stub=lcgallery/twoItems.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.lcGallery());
    });
});
