async function openPage(browser, { pageType, url, query = {}, expFlags = {} }) {
    await browser.yaOpenEcomSpa({
        service: 'spideradio.github.io',
        pageType,
        url,
        expFlags: Object.assign({}, expFlags, { 'analytics-disabled': '0' }),
        query: {
            ...query,
            hermione_yanalytics: 'stub',
        },
    });

    await browser.waitForExist('.Ganalytics_loaded', 'Не загрузилась google-аналитика');
    // Ожидаем отправки хита, он отправляется с задержкой в секунду
    await browser.pause(1000);
}

async function assertLastActionDeepEqual(browser, expected, message = '') {
    const { value: actions } = await browser.execute(() => Ya.yanalytics.testActions);
    assert.deepEqual(actions[actions.length - 1], expected, message);
}

async function removeLastAction(browser) {
    await browser.execute(() => {
        Ya.yanalytics.testActions = Ya.yanalytics.testActions.slice(0, -1);
    });
}

const hitAction = {
    action: 'track',
    alias: 'ga',
    params: { hitType: 'pageview' },
};

describe('Ecom-tap', () => {
    describe('Ganalytics', () => {
        it('Инициализация', async function() {
            const browser = this.browser;

            await openPage(browser, {
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: '207',
                    pcgi: 'rnd%3D0q',
                }
            });

            const { value } = await browser.execute(() => Ya.yanalytics.testActions);
            assert.deepEqual(value, [
                {
                    action: 'init',
                    alias: 'ga',
                    params: {
                        cookieDomain: 'none',
                        id: 'UA-177644510-1',
                        provider: 'google-analytics-ecommerce',
                    },
                },
                {
                    action: 'config',
                    alias: 'ga',
                    params: {
                        location: 'https://spideradio.github.io',
                        referrer: 'https://yandex.ru',
                        title: await browser.getTitle(),
                    },
                },
                hitAction,
            ], 'Счётчик pageview google-аналитики должен отправляться из айфрейма yanalytics.');
        });

        describe('Отправка хитов', () => {
            it('Главная', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'main' });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии главной не был отправлен pageview в google-аналитику');
            });

            it('Каталог', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'catalog' });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии каталога не был отправлен pageview в google-аналитику');
            });

            it('Корзина', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'cart' });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии корзины не был отправлен pageview в google-аналитику');
            });

            it('Чекаут', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'cart' });
                await removeLastAction(browser);

                await browser.yaWaitForVisible('.CartButton');
                await browser.click('.CartButton');
                await browser.pause(1000);

                await assertLastActionDeepEqual(browser, hitAction, 'При открытии чекаута не был отправлен pageview в google-аналитику');
            });

            it('Страница фильтров', async function() {
                const browser = this.browser;
                await openPage(browser, {
                    pageType: 'catalog',
                    query: { page_type: 'filter' },
                });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы фильтров не был отправлен pageview в google-аналитику');
            });

            it('О магазине', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'about' });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы о магазине не был отправлен pageview в google-аналитику');
            });

            it('О магазине (подробности)', async function() {
                const browser = this.browser;
                await openPage(browser, {
                    pageType: 'about_detail',
                    query: { about_category_id: '0' },
                });

                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы о магазине не был отправлен pageview в google-аналитику');
            });

            it('Профиль', async function() {
                const browser = this.browser;
                await openPage(browser, { pageType: 'user' });
                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы профиля не был отправлен pageview в google-аналитику');
            });

            it('Быстрый заказ', async function() {
                const browser = this.browser;
                await openPage(browser, {
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '207',
                        pcgi: 'rnd%3D0q',
                    }
                });

                await removeLastAction(browser);

                await browser.yaWaitForVisible('.ProductScreen-Actions-Button_oneClick');
                await browser.yaScrollPage('.ProductScreen-Actions-Button_oneClick', 0);
                await browser.click('.ProductScreen-Actions-Button_oneClick');
                await browser.pause(1000);

                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы быстрого заказа не был отправлен pageview в google-аналитику');
            });

            it('Карточка товара', async function() {
                const browser = this.browser;
                await openPage(browser, {
                    url: '/turbo/spideradio.github.io/s/',
                    query: {
                        product_id: '207',
                        pcgi: 'rnd%3D0q',
                    }
                });

                await assertLastActionDeepEqual(browser, hitAction, 'При открытии страницы товара не был отправлен pageview в google-аналитику');
            });
        });
    });
});
