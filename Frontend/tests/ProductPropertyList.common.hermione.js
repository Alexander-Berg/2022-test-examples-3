hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductPropertyList', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-productpropertylist--default', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.ProductPropertyList');
        });
    });
});
