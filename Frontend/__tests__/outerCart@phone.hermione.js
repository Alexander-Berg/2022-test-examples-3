describe('Ecom-tap', () => {
    describe('С внешней корзиной', async function() {
        it('Общий внешний вид', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 202,
                    pcgi: 'rnd%3D2lum7hf3',
                    patch: 'outerCartForSPA',
                }
            });

            await browser.yaWaitForVisible('.EcomFooter');
            await browser.yaShouldNotBeVisible('.EcomBottomBar');
            await browser.yaShouldNotBeVisible('.Cover-Icons');

            await browser.yaScrollPage('.EcomFooter');

            await browser.yaAssertViewportView('footer');
        });

        describe('Кнопки "В корзину"', () => {
            it('В карточке товара', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    url: '/turbo/spideradio.github.io/s/',
                    expFlags: { 'analytics-disabled': '0' },
                    query: {
                        product_id: 202,
                        pcgi: 'rnd%3D2lum7hf3',
                        patch: 'outerCartForSPA',
                    }
                });

                await browser.yaWaitForVisible('.EcomFooter');
                await browser.assertView('button', '.ProductScreen-Actions-Button_toCart');

                await browser.yaCheckLink({
                    selector: '.ProductScreen-Actions-Button_toCart',
                    message: 'Неправильная ссылка в "Добавить в корзину"',
                    target: '_blank',
                    url: {
                        href: '/turbo?text=about',
                        ignore: ['protocol', 'hostname'],
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'add-to-cart',
                });
            });

            it('На морде', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'main',
                    query: { patch: 'outerCartForSPA' },
                    expFlags: { 'analytics-disabled': '0' },
                });

                await browser.yaWaitForVisible('.EcomFooter');
                await browser.yaShouldBeVisible('.Collection + .ProductList');

                await browser.assertView(
                    'button',
                    '.Collection + .ProductList .ProductItem:nth-child(1) .ProductItem-Action'
                );

                await browser.yaCheckLink({
                    selector: '.Collection + .ProductList .ProductItem:nth-child(1) .ProductItem-Action',
                    message: 'Неправильная ссылка в "Добавить в корзину"',
                    target: '_blank',
                    url: {
                        href: '/turbo?text=about',
                        ignore: ['protocol', 'hostname'],
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'add-to-cart',
                });
            });

            it('В листинге', async function() {
                const browser = this.browser;

                await browser.yaOpenEcomSpa({
                    service: 'spideradio.github.io',
                    pageType: 'catalog',
                    query: { patch: 'outerCartForSPA' },
                    expFlags: { 'analytics-disabled': '0' },
                });

                await browser.yaWaitForVisible('.EcomFooter');
                await browser.yaShouldBeVisible('.ProductList');

                await browser.assertView(
                    'button',
                    '.ProductList .ProductItem:nth-child(1) .ProductItem-Action'
                );

                await browser.yaCheckLink({
                    selector: '.ProductList .ProductItem:nth-child(1) .ProductItem-Action',
                    message: 'Неправильная ссылка в "Добавить в корзину"',
                    target: '_blank',
                    url: {
                        href: '/turbo?text=about',
                        ignore: ['protocol', 'hostname'],
                    },
                });

                await browser.yaCheckMetrikaGoal({
                    counterId: 53911873,
                    name: 'add-to-cart',
                });
            });
        });
    });
});
