hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ShopList', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-shoplist--default', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.ShopList');
        });
    });
});
