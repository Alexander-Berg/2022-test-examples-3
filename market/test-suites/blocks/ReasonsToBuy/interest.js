import {makeCase, makeSuite} from 'ginny';
import ReasonsToBuy from '@self/platform/spec/page-objects/widgets/parts/ProductCard/ProductCardReasonsToBuy';

/**
 * Тесты на блок b-reasons-to-buy_type_interest.
 * @param {PageObject.ReasonsToBuy} reasonsToBuy
 */
export default makeSuite('Бейдж "Этот товар купили N человека".', {
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser
                        .waitForVisible(ReasonsToBuy.interest)
                        .should.eventually.to.be.equal(
                            true,
                            'Бейдж "Этот товар купили N человека" должен быть виден'
                        );
                },
            }),
        },
    },
});
