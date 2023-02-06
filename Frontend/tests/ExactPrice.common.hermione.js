hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ExactPrice', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-exactprice--plain', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.ExactPrice');
        });

        it('small container', async function() {
            await this.browser.yaOpenComponent('tests-exactprice--small-container', true);
            await this.browser.yaAssertViewThemeStorybook('small-container', '.ExactPrice');
        });

        it('big current price with small container', async function() {
            await this.browser.yaOpenComponent('tests-exactprice--big-current-price', true);
            await this.browser.yaAssertViewThemeStorybook('big-current-price', '.ExactPrice');
        });

        it('revert', async function() {
            await this.browser.yaOpenComponent('tests-exactprice--revert', true);
            await this.browser.yaAssertViewThemeStorybook('revert', '.ExactPrice');
        });
    });
});
