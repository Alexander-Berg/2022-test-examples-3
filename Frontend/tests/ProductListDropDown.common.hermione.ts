hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductListDropDown', function() {
        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-productlistdropdown--plain', true);
            await bro.yaAssertViewThemeStorybook('default', '.ProductListDropDown-SelectWrapper');

            await bro.yaOpenComponent(
                'tests-productlistdropdown--plain',
                true,
                [{ name: 'View', value: 'clear' }],
            );
            await bro.yaAssertViewThemeStorybook('clear', '.ProductListDropDown-SelectWrapper');
        });

        it('hover', async function() {
            const bro = this.browser;
            const selectorClass = await bro.getMeta('platform') === 'desktop' ?
                '.ProductListControlsButton' :
                '.ProductListDropDown-Select';

            await bro.yaOpenComponent('tests-productlistdropdown--plain', true);
            await bro.click(selectorClass);
            await bro.yaAssertViewThemeStorybook('default', '.ProductListDropDown-SelectWrapper');

            await bro.yaOpenComponent(
                'tests-productlistdropdown--plain',
                true,
                [{ name: 'View', value: 'clear' }],
            );
            await bro.click(selectorClass);
            await bro.yaAssertViewThemeStorybook('clear', '.ProductListDropDown-SelectWrapper');
        });
    });
});
