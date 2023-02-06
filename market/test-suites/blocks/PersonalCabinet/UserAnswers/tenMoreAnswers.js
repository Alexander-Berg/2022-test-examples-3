import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.widgets.content.UserAnswers} userAnswers
 */

const DEFAULT_VISIBLE_SNIPPETS_COUNT = 10;

export default makeSuite('Если ответов больше 10.', {
    params: {
        answersCount: 'Количество всех ответов пользователя',
    },
    story: {
        'По умолчанию': {
            'кнопка "Показать еще" отображается': makeCase({
                id: 'm-touch-3133',
                async test() {
                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Показать еще" отображается');
                },
            }),
            'отображается ровно 10 сниппетов': makeCase({
                id: 'm-touch-3134',
                async test() {
                    const visibleAnswersCount = await this.userAnswers.getAnswerSnippetsCount();

                    return this.expect(visibleAnswersCount).be.equal(DEFAULT_VISIBLE_SNIPPETS_COUNT,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
        'Клик по кнопке "Показать еще"': {
            'скрывает саму кнопку': makeCase({
                id: 'm-touch-3135',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userAnswers.clickLoadMoreButton(),
                        valueGetter: () => this.userAnswers.isLoadMoreButtonVisible(),
                    });

                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка "Показать еще" скрыта');
                },

            }),
            'загружает оставшиеся сниппеты ответов': makeCase({
                id: 'm-touch-3136',
                async test() {
                    const expectedAnswersCount = this.params.answersCount;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userAnswers.clickLoadMoreButton(),
                        valueGetter: () => this.userAnswers.getAnswerSnippetsCount(),
                    });

                    const visibleAnswersCount = await this.userAnswers.getAnswerSnippetsCount();
                    return this.expect(visibleAnswersCount).be.equal(expectedAnswersCount,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
    },
});
