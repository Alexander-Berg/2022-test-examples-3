function test(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .assertView(page, PO.lcGoodGallery())
            .yaCheckClientErrors();
    };
}

function testZoom(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .moveToObject(PO.lcGoodGallery(), 200, 250)
            .assertView(`${page}WithZoom`, PO.lcGoodGallery(), {
                screenshotDelay: 500,
            });
    };
}

function testThumbClick(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .click(PO.lcGoodGallery.thumb())
            .assertView(`${page}WithThumbClick`, PO.lcGoodGallery(), {
                screenshotDelay: 500,
            });
    };
}

function testThumbButtonClick(page) {
    return function() {
        return this.browser
            .url(`/turbo?stub=lcgoodgallery/${page}.json`)
            .yaWaitForVisible(PO.lcGoodGallery(), 'Блок не загрузился')
            .click(PO.lcGoodGallery.thumbButtonDown())
            .assertView(`${page}WithThumbButtonClick`, PO.lcGoodGallery(), {
                screenshotDelay: 500,
            });
    };
}

specs({
    feature: 'LcGoodGallery',
}, () => {
    it('Внешний вид блока. Одно изображение, desktop', test('desktopOneImage'));
    it('Внешний вид блока. Два изображения, desktop', test('desktopTwoImages'));
    it('Внешний вид блока. Много изображений, desktop', test('desktopManyImages'));
    it('Зум при наведении. Одно изображение, desktop', testZoom('desktopOneImage'));
    it('Выбор тумбы по клику. Два изображения, desktop', testThumbClick('desktopTwoImages'));
    it('Проскролл тумб вниз при клике на кнопку проскролла. Много изображений, desktop', testThumbButtonClick('desktopManyImages'));
});
