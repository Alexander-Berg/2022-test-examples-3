describe('Storybook', function() {
    describe('ReviewsEmptyList', function() {
        it('success', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-reviews-reviewsemptylist--plain', true);

            await browser.yaAssertViewThemeStorybook('success', '.ReviewsEmptyList');
        });

        it('error', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-reviews-reviewsemptylist--error', true);

            await browser.yaAssertViewThemeStorybook('error', '.ReviewsEmptyList');
        });
    });
});
