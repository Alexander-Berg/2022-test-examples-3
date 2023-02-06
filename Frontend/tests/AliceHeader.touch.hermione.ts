describe('Storybook', function() {
    describe('AliceHeader', function() {
        it('redesign', async function() {
            await this.browser.yaOpenComponent('tests-aliceheader--redesign', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.AliceHeader');
        });
    });
});
