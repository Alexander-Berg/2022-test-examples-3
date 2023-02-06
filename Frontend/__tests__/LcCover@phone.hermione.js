specs({
    feature: 'LcCover',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока без картинки', function() {
        return this.browser
            .url('/turbo?stub=lccover/textOnTopWithoutImage.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('withoutImage', PO.lcCover());
    });
    hermione.only.notIn('safari13');
    it('Внешний вид блока с картинкой', function() {
        return this.browser
            .url('/turbo?stub=lccover/textOnRightWithImage.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('withImage', PO.lcCover());
    });
});
