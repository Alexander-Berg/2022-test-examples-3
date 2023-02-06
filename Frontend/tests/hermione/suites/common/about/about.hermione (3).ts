import assert from 'assert';

describe('Страница about', function() {
    it('Заголовок', async function() {
        await this.browser.yaOpenPageByUrl('/stub/about');

        const title = await this.browser.$(this.PO.About.Title());
        const text = await title.getText();

        assert.strictEqual(text, 'Hello about page');

        await this.browser.assertView('page', 'body');
    });
});
