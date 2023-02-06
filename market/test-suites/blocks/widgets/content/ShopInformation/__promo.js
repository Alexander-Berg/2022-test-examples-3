import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на промо-рецепты хаба магазина.
 * @param {PageObject.ShopInformation} shopInformation
 */
export default makeSuite('Промо-рецепты на хабе магазина.', {
    params: {
        shopId: 'Идентификатор магазина',
    },
    story: {
        'По умолчанию': {
            'корректно отображаются': makeCase({
                id: 'marketfront-3276',
                issue: 'MARKETVERSTKA-33020',
                async test() {
                    const [promoRecipe, promocodeRecipe] = await this.shopInformation.getPromoRecipesText();

                    return this.browser.allure.runStep('Проверяем названия рецептов', async () => {
                        await this.expect(promocodeRecipe).to.equal('Промокоды на товары магазина');

                        return this.expect(promoRecipe).to.equal('Акции на товары магазина');
                    });
                },
            }),
            'содержат ссылки на SEO-страницы магазина': makeCase({
                id: 'marketfront-3277',
                issue: 'MARKETVERSTKA-33021',
                async test() {
                    const {shopId} = this.params;
                    const [promoRecipe, promocodeRecipe] = await this.shopInformation.getPromoRecipesUrls();

                    return this.browser.allure.runStep('Проверяем ссылки рецептов', async () => {
                        await this.expect(promocodeRecipe).to.be.link({
                            pathname: `/promo/promocodes_for_${shopId}`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });

                        return this.expect(promoRecipe).to.be.link({
                            pathname: `/promo/promo_for_${shopId}`,
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    });
                },
            }),
        },
    },
});
