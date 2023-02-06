import {makeCase, makeSuite} from 'ginny';

import {offerMock as kettleOfferMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import {spreadDiscountCountPromo} from '@self/platform/spec/hermione/fixtures/promo/promo.mock';

/**
 * Тесты на блок CartItemDealTerms
 * @param {PageObject.CartItemDealTerms} cartItemDealTerms
 */
export default makeSuite('Промо (блок DealTerms)', {
    environment: 'kadavr',
    feature: 'Промо',
    story: {
        async beforeEach() {
            // переиспользуем состояние KADAVR установленне в родительской/вызывающей сюите - cartItem
            // ...market/platform.desktop/spec/hermione/test-suites/blocks/cartItem
            await this.browser.setState(
                // модифицируем состояние ранее установлленного в состоянии товара (заведомо известного)
                `report.collections.offer.${kettleOfferMock.wareId}.promos`,
                // моки необходимых promo ( raw / не нормализованных )
                [spreadDiscountCountPromo]
            );

            await this.browser.yaReactPageReload();
        },
        'Если для позиции (оффера) в корзине установлена промо-акция "Скидка за количество товара"': {
            'Компонент промо-акции "Скидка за количество товара" должен быть показан': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4864',
                issue: 'MARKETFRONT-54338',
                async test() {
                    return this.cartOfferDealTerms.isSpreadDiscountCountTermsVisible()
                        .should.eventually.be.equal(true,
                            'Компонент промо-акции "Скидка за количество товара" должен быть показан'
                        );
                },
            }),
            'Текст-описание в компоненте промо-акции "Скидка за количество товара" должнен быть корректным': makeCase({
                environment: 'kadavr',
                id: 'marketfront-4864',
                issue: 'MARKETFRONT-54338',
                async test() {
                    return this.cartOfferDealTerms.getSpreadDiscountCountTermsText()
                        .should.eventually.be.equal('2 шт. – 10%, 5 шт. – 20%',
                            'Текст-описание в компоненте промо-акции "Скидка за количество товара" должнен быть корректным'
                        );
                },
            }),
        },
        'Если у позиции в корзине (оффере) есть визуалилируемые промо-акции': {
            'блок DealTerms должен быть показан': makeCase({
                environment: 'kadavr',
                async test() {
                    return this.cartOfferDealTerms.isVisible()
                        .should.eventually.be.equal(true,
                            'Блок DealTerms c визуализацийе промо-акций не был показан'
                        );
                },
            }),
        },
    },
});
