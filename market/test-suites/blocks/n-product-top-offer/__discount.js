import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-top-offer
 * @param {PageObject.ProductTopOffer} topOffer
 */
export default makeSuite('Сниппет оффера из ТОП6 со скидкой.', {
    feature: 'Сниппет.',
    story: {
        'Бейдж скидки': {
            'по умолчанию': {
                'должен присутствовать': makeCase({
                    id: 'marketfront-1561',
                    issue: 'MARKETVERSTKA-26171',
                    async test() {
                        const isDiscountBadgeExists = await this.topOffer.hasDiscountBadge();

                        return this.expect(isDiscountBadgeExists).to.be.equal(true, 'Бейдж скидки присутствует');
                    },
                }),
            },
        },
    },
});
