hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductCardSkeleton', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-productcardskeleton--plain', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.ProductCardSkeleton');
        });
    });
});
