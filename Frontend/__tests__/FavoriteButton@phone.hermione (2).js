const FAVORITE_BUTTON = '.FavoriteButton';
const FAVORITE_LOGIN_POPUP = '.FavoriteLoginPopup';
const FAVORITE_LOGIN_AUTH_BUTTON = '.FavoriteLoginPopup-AuthButton';
const FAVORITE_ADD_NOTIFY = '.BottomBar-Item_type_user .BottomBar-ItemPopup';

// костыль: скрываем контет шторки чтоб клик был по оврелею
// т.к. контент шторки перекрывает место клика по оверлею, а нормального способа кликнуть выше в гермионе нет
// подумать над вынесением вкоманду
const closeDrawer = browser => {
    return browser
        .execute(selector => {
            document.querySelector(selector).style.display = 'none';
        }, '.Drawer-Curtain')
        .click('.Drawer-Overlay');
};

describe('Избранное', function() {
    describe('Пользователь разлогинен', function() {
        beforeEach(async function() {
            const browser = this.browser;

            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/rnd/c7hj4fr',
                expFlags: {
                    'analytics-disabled': '0',
                },
                query: {
                    product_id: 207,
                },
            });
            await browser.click(FAVORITE_BUTTON);
            await browser.yaWaitForVisible(FAVORITE_LOGIN_POPUP);
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'favorite-login-popup-show'
            });
        });

        hermione.skip.in(/.*/, 'Скипаем при переезде в ecom-sins из-за ошибки TypeError: Cannot read property \'binary\' of undefined');
        it('Попап залогина', async function() {
            const browser = this.browser;

            await browser.assertView('favorite-login-popup', `${FAVORITE_LOGIN_POPUP} .Drawer-Content`);

            await closeDrawer(browser);
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'favorite-login-popup-dismissed'
            });
            await browser.yaWaitForHidden(FAVORITE_LOGIN_POPUP);
            await browser.click(FAVORITE_BUTTON);
            await browser.yaWaitForVisible(FAVORITE_LOGIN_POPUP);
            await browser.click(FAVORITE_LOGIN_AUTH_BUTTON);

            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'favorite-login-popup-try-login'
            });
        });

        it('Добавление в избранное после авторизации', async function() {
            const browser = this.browser;

            await browser.onRecord(() => browser.auth('tap-user'));

            await browser.yaOpenEcomSpa({
                pageType: 'main',
                service: 'spideradio.github.io',
                expFlags: {
                    'analytics-disabled': '0',
                }
            });

            await browser.yaWaitForVisible(FAVORITE_ADD_NOTIFY);
            await browser.yaCheckMetrikaGoal({
                counterId: 53911873,
                name: 'save-favorite-from-product',
                params: {
                    afterAuthorization: 1
                }
            });
        });
    });
});
