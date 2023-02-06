hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('CashbackCoins', function() {
        it('Проверка внешнего вида для разных значений на примере Плюса', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-cashbackpluscoins--plain', true, [{
                name: 'amount',
                value: '20',
            }]);
            await browser.yaAssertViewThemeStorybook('plain', '.CashbackCoins');

            await browser.yaOpenComponent('tests-cashbackpluscoins--plain', true, [{
                name: 'amount',
                value: '21',
            }]);
            await browser.yaAssertViewThemeStorybook('plain-21', '.CashbackCoins');

            await browser.yaOpenComponent('tests-cashbackpluscoins--plain', true, [{
                name: 'amount',
                value: '25000',
            }]);
            await browser.yaAssertViewThemeStorybook('plain-25000', '.CashbackCoins');

            await browser.yaOpenComponent('tests-cashbackpluscoins--plain', true, [{
                name: 'amount',
                value: '0',
            }]);
            await browser.yaShouldNotBeVisible('.CashbackCoins', 'Компонент не должен рендериться при 0 баллов');

            await browser.yaOpenComponent('tests-cashbackpluscoins--plain', true, [{
                name: 'amount',
                value: '-17826',
            }]);
            await browser.yaShouldNotBeVisible('.CashbackCoins', 'Компонент не должен рендериться при отрицательных баллах');
        });

        it('Проверка внешнего вида МВидео', async function() {
            const { browser } = this;

            await browser.yaOpenComponent('tests-cashbackmvideocoins--plain', true, [{
                name: 'amount',
                value: '20',
            }]);
            await browser.yaAssertViewThemeStorybook('plain', '.CashbackCoins');
        });
    });
});
