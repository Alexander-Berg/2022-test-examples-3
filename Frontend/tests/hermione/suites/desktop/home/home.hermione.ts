import assert from 'assert';

describe('Главная страница', function() {
    it('Заголовок', async function() {
        const { browser, PO } = this;

        await browser.yaOpenPageByUrl('/adv');

        const pageTitle = await browser.getTitle();

        assert.strictEqual(pageTitle, 'adv');

        await browser.yaWaitForVisible(PO.Title());

        const title = await this.browser.$(PO.Title());
        const text = await title.getText();

        assert.strictEqual(text, 'Hello home page, it\'s desktop');

        await browser.assertView('page', 'body');
    });
});
