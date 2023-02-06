hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook-Share', function() {
    describe('ProductProperties', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true);

            await browser.yaAssertViewThemeStorybook('plain', '.ProductProperties');
        });

        it('more text', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [{
                name: 'Position long prop',
                value: '2',
            }]);

            await browser.yaAssertViewThemeStorybook('more', '.ProductProperties');
        });

        it('more text without space', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [{
                name: 'firstPropertyValue',
                value: new Array(10).fill('6000мм').join(''),
            }]);

            await browser.yaAssertViewThemeStorybook('more-space', '.ProductProperties');
        });

        it('count 9', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [{
                name: 'count',
                value: '9',
            }]);

            await browser.yaAssertViewThemeStorybook('count9', '.ProductProperties');
        });

        it('count 9 advanced', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [
                {
                    name: 'count',
                    value: '9',
                },
                {
                    name: 'advanced',
                    value: 'true',
                },
            ]);

            await browser.yaAssertViewThemeStorybook('count9-adv', '.ProductProperties');
        });

        it('count 13 advanced', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [
                {
                    name: 'count',
                    value: '13',
                },
                {
                    name: 'advanced',
                    value: 'true',
                },
            ]);

            await browser.yaAssertViewThemeStorybook('count13-adv', '.ProductProperties');
        });

        it('collapsed', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('share-productproperties--default', true, [
                {
                    name: 'count',
                    value: '13',
                },
            ]);

            await browser.click('.ProductProperties-All');

            await browser.yaAssertViewThemeStorybook('collapsed', '.ProductProperties');
        });
    });
});
