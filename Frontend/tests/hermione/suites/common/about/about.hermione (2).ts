import assert from 'assert';

describe('Страница проекта base', function() {
    hermione.skip.in(/.*/, '');
    it('Открывается страница', async function() {
        await this.browser.yaOpenPageByUrl('/exps/base');
        const title = await this.browser.$('.base-root h2');
        const text = await title.getText();

        assert.strictEqual(text, 'Проекты:');
    });
});
