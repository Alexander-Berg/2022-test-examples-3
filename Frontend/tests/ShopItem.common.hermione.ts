hermione.skip.in('linux-firefox', 'Проблема с FF в storybook GOODS-3845');
describe('Storybook', function() {
    describe('ShopItem', function() {
        it('default', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--default', true);
            await this.browser.yaAssertViewThemeStorybook('default', '.ShopItem');
        });

        it('longName', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--longname', true);
            await this.browser.yaAssertViewThemeStorybook('longName', '.ShopItem');
        });

        it('placeholder', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--placeholder', true);
            await this.browser.yaAssertViewThemeStorybook('placeholder', '.ShopItem');
        });

        it('oldPrice', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--oldprice', true);
            await this.browser.yaAssertViewThemeStorybook('oldPrice', '.ShopItem');
        });

        it('longNameWithOldPrice', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--longnamewitholdprice', true);
            await this.browser.yaAssertViewThemeStorybook('longNameWithOldPrice', '.ShopItem');
        });

        it('проверенный магазин', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--verified', true);
            await this.browser.yaAssertViewThemeStorybook('verified', '.ShopItem');
        });

        it('Не существующий favicon должен заменяться на плейсхолдер', async function() {
            await this.browser.yaOpenComponent(
                'tests-shopitem--plain',
                true,
                [{
                    name: 'hostname',
                    value: 'not-valid-shop-without-favicon',
                }],
            );
            await this.browser.assertView('plain', '.ShopItem');
        });

        it('с лейблом у цены', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--plain', true, [{
                name: 'priceLabel',
                value: 'Хорошая цена',
            }]);
            await this.browser.yaAssertViewThemeStorybook('price-label', '.ShopItem');
        });

        it('с лейблом и подсказкой у цены', async function() {
            await this.browser.yaOpenComponent('tests-shopitem--plain', true, [{
                name: 'priceLabel',
                value: 'Хорошая цена',
            }, {
                name: 'priceLabelDisclaimer',
                value: 'Сейчас это самая низкая цена на товар из всех известных Яндексу предложений',
            }]);
            await this.browser.yaAssertViewThemeStorybook('price-label-disclaimer', '.ShopItem');
        });
    });
});
