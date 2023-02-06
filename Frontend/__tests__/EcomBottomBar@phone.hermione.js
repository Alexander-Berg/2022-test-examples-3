describe('Ecom-tap', function() {
    describe('turbo-app-bottombar', function() {
        it('Внешний вид', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                expFlags: { 'turbo-app-bottombar': 1 },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.assertView('bottombar', '.EcomBottomBar');
        });

        it('Переходы по ссылкам', async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                expFlags: { 'turbo-app-bottombar': 1 },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.click('.BottomBar-Item_type_catalog');
            await browser.waitForVisible('.EcomScreen_type_product-list', 'Каталог не открылся');

            await browser.click('.BottomBar-Item_type_cart');
            await browser.waitForVisible('.EcomScreen_type_cart', 'Корзина не открылась');

            await browser.click('.BottomBar-Item_type_about');
            await browser.waitForVisible('.BottomBar-Item_type_about', 'Страница о магазине не открылась');

            await browser.click('.BottomBar-Item_type_favorites');
            await browser.yaWaitUntil(
                'Не произошел переход на Паспорт при клике в избранное',
                () => browser.execute(function() {
                    return location.host === 'passport.yandex.ru';
                })
            );
        });
    });
});
