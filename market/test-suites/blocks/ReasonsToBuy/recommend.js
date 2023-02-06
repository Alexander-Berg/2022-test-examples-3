import {makeCase, makeSuite} from 'ginny';
import ReasonsToBuy from '@self/platform/spec/page-objects/widgets/parts/ProductCard/ProductCardReasonsToBuy';

/**
 * Тесты на блок b-reasons-to-buy_type_recommend.
 * @param {PageObject.ReasonsToBuy} reasonsToBuy
 */
export default makeSuite('Бейдж "Выбор покупателей".', {
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                test() {
                    return this.browser
                        .waitForVisible(ReasonsToBuy.recommend)
                        .should.eventually.to.be.equal(
                            true,
                            'Бейдж "Выбор покупателей" должен быть виден'
                        );
                },
            }),
        },
    },
});
