const { URL } = require('url');

describe('Ecom-tap', function() {
    describe('Авторизация', () => {
        it('Логин и аватар в навигационной панели для авторизованного пользователя', async function() {
            const { browser } = this;

            await browser.onRecord(() => browser.auth('tap-user'));
            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 202,
                    pcgi: 'rnd%3D2lum7hf3',
                }
            });

            await browser.yaMockImages();
            await browser.assertView('plain', '.BottomBar-Item_type_user');
        });

        it('Разлогин', async function() {
            const { browser } = this;

            await browser.onRecord(() => browser.auth('tap-user'));
            await browser.yaOpenEcomSpa({
                url: '/turbo/spideradio.github.io/s/',
                query: {
                    product_id: 202,
                    pcgi: 'rnd%3D2lum7hf3',
                }
            });

            await browser.click('.BottomBar-Item_type_user');

            await browser.waitForExist('.NavigationTransition_state_entered');
            await browser.waitForVisible('.UserScreen-MenuItem:last-child');
            await browser.click('.UserScreen-MenuItem:last-child');

            const url = await browser.getUrl();
            const { pathname } = (new URL(url));

            await browser.waitForVisible('.ScreenContent');

            assert.equal(
                pathname,
                '/turbo/spideradio.github.io/n/yandexturbocatalog/main/',
                'Не произошел редирект на гланую страницу',
            );
            await browser.assertView('plain', '.BottomBar-Item_type_user');
        });
    });
});
