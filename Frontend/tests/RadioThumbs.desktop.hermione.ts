hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('RadioThumbs.desktop.hermione.ts', function() {
        it('default', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-radiothumbs--plain', true);

            await bro.yaAssertViewThemeStorybook('plain', '.RadioThumbs');
        });

        it('hover-return', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-radiothumbs--plain', true);

            await bro.$('.RadioThumbs .RadioThumbs-Item:nth-child(3)').moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(3)');
            const { length: curItemsLength } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength, 1, 'Должна быть только одна активная картинка');

            await bro.$('.RadioThumbs .RadioThumbs-Item:nth-child(4)').moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(4)');
            const { length: curItemsLength2 } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength2, 1, 'Должна быть только одна активная картинка');

            await bro.yaAssertViewThemeStorybook('hover', '.RadioThumbs');

            await bro.$('body').moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(1)');
            const { length: curItemsLength3 } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength3, 1, 'Должна быть только одна активная картинка');
        });

        it('click-select', async function() {
            const bro = this.browser;

            await bro.yaOpenComponent('tests-radiothumbs--plain', true);

            const thirdElem = await bro.$('.RadioThumbs .RadioThumbs-Item:nth-child(3)');
            await thirdElem.moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(3)');
            const { length: curItemsLength } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength, 1, 'Должна быть только одна активная картинка');

            await thirdElem.click();

            await bro.$('.RadioThumbs .RadioThumbs-Item:nth-child(4)').moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(4)');
            const { length: curItemsLength2 } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength2, 1, 'Должна быть только одна активная картинка');

            await bro.$('body').moveTo({ xOffset: 10, yOffset: 10 });
            await bro.yaWaitForVisible('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active:nth-child(3)');
            const { length: curItemsLength3 } = await bro.$$('.RadioThumbs .RadioThumbs-Item.RadioThumbs-Item_active');
            assert.strictEqual(curItemsLength3, 1, 'Должна быть только одна активная картинка');

            await bro.yaAssertViewThemeStorybook('hover', '.RadioThumbs');
        });
    });
});
