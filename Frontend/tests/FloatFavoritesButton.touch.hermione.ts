describe('Storybook', function() {
    describe('FloatFavoritesButton', function() {
        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-floatfavoritesbutton--plain', true);
            await bro.yaAssertViewThemeStorybook('default', '.FloatFavoritesButton');

            const favoritesUrl = new URL(await bro.getAttribute('.FloatFavoritesButton', 'href'));
            const target = await bro.getAttribute('.FloatFavoritesButton', 'target');

            assert.strictEqual(favoritesUrl.pathname + favoritesUrl.search, '/collections/?type=goods&utm_source=products');
            assert.strictEqual(target, '_blank');
        });
    });
});
