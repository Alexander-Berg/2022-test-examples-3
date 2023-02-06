import assert from 'assert';

describe('Ссылка несуществующего проекта', function() {
    hermione.skip.in(/.*/, '');
    it('Открывается страница', async function() {
        const { browser } = this;

        await browser.yaOpenPageByUrl('/exps/ba');

        const title = await this.browser.$('.base-root h2');
        const text = await title.getText();

        assert.strictEqual(text, 'Проекты:');
    });
});
