specs({
    feature: 'LcTextComponent',
}, () => {
    it('Внешний вид заголовка', function() {
        return this.browser
            .url('/turbo?stub=lctextcomponent/title.json')
            .yaWaitForVisible(PO.lcTextBlock(), 'Текстовый блок с заголовком не загрузился')
            .assertView('plain', PO.lcTextBlock());
    });

    it('Внешний вид подзаголовка', function() {
        return this.browser
            .url('/turbo?stub=lctextcomponent/subtitle.json')
            .yaWaitForVisible(PO.lcTextBlock(), 'Текстовый блок с подзаголовком не загрузился')
            .assertView('plain', PO.lcTextBlock());
    });

    it('Внешний вид простого текста', function() {
        return this.browser
            .url('/turbo?stub=lctextcomponent/text.json')
            .yaWaitForVisible(PO.lcTextBlock(), 'Текстовый блок не загрузился')
            .assertView('plain', PO.lcTextBlock());
    });
});
