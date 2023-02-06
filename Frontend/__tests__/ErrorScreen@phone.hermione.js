async function openErrorPage(browser) {
    await browser.yaOpenEcomSpa({
        service: 'spideradio.github.io',
        pageType: 'main',
        expFlags: { 'turbo-app-error-counter': 1 },
    });
    await browser.yaWaitForVisible('.EcomScreen_type_main .CategoryList-ItemContainer');
    await browser.click('.CategoryList-Item:nth-child(1)');
    await browser.yaWaitForVisible('.EcomScreen_type_error', 'не появилась страница об ошибке');
}

describe('Ecom-tap', function() {
    it('При ошибке работает переход назад', async function() {
        const browser = this.browser;
        await openErrorPage(browser);

        await browser.back();
        await browser.yaWaitForVisible('.EcomScreen_type_product-list', 'Не сработал клик Назад по истории');
    });

    it('При ошибке работает кнопка перезагрузить', async function() {
        const browser = this.browser;
        await openErrorPage(browser);

        await browser.click('.Button2');
        await browser.yaWaitForVisible('.EcomScreen_type_main', 'Не сработал переход по кнопке Перезагрузить');
    });
});
