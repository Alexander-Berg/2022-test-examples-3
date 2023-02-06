import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-product-review-item,
 * которые представляет отзыв других пользователей
 * Характерен наличием кнопки "Ответить" для написания каментария
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Блок с отзывом от другого пользователя.', {
    feature: 'Отображение отзыва',
    story: {
        'По умолчанию': {
            'должен содержать ссылку "Ответить"': makeCase({
                id: 'marketfront-798',
                test() {
                    return this.productReviewItem
                        .hasReplyLink()
                        .should.eventually.be.equal(true, 'Блок содержит ссылку «Ответить»');
                },
            }),

            'не должен содержать кнопки "Изменить"': makeCase({
                id: 'marketfront-799',
                test() {
                    return this.productReviewItem
                        .hasEditLink()
                        .should.eventually.be.equal(false, 'Блок не содержит кнопки «Изменить»');
                },
            }),

            'не должен содержать кнопки "Удалить"': makeCase({
                id: 'marketfront-800',
                test() {
                    return this.productReviewItem
                        .hasDeleteLink()
                        .should.eventually.be.equal(false, 'Блок не содержит кнопки «Удалить»');
                },
            }),
        },
    },
});
