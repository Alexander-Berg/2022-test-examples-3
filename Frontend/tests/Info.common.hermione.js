hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('MainPage-Info', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-mainpage-info--plain', true);
            await browser.yaAssertViewThemeStorybook('plain', '.MainPage-Info');
        });
    });
});
