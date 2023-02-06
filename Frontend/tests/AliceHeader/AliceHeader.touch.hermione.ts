describe('AliceHeader', function() {
    const rootSelector = '.AliceHeader';
    const closeSelector = '.AliceHeader-Close';

    it('Скрытие блока', async function() {
        const bro = this.browser;
        await bro.yaOpenPageByUrl('/products/search?text=iphone&query_source=main_page&exp_flags=disable_alice_message=1');
        await bro.yaWaitForVisible(rootSelector, 'блок Алисы не появился');

        await bro.click(closeSelector);

        await bro.yaWaitForHidden(rootSelector);

        await bro.refresh();

        await bro.yaWaitForHidden(rootSelector);
    });
});
