describe('Storybook', function() {
    describe('Reviews', function() {
        it('default', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-reviews--plain', true);
            await bro.yaWaitForVisible('.ReviewsList');
            await bro.yaAssertViewThemeStorybook('plain', '.ReviewsList');
        });

        it('Работает сортировка', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-reviews--plain', true);
            await bro.yaWaitForVisible('.ReviewsList');

            const waitSorting = await bro.yaWaitElementsChanging(
                '.ReviewsList-Item',
                { timeoutMsg: 'Отзывы не поменялись' },
            );

            await bro.click('.ReviewsList-Controls .ProductListDropDown-Select');
            await bro.selectByAttribute('.ReviewsList-Controls .ProductListDropDown-Select', 'value', 'by_time');

            await bro.yaWaitForVisible('.ReviewSkeleton');
            await bro.yaWaitForHidden('.ReviewSkeleton');

            await waitSorting();
        });

        it('Работает догрузка следующих отзывов', async function() {
            const bro = this.browser;
            await bro.yaOpenComponent('tests-reviews--plain', true);
            await bro.yaWaitForVisible('.ReviewsList');

            const { length: curItemsLength } = await bro.$$('.ReviewsList-Item');

            assert.strictEqual(curItemsLength, 2, 'неверное исходное количество отзывов');
            await bro.click('.ReviewsList-More');
            await bro.yaWaitForVisible('.ReviewSkeleton');
            await bro.yaWaitForHidden('.ReviewSkeleton');

            await bro.waitUntil(async() => {
                const { length } = await bro.$$('.ReviewsList-Item');
                return length === 4;
            }, {
                timeout: 2000,
                timeoutMsg: 'не загрузились следующие отзывы',
            });
        });
    });
});
