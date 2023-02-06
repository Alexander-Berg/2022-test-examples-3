describe('Storybook', function() {
    describe('ReviewSkeleton', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-reviews-reviewskeleton--plain', true);

            await browser.yaAssertViewThemeStorybook('plain', '.ReviewSkeleton');
        });
    });
});
