function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .assertView(page, PO.lcGoodGallery(), {
                allowViewportOverflow: true,
            })
            .yaCheckClientErrors();
    };
}

function testWithBulletClick(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .click(PO.lcGoodGallery.thumb())
            .assertView(`${page}WithBulletClick`, PO.lcGoodGallery(), {
                screenshotDelay: 500,
                allowViewportOverflow: true,
            });
    };
}

specs({
    feature: 'LcGoodGallery',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Одно изображение, phone', test('phoneOneImage'));
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Много изображений, phone', test('phoneManyImages'));
    hermione.only.notIn('safari13');
    it('Переключение изображения. Много изображений, phone', testWithBulletClick('phoneManyImages'));
});
