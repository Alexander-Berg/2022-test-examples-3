specs({
    feature: 'beruSkuBuyPanel',
}, () => {
    hermione.only.notIn('safari13');
    it('Базовый вид блока', function() {
        return this.browser
            .url('/turbo?stub=beruskubuypanel/default.json')
            .yaWaitForVisible(PO.blocks.beruSkuBuyPanel(), 'Блок не появился')
            .assertView('default', PO.blocks.beruSkuBuyPanel.mainButtonContainer());
    });

    hermione.only.notIn('safari13');
    it('По умолчанию скрыт, появляется только при отсутствии кнопки "В корзину (основная)"', function() {
        return this.browser
            .url('/turbo?stub=beruskubuypanel/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не появилась')
            .yaTouchScroll(PO.page.container(), 900)
            .yaWaitForVisible(PO.blocks.beruSkuBuyPanel.stickyWrapper(), 'Блок не появился')
            .assertView('stickyWrapper', PO.blocks.beruSkuBuyPanel.stickyWrapper());
    });

    hermione.only.notIn('safari13');
    it('Проверка появления блока при скролле в iframe', function() {
        return this.browser
            .yaOpenInIframe('?stub=beruskubuypanel/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не появилась')
            .yaTouchScroll(PO.page.container(), 1000)
            .yaWaitForVisible(PO.blocks.beruSkuBuyPanel.stickyWrapper(), 'Блок не появился');
    });
});
