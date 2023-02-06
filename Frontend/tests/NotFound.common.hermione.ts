hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('NotFound', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-notfound--plain', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.NotFound');
        });
    });
});
