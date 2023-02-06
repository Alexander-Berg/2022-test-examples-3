hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductListDropDown', function() {
        it('drop-down-list', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-productlistdropdown--plain', true);
            await bro.click('.ProductListControlsButton');

            await bro.yaAssertViewThemeStorybook('drop-down-list', '.ProductListDropDown-Popup');
        });
    });
});
