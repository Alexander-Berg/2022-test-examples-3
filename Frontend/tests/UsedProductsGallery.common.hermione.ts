hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('UsedProductsGallery', function() {
        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-usedproductsgallery--plain', true);
            await bro.yaAssertViewThemeStorybook('default', '.UsedProductsGalleryStory');
        });
    });
});
