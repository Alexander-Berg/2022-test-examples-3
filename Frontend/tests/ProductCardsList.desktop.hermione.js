hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductCardsList', function() {
        it('wide', async function() {
            await this.browser.yaOpenComponent('tests-productcardslist-desktop--wide', false);
            await this.browser.yaAssertViewThemeStorybook('wide', '.ProductCardsList', {
                allowViewportOverflow: true,
            });
        });
    });
});
