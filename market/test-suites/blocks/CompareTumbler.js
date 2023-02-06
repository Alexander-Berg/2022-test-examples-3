import {makeCase, makeSuite} from 'ginny';
import {routes} from '@self/platform/spec/hermione/configs/routes';

/**
 * Тесты на блок CompareTumbler
 * @param {PageObject.Index} compareTumbler
 * @param {PageObject.Notification} notification
 * @param {PageObject.ProductCompare} productCompare
 */
export default makeSuite('Тумблер добавления товара к сравнению.', {
    story: {
        'При клике': {
            'товар добавлен к сравнению': makeCase({
                id: 'm-touch-1146',
                issue: 'MOBMARKET-5018',
                test() {
                    const productId = routes.product.dress.productId;

                    return this.compareTumbler.click()
                        .then(() => this.notification.isTextVisible())
                        .then(() => this.notification.clickText())
                        .then(() => this.productCompare.getModelByDataId(productId))
                        .should.eventually.be.equal(
                            true, 'Проверяем что искомая модель присутствует в списке сравнения'
                        );
                },
            }),
        },
    },
});
