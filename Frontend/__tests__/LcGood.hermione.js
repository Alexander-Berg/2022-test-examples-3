specs({
    feature: 'LcGood',
}, () => {
    hermione.only.notIn('safari13');
    it('Карточка товара', function() {
        return this.browser
            .url('/turbo?stub=lcgood/card.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .click(PO.lcGood.paranja())
            .assertView('plain', PO.lcGoodModal(), {
                screenshotDelay: 100,
                compositeImage: true,
            })
            .yaCheckClientErrors();
    });

    hermione.only.notIn('safari13');
    it('Карточка товара с свойствами', function() {
        return this.browser
            .url('/turbo?stub=lcgood/cardWithAttributes.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .click(PO.lcGood.paranja())
            .assertView('plain', PO.lcGoodModal(), {
                screenshotDelay: 100,
                compositeImage: true,
            });
    });

    hermione.only.in('chrome-desktop');
    hermione.only.notIn('safari13');
    it('Выбор свойств в карточке товара', function() {
        return this.browser
            .url('/turbo?stub=lcgood/cardWithAttributes.json')
            .yaWaitForVisible(PO.lcGood(), 'Блок не загрузился')
            .click(PO.lcGood.paranja())
            .click(PO.lcGoodCard.sizeRadio())
            .click(PO.lcGoodCard.colorRadio())
            .click(PO.lcGoodCard.customAttributeSelect())
            .click(PO.lcGoodCard.customAttributeValue())
            .assertView('plain', PO.lcGoodModal(), {
                screenshotDelay: 500,
            });
    });
});
