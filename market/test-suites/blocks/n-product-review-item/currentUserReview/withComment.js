import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-product-review-item с комментарием,
 * который является отзывом текущего пользователя
 * Характерен наличием кнопки "Удалить" и отсутствием кнопки "Изменить"
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Блок с отзывом от текущего пользователя с комментарием.', {
    environment: 'kadavr',
    story: {
        'По умолчанию': {
            'не должен содержать ссылку "Изменить"': makeCase({
                feature: 'Редактирование отзыва',
                id: 'marketfront-2691',
                test() {
                    return this.productReviewItem
                        .hasEditLink()
                        .should.eventually.to.be.equal(false, 'Кнопка "Изменить" не отображается');
                },
            }),

            'должен содержать ссылку "Удалить"': makeCase({
                feature: 'Удаление отзыва',
                id: 'marketfront-2690',
                test() {
                    return this.productReviewItem
                        .hasDeleteLink()
                        .should.eventually.to.be.equal(true, 'Кнопка "Удалить" отображается');
                },
            }),
        },
    },
});
