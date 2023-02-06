specs({
    feature: 'beruExpandableText',
}, () => {
    beforeEach(function() {
        return this.browser.url('/turbo?stub=beruexpandabletext/default.json');
    });

    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.beruExpandableText());
    });

    hermione.only.notIn('safari13');
    it('Разворачивание полного текста по клику', function() {
        return this.browser
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .assertView('plain', PO.blocks.beruExpandableText())
            .click(PO.blocks.beruExpandableText.button())
            .yaWaitForHidden(PO.blocks.beruExpandableText.button(), 'Должен быть виден полный текст без кнопки')
            .assertView('full-text', PO.blocks.beruExpandableText());
    });
});
