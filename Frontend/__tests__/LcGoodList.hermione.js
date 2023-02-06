specs({
    feature: 'LcGoodList',
}, () => {
    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Одна колонка', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withOneColumn.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Две колонки', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withTwoColumns.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Три колонки', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withThreeColumns.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Четыре колонки', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withFourColumns.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .yaScrollPageToBottom()
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Длинный текст в кнопке', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withLongButtonCaption.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop', 'firefox', 'iphone']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Превью изображений', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withImagePreview.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.lcGoodList())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Контрастный фон и картинка товара', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withContrastBackground.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.page())
            .yaCheckClientErrors();
    });

    hermione.only.in(['chrome-desktop']);
    hermione.only.notIn('safari13');
    it('Внешний вид блока. Светлая картинка товара на светлом фоне', function() {
        return this.browser
            .url('/turbo?stub=lcgoodlist/withLightBackground.json')
            .yaWaitForVisible(PO.lcGoodList(), 'Блок не загрузился')
            .assertView('plain', PO.page())
            .yaCheckClientErrors();
    });
});
