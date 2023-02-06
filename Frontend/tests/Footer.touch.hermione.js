describe('Storybook', function() {
    describe('Footer', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-footer--plain', true);

            await browser.yaAssertViewThemeStorybook('plain', '.Footer');
        });

        it('emptySearch', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-footer--empty-search', true);

            await browser.yaAssertViewThemeStorybook('empty-search', '.Footer');
        });
    });
});
