hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('AliceHeader', function() {
        it('plain', async function() {
            await this.browser.yaOpenComponent('tests-aliceheader--plain', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.AliceHeader');
        });

        it('guru', async function() {
            await this.browser.yaOpenComponent('tests-aliceheader--guru', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.AliceHeader');
        });

        it('gifts', async function() {
            await this.browser.yaOpenComponent('tests-aliceheader--gifts', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.AliceHeader');
        });
    });
});
