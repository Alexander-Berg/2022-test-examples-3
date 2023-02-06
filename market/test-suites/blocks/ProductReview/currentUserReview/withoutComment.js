import {makeSuite, makeCase} from 'ginny';
import {DEFAULT_TIMEOUT} from '@self/platform/spec/hermione/commands/yaWaitForPageReloadedExtended';

/**
 * Тесты на компонент productReview,
 * который является отзывом текущего пользователя
 * Характерен наличием кнопок "Изменить" и "Удалить"
 *
 * @param {PageObject.ProductReview} productReview
 */
export default makeSuite('Блок с отзывом от текущего пользователя без комментариев.', {
    environment: 'kadavr',
    story: {
        'Ссылка "Изменить".': {
            'При клике': {
                'открывается новая страница редактирования отзыва.': makeCase({
                    feature: 'Редактирование отзыва',
                    id: 'marketfront-805',
                    params: {
                        path: 'Имя роута редактирования отзыва',
                        query: 'Параметры роута',
                    },

                    test() {
                        return this.browser.yaWaitForPageReloadedExtended(
                            () => this.productReview.clickEditButton(),
                            DEFAULT_TIMEOUT
                        )
                            .then(() => Promise.all([
                                this.browser.getUrl(),
                                this.browser.yaBuildURL(this.params.path, this.params.query),
                            ])
                                .then(([openedUrl, expectedPath]) => this
                                    .expect(openedUrl, 'Проверяем что URL изменился')
                                    .to.be.link({
                                        pathname: expectedPath,
                                    }, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                                )
                            );
                    },
                }),
            },
        },

    },
});
