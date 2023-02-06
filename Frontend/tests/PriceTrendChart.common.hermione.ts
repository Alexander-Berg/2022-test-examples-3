hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('PriceTrendChart', function() {
        it('Внешний вид', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-pricetrendchart--plain', true);
            await bro.yaAssertViewThemeStorybook('plain', '.PriceTrendChart');
        });
    });
});
