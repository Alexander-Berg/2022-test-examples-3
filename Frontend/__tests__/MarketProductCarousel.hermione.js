specs({
    feature: 'MarketProductCarousel',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=marketproductcarousel%2Fdefault.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaCheckClientErrors()
            .yaWaitForVisible(PO.blocks.marketProductCarousel(), 'Компонент карусельки с карточками моделек')
            .assertView('plain', PO.blocks.marketProductCarousel());
    });
});
