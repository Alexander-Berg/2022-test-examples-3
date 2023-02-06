specs({
    feature: 'LoadPageButton',
}, () => {
    hermione.only.notIn('safari13');
    it('Внешний вид блока', function() {
        return this.browser
            .url('/turbo?stub=loadpagebutton/default.json')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .assertView('plain', PO.blocks.loadPageButton());
    });

    hermione.only.notIn('safari13');
    it('Кнопка должна нормально вписаться в сетку', function() {
        return this.browser
            .url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=0&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&turbo_enable_cgi_forwarding=1&exp_flags=ecommerce-design;ecom-listing-button')
            .yaWaitForVisible(PO.page(), 'Страница не загрузилась')
            .yaScrollPageToBottom()
            .assertView('plain', [
                PO.blocks.loadPageButton(),
                PO.blocks.products.itemLast(),
            ]);
    });

    hermione.only.notIn('safari13');
    it('Кнопка для загрузки появляется только для следующей страницы', async function() {
        const browser = this.browser;

        await browser.url('/turbo?text=logomebel.ru%2Fyandexturbocatalog%2F&page=1&srcrwr=SEARCH_SAAS_LISTINGS:SAAS_ANSWERS&turbo_enable_cgi_forwarding=1&exp_flags=ecommerce-design;ecom-listing-button');
        await browser.yaWaitForVisible(PO.page(), 'Страница не загрузилась');
        const buttonsNext = await browser.elements(PO.loadPageButtonNext());
        assert.equal(buttonsNext.value.length, 1, 'Кнопка дозагрузки следующей страницы ленты должна быть одна');
        const buttonsPrev = await browser.elements(PO.loadPageButtonPrev());
        assert.equal(buttonsPrev.value.length, 0, 'Предыдущие страницы ленты должны загружаться без кнопки');
    });
});
