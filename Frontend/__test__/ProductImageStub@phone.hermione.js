specs({
    feature: 'ProductImageStub',
}, () => {
    hermione.only.notIn('safari13');
    it('Должен вставляться на листинге', function() {
        return this.browser
            .url('/turbo?stub=productspage%2Findex.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.blocks.products.item())
            .yaWaitForVisible(PO.blocks.products.item0.imageStub(), 'У первого товара не стаб-картинка')
            // Для контроля размера изображения
            .assertView('product-item', PO.blocks.products.item0());
    });

    hermione.only.notIn('safari13');
    it('Должен вставляться в карточке товара', function() {
        return this.browser
            .url('/turbo?stub=productpage%2Fproduct-1.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.productImageStub(), 'У товара без картинки не появился стаб')
            // Для контроля размера изображения
            .assertView('image', PO.blocks.productImageStub());
    });

    hermione.only.notIn('safari13');
    it('Должен вставляться в корзине', function() {
        return this.browser
            .url('/turbo?stub=productimagestub%2Fin-cart.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .yaWaitForVisible(PO.blocks.cart.list.firstItem.thumbLink.imageStub(), 'У первого товара нет стаба картинки')
            // Для контроля размера изображения
            .assertView('cart-list', PO.cart.list());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Должен вставляться на листинге (landscape)', function() {
        return this.browser
            .setOrientation('landscape')
            .url('/turbo?stub=productspage%2Findex.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaIndexify(PO.blocks.products.item())
            .yaWaitForVisible(PO.blocks.products.item0.imageStub(), 'У первого товара не стаб-картинка')
            // Для контроля размера изображения
            .assertView('product-item', PO.blocks.products.item0());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Должен вставляться в карточке товара (landscape)', function() {
        return this.browser
            .setOrientation('landscape')
            .url('/turbo?stub=productpage%2Fproduct-1.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.blocks.productImageStub(), 'У товара без картинки не появился стаб')
            // Для контроля размера изображения
            .assertView('image', PO.blocks.productImageStub());
    });

    hermione.only.in('chrome-phone', 'setOrientation() используем только в chrome-phone');
    hermione.only.notIn('safari13');
    it('Должен вставляться в корзине (landscape)', function() {
        return this.browser
            .setOrientation('landscape')
            .url('/turbo?stub=productimagestub%2Fin-cart.json&exp_flags=platform%3Dtouch')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaWaitForVisible(PO.cart.list(), 'Не загрузился список товаров')
            .yaWaitForVisible(PO.blocks.cart.list.firstItem.thumbLink.imageStub(), 'У первого товара нет стаба картинки')
            // Для контроля размера изображения
            .assertView('cart-list', PO.cart.list());
    });
});
