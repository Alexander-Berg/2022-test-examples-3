hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ListCut', function() {
        it('default', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-listcut--plain', true);
            await browser.yaAssertViewThemeStorybook('collapsed', '.ListCut');
            await browser.click('.ListCut-CutControl');
            await browser.yaAssertViewThemeStorybook('expanded', '.ListCut');
        });

        it('hide', async function() {
            const { browser } = this;
            const hideText = 'Скрыть';
            let items;

            await browser.yaOpenComponent('tests-listcut--plain', true, [
                { name: 'hideText', value: hideText },
            ]);
            items = await browser.$$('.ListCut-Items div');
            assert.strictEqual(items.length, 2, 'изначально должно быть два элемента');

            await browser.click('.ListCut-CutControl');
            items = await browser.$$('.ListCut-Items div');
            assert.strictEqual(items.length, 5, 'клик должен показывать пять элементов');

            const control = await browser.$('.ListCut-CutControl');
            assert.strictEqual(await control.getText(), hideText, 'отличается текст кнопки скрытия элементов');
        });

        it('step', async function() {
            const { browser } = this;
            let items;

            await browser.yaOpenComponent('tests-listcut--plain', true, [
                { name: 'step', value: '2' },
            ]);
            items = await browser.$$('.ListCut-Items div');
            assert.strictEqual(items.length, 2, 'изначально должно быть два элемента');

            await browser.click('.ListCut-CutControl');
            items = await browser.$$('.ListCut-Items div');
            assert.strictEqual(items.length, 4, 'первый клик должен добавлять ещё два элемента');

            await browser.click('.ListCut-CutControl');
            items = await browser.$$('.ListCut-Items div');
            assert.strictEqual(items.length, 5, 'второй клик должен добавлять ещё один последний элемент');

            const control = await browser.$('.ListCut-CutControl');
            assert.isFalse(await control.isExisting(), 'кнопка разворачивания должна быть скрыта');
        });
    });
});
