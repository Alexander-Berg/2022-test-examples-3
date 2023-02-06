const LS_KEY = 'turbo-app-ecom--spideradio.github.io';

const __ym = {
    turbo_page: 1,
    doc_ui: 'touch-phone',
};

describe('Ecom-tap', function() {
    describe('Уточнение города пользователя', function() {
        it('Если в LS нет информации о городе - показываем попап', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_product-list');

            await browser.execute(function(LS_KEY) {
                return localStorage.getItem(LS_KEY);
            }, LS_KEY).then(({ value }) => {
                assert.isNull(value);
            });

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-ask-user-city': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.yaWaitForVisible('.EcomDrawer');

            await browser.assertView('plain', '.EcomDrawer');

            await browser.execute(function(LS_KEY) {
                return localStorage.getItem(LS_KEY);
            }, LS_KEY).then(({ value }) => {
                const data = JSON.parse(value);
                assert.deepEqual(data.deliveryRegion, {
                    name: 'Череповец',
                    id: 968,
                    lon: 37.906929,
                    lat: 59.127415,
                });
            });
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'region-popup-shown',
                params: { ecom_spa: 1, __ym },
            });

            await browser.click('.EcomDrawer .Button2_view_action');
            await browser.yaWaitForHidden('.EcomDrawer');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'region-popup-ok-clicked',
                params: { ecom_spa: 1, __ym },
            });
        });

        it('Попап не покажется, если в LS есть данные о регионе', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.yaWaitForVisible('.EcomScreen_type_product-list');

            await browser.execute(function(LS_KEY) {
                localStorage.setItem(
                    LS_KEY,
                    JSON.stringify({ deliveryRegion: {
                        name: 'Череповец',
                        id: 968,
                        lon: 37.906929,
                        lat: 59.127415,
                    } })
                );
            }, LS_KEY);

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-ask-user-city': 1,
                },
                query: {
                    patch: 'setRegion',
                },
            });
            await browser.yaWaitForVisible('.EcomScreen_type_product-list');

            await browser.pause(500);
            await browser.yaShouldNotBeVisible('.EcomDrawer');
        });

        it('Неправильно определили регион', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-ask-user-city': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.yaWaitForVisible('.EcomDrawer');

            await browser.click('.EcomDrawer .Button2_view_default');
            await browser.yaWaitForHidden('.EcomDrawer');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'region-popup-another-city-clicked',
                params: { ecom_spa: 1, __ym },
            });
        });

        it('Закрытие попапа по фону', async function() {
            const browser = this.browser;

            // открываем страницу
            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                expFlags: {
                    turboforms_endpoint: '/',
                    'turbo-app-ask-user-city': 1,
                    'analytics-disabled': '0',
                },
                query: {
                    patch: 'setRegion',
                },
            });

            await browser.yaWaitForVisible('.EcomDrawer');

            await browser.click('.EcomDrawer .Drawer-Overlay');
            await browser.yaWaitForHidden('.EcomDrawer');
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'region-popup-dismissed',
                params: { ecom_spa: 1, __ym },
            });
        });
    });
});
