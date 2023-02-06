import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на блок n-product-review-item
 * Для отзыва от проверенного покупателя
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Блок с шильдиком "От проверенных покупателей".', {
    feature: 'Отображение отзыва',
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'должен отображаться': makeCase({
                id: 'marketfront-1162',
                issue: 'MARKETVERSTKA-25406',
                test() {
                    return this.productReviewItem.hasUserCpaBadge()
                        .should.eventually.to.be.equal(true, 'Отзыв должен иметь шильдик проверенного покупателя');
                },
            }),
        },
    },
});
