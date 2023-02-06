hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('RatingStars', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-ratingstars--showcase', true);
            await browser.yaAssertViewThemeStorybook('plain', '.story-wrapper');
        });
    });
});
