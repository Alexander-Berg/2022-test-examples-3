hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('Thumbnail', function() {
        it('plain', async function() {
            await this.browser.yaOpenComponent('tests-thumbnail--plain', true);
            await this.browser.yaAssertViewThemeStorybook('plain', '.Thumbnail');
        });

        it('fallback', async function() {
            await this.browser.yaOpenComponent('tests-thumbnail--fallback', true);
            await this.browser.yaAssertViewThemeStorybook('fallback', '.Thumbnail');
        });

        it('custom-size', async function() {
            await this.browser.yaOpenComponent('tests-thumbnail--customsize', true);
            await this.browser.yaAssertViewThemeStorybook('custom-size', '.Thumbnail');
        });

        it('custom-size-fallback', async function() {
            await this.browser.yaOpenComponent('tests-thumbnail--customsizefallback', true);
            await this.browser.yaAssertViewThemeStorybook('custom-size-fallback', '.Thumbnail');
        });
    });
});
