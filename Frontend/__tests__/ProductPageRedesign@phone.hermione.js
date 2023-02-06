specs({
    feature: 'product-page',
    type: 'redesign',
}, () => {
    describe('Новый футер', function() {
        hermione.only.notIn('safari13');
        it('В отдельной карточке', function() {
            return this.browser
                .url('?stub=productpage/product-2-server-redesign.json&exp_flags=turboforms_endpoint=/')
                .yaWaitForVisible(PO.pageJsInited())
                .yaIndexify(PO.page.result())
                .yaShouldBeVisible(PO.ecomFooter());
        });

        hermione.only.notIn('safari13');
        it('В бесконечной ленте', function() {
            return this.browser
                .url('?stub=productpage/product-1-server-redesign.json&exp_flags=turboforms_endpoint=/')
                .yaWaitForVisible(PO.pageJsInited())
                .yaIndexify(PO.page.result())
                .yaScrollPageToBottom()
                .yaWaitForVisible(PO.page.result1())
                .yaShouldNotBeVisible(PO.page.result0.ecomFooter())
                .yaShouldNotBeVisible(PO.page.result1.ecomFooter())
                .yaShouldBeVisible(PO.page.result0.footerInline())
                .yaShouldBeVisible(PO.page.result1.footerInline());
        });
    });

    hermione.only.notIn('safari13');
    it('Внешний вид всей страницы', function() {
        return this.browser
            .url('/turbo?stub=productpage/ecommerce-design.json&exp_flags=turboforms_endpoint=/')
            .yaWaitForVisible(PO.pageJsInited())
            // Проскрол страницы до конца, чтобы выполнился autoload и потом не моргал на скриншоте.
            // В стабе указан related_type_ajax чтобы отрисовать маленький футер.
            .yaScrollPageToBottom()
            .pause(250)
            .assertView('plain', PO.page(), {
                ignoreElements: [
                    PO.blocks.ecomHeader(),
                    PO.blocks.slider(),
                    PO.blocks.accordion(),
                    PO.blocks.productsCarousel(),
                    PO.blocks.productsCarousel.turboNativeScroll(),
                ],
            });
    });
});
