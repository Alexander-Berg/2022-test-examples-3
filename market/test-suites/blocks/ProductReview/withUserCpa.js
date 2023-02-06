import {makeSuite, makeCase} from 'ginny';

/**
 * Для отзыва от проверенного покупателя
 *
 * @param {PageObject.ProductReview} productReview
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
                    return this.productReview.hasUserCpaBadge()
                        .should.eventually.to.be.equal(true, 'Отзыв должен иметь шильдик проверенного покупателя');
                },
            }),
        },
    },
});
