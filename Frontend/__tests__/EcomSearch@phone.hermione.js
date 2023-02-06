describe('EcomSearch', function() {
    it('Интеграционный сценарий поиска', async function() {
        const browser = this.browser;

        // открываем страницу
        await browser.yaOpenEcomSpa({
            service: 'spideradio.github.io',
            pageType: 'main',
        });

        // вводим в поиске на главной необходимую инфу
        await browser.click('.SuggestSearch-Form');
        await browser.yaWaitForVisible('.SuggestSearch_focused', 'Не появился фокус в поле поиска');
        await browser.yaMockImages();
        await browser.assertView('search-form', '.SuggestSearch-InputWrapper');
        await browser.setValue('.SuggestSearch .Textinput-Control', 'lorem');

        // отправляемся в каталог
        await browser.click('.SuggestSearch-Button_type_submit');
        await browser.yaWaitForVisible('.EcomScreen_type_product-list .ProductList', 'Не произошел переход на страницу каталога');

        await browser.yaMockImages();
        await browser.yaAssertViewportView('catalog-page');
        await browser.click('.SuggestSearch-Icon_type_cross');
        await browser.setValue('.Textinput-Control', 'oops');
        const value = await browser.getValue('.Textinput-Control');

        assert.strictEqual(value, 'oops', 'Не изменилось значение в поле ввода');
        await browser.yaMockImages();
        await browser.assertView('search-form-filled', '.SuggestSearch-InputWrapper');
    });
});
