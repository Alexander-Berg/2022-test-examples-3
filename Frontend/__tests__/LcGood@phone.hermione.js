specs({
    feature: 'LcGood',
}, () => {
    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Размер XS', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneSizeXS.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Размер S', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneSizeS.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Размер M', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneSizeM.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Размер L', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneSizeL.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Без описания', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneEmptyDescription.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. С длинным текстом у кнопки', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneLongButtonCaption.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. С длинным заголовком', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneLongCaption.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.page())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. С длинным описанием', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneLongDescription.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Формат изображения 4х5', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneRatio4x5.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. С коротким описанием', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneShortDescription.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Без скидки', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneWithoutDiscount.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });

    hermione.only.in('iphone', 'В остальных браузерах тесты проходят очень нестабильно');
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Без изображения', function() {
        return this.browser
            .url('/turbo?stub=lcgood/phoneWithoutImage.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .assertView('plain', PO.lcGood())
            .yaCheckClientErrors();
    });
});
