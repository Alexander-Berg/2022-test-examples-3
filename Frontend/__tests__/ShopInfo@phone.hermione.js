const { URL } = require('url');

const params = {
    __ym: {
        turbo_page: 1,
        doc_ui: 'touch-phone',
    },
    ecom_spa: 1,
    from: 'MainScreen',
};

hermione.only.notIn('iphone', 'Страница отзывов не работает на iphone < 12');
describe('ShopInfo', () => {
    it('Проверка информационного блока на главном экране', async function() {
        const browser = this.browser;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            expFlags: {
                'analytics-disabled': '0',
            },
            query: {
                patch: 'setShopInfo',
            },
        });

        await browser.yaWaitForVisible('.EcomScreen_type_main');

        // Внешний вид
        await browser.yaScrollPage('.ShopInfo');
        await browser.assertView('shop-info', ['.ShopInfo']);

        // Ссылка на отзывы
        await browser.click('.ShopReviewsLink');

        const reviewsUrl = await browser.getUrl();
        const { searchParams: reviewsParams } = new URL(reviewsUrl);

        assert.equal(reviewsParams.get('frame_id'), 'main-reviews', 'Не произошел переход на страницу отзывов');
        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'shop-review-button-click',
            params,
        });

        await browser.back();
        await browser.yaWaitForVisible('.ShopInfo');
        await browser.yaScrollPage('.ShopInfo');

        // Проверка контактов
        await browser.yaIndexify('.ShopInfo-Contact');

        await browser.yaCheckLink({
            selector: '.ShopInfo-Contact[data-index="0"] .Link',
            message: 'Неправильная ссылка на телефон',
            target: '_parent',
            url: {
                href: 'tel:74997042424',
            },
        });

        await browser.click('.ShopInfo-Contact[data-index="0"] .Link');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'shop-phone-link-click',
            params,
        });

        await browser.yaCheckLink({
            selector: '.ShopInfo-Contact[data-index="1"] .Link',
            message: 'Неправильная ссылка на email',
            target: '_parent',
            url: {
                href: 'mailto:info@pixel24.ru',
            },
        });

        await browser.click('.ShopInfo-Contact[data-index="1"] .Link');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'shop-mail-link-click',
            params,
        });

        // Проверка ссылки "Подробнее"
        await browser.click('.NavigationLinkWithArrow:not(.ShopReviewsLink)');

        const aboutUrl = await browser.getUrl();
        const { pathname: aboutPath } = new URL(aboutUrl);

        assert.equal(aboutPath, '/turbo/spideradio.github.io/n/yandexturbocatalog/about/', 'Не произошел переход на страницу "О магазине"');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'shop-info-more-link',
            params,
        });

        await browser.back();
        await browser.yaWaitForVisible('.ShopInfo');
        await browser.yaScrollPage('.YandexMarketCheckLink');

        // СКК
        await browser.click('.YandexMarketCheckLink');

        const marketUrl = await browser.getUrl();
        const { pathname: marketPath } = new URL(marketUrl);

        assert.equal(marketPath, '/turbo/spideradio.github.io/n/yandexturbocatalog/market_check/', 'Не произошел переход на страницу СКК');

        await browser.yaCheckMetrikaGoal({
            counterId: 53911873,
            name: 'click-market-check-link',
            params,
        });
    });
});
