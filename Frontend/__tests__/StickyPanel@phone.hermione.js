describe('StickyPanel', function() {
    it('есть на странице, если кнопка "Оформить заказ" находится за пределами экрана', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
        });

        await browser.yaScrollPage('.ProductItem:nth-child(1)', 0);
        await browser.click('.ProductItem:nth-child(1) .Button.ProductItem-Action');
        await browser.click('.ProductItem:nth-child(2) .Button.ProductItem-Action');

        await browser.yaScrollPage('.ProductItem:nth-child(3)', 0);
        await browser.click('.ProductItem:nth-child(3) .Button.ProductItem-Action');
        await browser.click('.ProductItem:nth-child(4) .Button.ProductItem-Action');
        await browser.click('.BottomBar-Item:nth-child(3)');

        await browser.yaWaitForVisible(PO.blocks.stickyPanel());
        await browser.yaShouldBeVisible(PO.blocks.stickyPanel());
        await browser.yaWaitForHidden('.Popup');
        await browser.assertView('plain', PO.blocks.stickyPanel());
    });

    it('нет на странице, если видно кнопку "Оформить заказ"', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
        });

        await browser.yaShouldNotBeVisible(PO.blocks.stickyPanel());
    });

    it('появляется и скрывается в зависимости от видимости кнопки "Оформить заказ"', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
        });

        await browser.yaScrollPage('.ProductItem:nth-child(1)', 0);
        await browser.click('.ProductItem:nth-child(1) .Button.ProductItem-Action');
        await browser.click('.ProductItem:nth-child(2) .Button.ProductItem-Action');

        await browser.yaScrollPage('.ProductItem:nth-child(3)', 0);
        await browser.click('.ProductItem:nth-child(3) .Button.ProductItem-Action');
        await browser.click('.ProductItem:nth-child(4) .Button.ProductItem-Action');
        await browser.click('.BottomBar-Item:nth-child(3)');

        await browser.yaWaitForVisible(PO.blocks.stickyPanel());
        await browser.yaShouldBeVisible(PO.blocks.stickyPanel());

        await browser.yaScrollPage('.ScreenContent .CartButton', 0);

        await browser.yaWaitForHidden(PO.blocks.stickyPanel());
        await browser.yaShouldNotBeVisible(PO.blocks.stickyPanel());
    });
});
