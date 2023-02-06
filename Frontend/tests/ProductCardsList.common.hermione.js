hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ProductCardsList', function() {
        //В appium-chrome-phone на этих версиях гермионы и браузера у этого теста возникает клиентская ошибка
        //Unexpected token при снятии скриншота
        hermione.only.notIn('appium-chrome-phone');
        it('narrow', async function() {
            await this.browser.yaOpenComponent('tests-productcardslist--narrow', true);
            await this.browser.yaAssertViewThemeStorybook('narrow', '.ProductCardsList', {
                allowViewportOverflow: true,
            });
        });

        it('loading-more', async function() {
            await this.browser.yaOpenComponent('tests-productcardslist--loading-more', true);
            await this.browser.yaAssertViewThemeStorybook('loading-more', '.ProductCardsList', {
                allowViewportOverflow: true,
            });
        });
    });
});
