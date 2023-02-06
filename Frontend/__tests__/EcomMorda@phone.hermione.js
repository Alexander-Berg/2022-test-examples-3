specs({
    feature: 'EcomMorda',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид', function() {
        return this.browser
            .url('/turbo?stub=ecommorda/default.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', [PO.page()], {
                ignoreElements: [
                    PO.blocks.ecomHeader(),
                    PO.blocks.formPresetSearch(),
                    PO.blocks.categories(),
                    PO.blocks.sliderThemeEcomBanner(),
                    PO.blocks.products(),
                    PO.blocks.productsCarousel.turboNativeScroll(),
                    PO.blocks.ecomFooter(),
                ],
            });
    });

    hermione.only.notIn('safari13');
    it('Без скидок', function() {
        return this.browser
            .url('/turbo?stub=ecommorda/without-sales.json')
            .yaWaitForVisible(PO.page())
            .assertView('plain', [PO.page()], {
                ignoreElements: [
                    PO.blocks.ecomHeader(),
                    PO.blocks.formPresetSearch(),
                    PO.blocks.categories(),
                    PO.blocks.sliderThemeEcomBanner(),
                    PO.blocks.products(),
                    PO.blocks.productsCarousel.turboNativeScroll(),
                    PO.blocks.ecomFooter(),
                ],
            });
    });
});
