import {makeCase, makeSuite, mergeSuites} from 'ginny';

import {offerMock as kettleOfferMock} from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';

import {unitInfo} from '@self/platform/spec/hermione/fixtures/unitInfo';

/**
 * Тесты на блок CartItemDealTerms
 * @param {PageObject.CartItemDealTerms} cartItemDealTerms
 */
export default makeSuite('Нестанадртные единицы измерения продажи товара', {
    environment: 'kadavr',
    feature: 'UnitInfo',
    story: mergeSuites(
        {
            async beforeEach() {
                // переиспользуем состояние KADAVR установленное в родительском/вызывающем сюите - cartItem
                // ...market/platform.desktop/spec/hermione/test-suites/blocks/cartItem
                await this.browser.setState(
                    // модифицируем состояние ранее установлленного в состоянии товара (заведомо известного)
                    `report.collections.offer.${kettleOfferMock.wareId}.unitInfo`,
                    // моки необходимых unitInfo ( raw / не нормализованных )
                    unitInfo
                );

                await this.browser.yaReactPageReload();
            },
        },
        makeSuite('Если у позиции в корзине (оффере) есть нестандартные единицы продажи.', {
            story: mergeSuites(
                {
                    'По умолчанию': {
                        'в каунтере должна выводиться 1 упаковка': makeCase({
                            id: 'marketfront-5403',
                            issue: 'MARKETFRONT-78827',
                            environment: 'kadavr',
                            async test() {
                                return this.amountSelect.getCurrentCountText()
                                    .should.eventually.be.equal('1 уп', 'в каунтере отображается 1 уп');
                            },
                        }),
                    },
                }
            ),
        })
    ),
});
