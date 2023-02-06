hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('PriceRange', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-pricerange--plain', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.PriceRange');
        });

        it('small container', async function() {
            await this.browser.yaOpenComponent('tests-pricerange--small-container', true);
            await this.browser.yaAssertViewThemeStorybook('small-container', '.PriceRange');
        });
    });
});
