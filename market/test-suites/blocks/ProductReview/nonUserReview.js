import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент ProductReview,
 * которые представляет отзыв других пользователей
 * Характерен наличием кнопки "Ответить" для написания каментария
 *
 * @param {PageObject.ProductReview} productReview
 * @property {object} this.params - содержит признак авторизованности пользователя (isAutorized)
 */
export default makeSuite('Блок с отзывом от другого пользователя.', {
    feature: 'Отображение отзыва',
    story: {
        'По умолчанию': {
            'должен содержать ссылку "Ответить"': makeCase({
                id: 'marketfront-798',
                test() {
                    return this.productReview
                        .hasReplyLink()
                        .should.eventually.be.equal(true, 'Блок содержит ссылку «Ответить»');
                },
            }),

            'не должен содержать кнопки "Изменить"': makeCase({
                id: 'marketfront-799',
                test() {
                    return this.productReview
                        .hasEditLink()
                        .should.eventually.be.equal(false, 'Блок не содержит кнопки «Изменить отзыв»');
                },
            }),

            'не должен содержать кнопки "Удалить"': makeCase({
                id: 'marketfront-800',
                test() {
                    return this.productReview
                        .hasDeleteLink()
                        .should.eventually.be.equal(false, 'Блок не содержит кнопки «Удалить»');
                },
            }),
        },

        'Ссылка "Ответить".': {
            'При клике': {
                'должна показываться форма для комментария под отзывом': makeCase({
                    id: 'MARKETFRONT-4059',
                    async test() {
                        await this.productReview.clickReplyButton();
                        const fromIsVisible = this.params.isAuthorized;
                        const actionButton = fromIsVisible ?
                            'не отображается для авторизованного' :
                            'отображается для неавторизованного';
                        const actionForm = fromIsVisible ?
                            'отображается для авторизованного' :
                            'не отображается для неавторизованного';
                        await this.productReview.isAuthorizeButtonVisible()
                            .should.eventually.be.equal(
                                !fromIsVisible,
                                `Кнопка Войдите ${actionButton}`);
                        return this.productReview.isCommentFormVisible()
                            .should.eventually.be.equal(
                                fromIsVisible,
                                `Форма комментария ${actionForm}`);
                    },
                }),
            },
        },
    },
});
