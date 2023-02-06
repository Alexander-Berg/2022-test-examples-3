describe('Cover', function() {
    it('Внешний вид шапки без логотипа', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
            query: { patch: 'removeShopLogo' },
        });

        await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons', '.SuggestSearch']);
    });

    it('Внешний вид минишапки без логотипа', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'catalog',
            query: { patch: 'removeShopLogo' },
        });

        await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons']);
    });

    describe('Новая шапка', function() {
        it('Внешний вид шапки без логотипа', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                query: { patch: 'removeShopLogo' },
            });

            await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons', '.SuggestSearch'], { ignoreElements: ['.CategoryList'] });
        });

        it('Внешний вид минишапки без логотипа', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
                query: { patch: 'removeShopLogo' },
            });

            await browser.assertView('cover', ['.Cover', '.SuggestSearch'], { ignoreElements: ['.CategoryList'] });
        });

        it('Внешний вид на главной странице', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
            });

            await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons', '.SuggestSearch'], { ignoreElements: ['.CategoryList'] });
        });

        it('Внешний вид на странице каталога', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'catalog',
            });

            await browser.assertView('cover', ['.Cover', '.SuggestSearch'], { ignoreElements: ['.CategoryList'] });
        });

        it('Внешний вид на странице товара', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/rnd/oqr9w',
                query: { product_id: 132 },
            });

            await browser.yaShouldNotBeVisible('.SuggestSearch', 'На карточке товара не должно быть поиска');
            await browser.assertView('cover', ['.Cover']);
        });

        it('Внешний вид поиска', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
            });

            await browser.setValue('.SuggestSearch-Input input', 'Текст');

            await browser.assertView('search', ['.Cover']);
        });
    });

    describe('Проверка кнопки закрытия в шторке', () => {
        it('В большой шапке', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                query: {
                    'in-iframe': 1,
                    'overlay-drawer': 1,
                },
            });

            await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons', '.SuggestSearch']);

            await browser.yaIndexify('.Cover-Icon');
            await browser.click('.Cover-Icon[data-index="1"]');

            await browser.yaCheckPostMessage({ action: 'close' });
        });

        it('В минишапке', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    'in-iframe': 1,
                    'overlay-drawer': 1,
                    product_id: '202',
                    pcgi: 'rnd%3D2lum7hf3',
                },
            });

            await browser.assertView('cover', ['.Cover-Content', '.Cover-Icons']);

            await browser.yaIndexify('.Cover-Icon');
            await browser.click('.Cover-Icon[data-index="1"]');

            await browser.yaCheckPostMessage({ action: 'close' });
        });
    });

    describe('storybook', () => {
        it('Внешний вид', async function() {
            const { browser } = this;

            await browser.yaOpenEcomStory('cover', 'showcase');
            await browser.assertView('first', [
                '.story .r0 .c0',
                '.story .r1 .c0',
                '.story .r2 .c0',
            ]);
            // Скролим страницу до конца вправо, чтобы снять второй столбец.
            await browser.yaScrollElement('#root', 800);
            await browser.assertView('second', [
                '.story .r0 .c1',
                '.story .r1 .c1',
                '.story .r2 .c1',
            ]);
        });
    });

    describe('Под флагом turbo-app-bottombar', () => {
        it('Иконка профиля в шапке ведет на страницу пользователя', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                query: { patch: 'setBlackboxData' },
                expFlags: { 'turbo-app-bottombar': 1 },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.yaWaitForVisible('.UserIcon');
            await browser.click('.UserIcon');

            await browser.waitForVisible('.UserScreen', 'Не произошел переход на страницу пользователя');
        });

        it('Иконка профиля в шапке ведет на Паспорт', async function() {
            const { browser } = this;

            await browser.yaOpenEcomSpa({
                service: 'spideradio.github.io',
                pageType: 'main',
                query: { patch: 'setBlackboxData' },
                expFlags: { 'turbo-app-bottombar': 1 },
            });

            await browser.yaWaitForVisible('.ScreenContent');
            await browser.yaWaitForVisible('.UserIcon');
            await browser.click('.UserIcon');

            await browser.yaWaitUntil(
                'Не произошел переход на Паспорт при клике в избранное',
                () => browser.execute(function() {
                    return location.host === 'passport.yandex.ru';
                })
            );
        });
    });
});
