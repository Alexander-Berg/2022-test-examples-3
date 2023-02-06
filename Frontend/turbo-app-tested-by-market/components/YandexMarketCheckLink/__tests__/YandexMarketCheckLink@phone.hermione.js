describe('YandexMarketCheckLink', function() {
    it('Внешний вид', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/s/' +
            '?pcgi=rnd%3Dfwj3pt&srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheckLink');
        await browser.yaScrollPage('.YandexMarketCheckLink', 0);
        await browser.assertView('plain', '.YandexMarketCheckLink');
    });

    it('Наличие на странице "О Магазине"', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/n/yandexturbocatalog/about/' +
            '?srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheckLink');
    });

    it('Клик по ссылке', async function() {
        const { browser } = this;

        await browser.url('/turbo/spideradio.github.io/n/yandexturbocatalog/about/' +
            '?srcrwr=SAAS%3ASAAS_ANSWERS&exp_flags=turbo-app-tested-by-market&exp_flags=turbo-app-any-ua');

        await browser.yaWaitForVisible('.YandexMarketCheckLink');
        await browser.yaScrollPage('.YandexMarketCheckLink', 0);
        await browser.click('.YandexMarketCheckLink .Link');
        await browser.yaWaitForVisible('.YandexMarketCheck');
    });
});
