import {makeCase, makeSuite} from 'ginny';
import ReasonsToBuy from '@self/platform/spec/page-objects/widgets/parts/ProductCard/ProductCardReasonsToBuyBest';

/**
 * Тесты на блок b-reasons-to-buy_type_best.
 * @param {PageObject.ReasonsToBuy} reasonsToBuy
 */
export default makeSuite('Бейдж "Покупателям нравится".', {
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser
                        .waitForVisible(ReasonsToBuy.root)
                        .should.eventually.to.be.equal(
                            true,
                            'Бейдж "Покупателям нравится" должен быть виден'
                        );
                },
            }),
        },
    },
});
