const { URL } = require('url');

describe('AboutShopScreen', function() {
    it('Клик по логотипу магазина', async function() {
        const { browser } = this;

        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'about',
        });

        await browser.yaWaitForVisible('.ScreenContent');
        await browser.click('.AboutShopScreen__logo .Link');
        await browser.yaWaitForVisible('.EcomScreen_type_main');

        const url = await browser.getUrl();
        const pageUrl = new URL(url);

        assert.strictEqual(pageUrl.pathname, '/turbo/spideradio.github.io/n/yandexturbocatalog/main/');
    });

    it('Ссылка Пожаловаться', async function() {
        const { browser } = this;
        // Магазин с turboAppEnabled=false
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'about',
            query: { patch: 'setTurboAppEnabledFalse' },
        });

        await browser.yaWaitForVisible('.SupportLinks');
        await browser.getAttribute('.SupportLinks .SupportLinks-Link:first-child', 'href')
            .then(href => browser.yaCheckURL(
                href,
                'https://yandex.ru/support/abuse/troubleshooting/turbo/list.html',
                'Не та ссылка на "Пожаловаться"'
            ));
    });

    it('Ссылка Пожаловаться ведет на ФОС', async function() {
        const { browser } = this;
        // Магазин с turboAppEnabled=true
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'about',
        });

        await browser.yaWaitForVisible('.SupportLinks');
        await browser.click('.SupportLinks .SupportLinks-Link:first-child');
        await browser.yaWaitForVisible('.EcomFrame-Iframe');
        await browser.getAttribute('.EcomFrame-Iframe', 'src')
            .then(href => browser.yaCheckURL(
                href,
                'https://forms.yandex.ru/surveys/10023469.2596112ebb92735d531815bd8ca83c8860c8c25f/?shopname=spideradio.github.io&iframe=1',
                'Cсылка на "Пожаловаться" не ведет на iframe'
            ));
    });
});
