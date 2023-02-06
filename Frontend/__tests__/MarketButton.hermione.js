specs({
    feature: 'marketButton',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketbutton/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.marketButton(), 'На странице нет компонента MarketButton')
            .assertView('link', PO.blocks.marketButtonTypeLink())
            .assertView('theme_call_to_action', PO.blocks.marketButtonCallToAction())
            .assertView('button', PO.blocks.marketButtonTypeButton())
            .assertView('disabled_button', PO.blocks.marketButtonTypeDisabledButton())
            .assertView('small', PO.blocks.marketButtonSmall())
            .assertView('fullwidth', PO.blocks.marketButtonFullWidth());
    });
});
