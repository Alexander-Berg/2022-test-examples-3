hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('Misspell', function() {
        it('plain', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-misspell--plain', true);

            await browser.yaAssertViewThemeStorybook('plain', '.Misspell');
        });
    });
});
