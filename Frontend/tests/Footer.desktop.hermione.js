hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('Footer', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-footer--plain', true);

            await browser.setViewportSize({ width: 1920, height: 700 });
            await browser.yaAssertViewThemeStorybook('plain-1920', '.Footer');

            await browser.setViewportSize({ width: 1024, height: 700 });
            await browser.yaAssertViewThemeStorybook('plain-1024', '.Footer');

            await browser.setViewportSize({ width: 576, height: 700 });
            await browser.yaAssertViewThemeStorybook('plain-576', '.Footer', {
                allowViewportOverflow: true,
            });
        });

        it('emptySearch', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-footer--empty-search', true);

            await browser.setViewportSize({ width: 1920, height: 700 });
            await browser.assertView('empty-search-1920', '.Footer');

            await browser.setViewportSize({ width: 1024, height: 700 });
            await browser.assertView('empty-search-1024', '.Footer');

            await browser.setViewportSize({ width: 576, height: 700 });
            await browser.assertView('empty-search-576', '.Footer', {
                allowViewportOverflow: true,
            });
        });
    });
});
