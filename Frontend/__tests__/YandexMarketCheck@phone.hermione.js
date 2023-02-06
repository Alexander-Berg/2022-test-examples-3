describe('YandexMarketCheck', function() {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/n/yandexturbocatalog/market_check/' +
            '?srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheck');
        await browser.yaScrollPage('.YandexMarketCheck', 0);
        await browser.assertView('plain', '.YandexMarketCheck');
    });

    it('Переход с карточки товара', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/s/' +
            '?pcgi=rnd%3Dfwj3pt&srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheckLink');
        await browser.yaScrollPage('.YandexMarketCheckLink', 0);
        await browser.click('.YandexMarketCheckLink');
        await browser.yaWaitForVisible('.YandexMarketCheck', 'Страница не открылась');
        await browser.yaScrollPage('.ScreenHeaderBack', 0);
        await browser.pause(1000);
        await browser.click('.ScreenHeaderBack');
        await browser.yaWaitForVisible('.YandexMarketCheckLink', 'Переход назад не произошел.');
    });

    it('Переход с страницы "О магазине"', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/n/yandexturbocatalog/about/' +
            '?srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheckLink');
        await browser.yaScrollPage('.YandexMarketCheckLink', 0);
        await browser.click('.YandexMarketCheckLink');
        await browser.yaWaitForVisible('.YandexMarketCheck', 'Страница не открылась.');
        await browser.yaScrollPage('.ScreenHeaderBack', 0);
        await browser.pause(1000);
        await browser.click('.ScreenHeaderBack');
        await browser.yaWaitForVisible('.AboutShopScreen', 'Переход назад не произошел.');
    });

    it('Кнопка "Сообщить о проблеме" ведет на ФОС', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/n/yandexturbocatalog/market_check/' +
            '?srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheck-Button');
        await browser.yaScrollPage('.YandexMarketCheck-Button', 0);
        await browser.getAttribute('.YandexMarketCheck-Button', 'href')
            .then(href => browser.yaCheckURL(
                href,
                'https://forms.yandex.ru/surveys/10023469.2596112ebb92735d531815bd8ca83c8860c8c25f/?shopname=spideradio.github.io',
                'Сообщить о проблеме'
            ));
    });
});
