describe('Главная страница', function() {
    it('Заголовок', async function() {
        const { browser, PO } = this;

        await browser.url('/story-like-search');

        const pageTitle = await browser.getTitle();

        assert.strictEqual(pageTitle, 'story-like-search');

        const title = await browser.getText(PO.Title());

        assert.strictEqual(title, 'Hello story-like-search, it\'s desktop');
    });
});
