describe('Storybook', function() {
    describe('ProductCardsList', function() {
        it('horizontal', async function() {
            await this.browser.yaOpenComponent('tests-productcardslist--horizontal', true);

            await this.browser.yaAssertViewThemeStorybook('horizontal', 'body', {
                allowViewportOverflow: true,
            });
        });
    });
});
