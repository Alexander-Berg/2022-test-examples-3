hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('RatingLine', function() {
        it('Without reviews', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-ratingline--showcase-without-reviews', true);
            await browser.yaAssertViewThemeStorybook('plain', '.story-wrapper');
        });

        it('With reviews', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-ratingline--showcase-with-reviews', true);
            await browser.yaAssertViewThemeStorybook('plain', '.story-wrapper');
        });

        it('With overflow', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-ratingline--small-container', true);
            await browser.yaAssertViewThemeStorybook('plain', '.story-wrapper');
        });
    });
});
