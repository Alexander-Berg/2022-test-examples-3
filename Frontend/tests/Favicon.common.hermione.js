hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ShopItem', function() {
        it('favicon-set', async function() {
            await this.browser.yaOpenComponent('tests-favicon--favicon-set', true);
            await this.browser.yaWaitForBackgroundLoaded('.Favicon', {
                message: 'Не загрузились фавиконки с favicon.yandex.net',
                timeout: 3500,
            });
            await this.browser.yaAssertViewThemeStorybook('default', '.favicon-set');
        });
    });
});
