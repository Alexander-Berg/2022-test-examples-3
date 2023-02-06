import {makeCase, makeSuite} from 'ginny';

const DEFAULT_VISIBLE_SNIPPETS_COUNT = 10;

/**
 * @param {PageObject.widgets.content.UserAnswers} userAnswers
 */
export default makeSuite('Если ответов больше 10.', {
    params: {
        answersCount: 'Количество всех ответов пользователя',
    },
    story: {
        'По умолчанию': {
            'кнопка "Показать еще" отображается': makeCase({
                id: 'marketfront-3862',
                async test() {
                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка "Показать еще" отображается');
                },
            }),
            'отображается ровно 10 сниппетов': makeCase({
                id: 'marketfront-3863',
                async test() {
                    const visibleAnswersCount = await this.userAnswers.getAnswerSnippetsCount();

                    return this.expect(visibleAnswersCount).be.equal(DEFAULT_VISIBLE_SNIPPETS_COUNT,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
        'Клик по кнопке "Показать еще"': {
            'скрывает саму кнопку': makeCase({
                id: 'marketfront-3864',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.userAnswers.clickLoadMoreButton(),
                        valueGetter: () => this.userAnswers.isLoadMoreButtonVisible(),
                    });

                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка "Показать еще" скрыта');
                },

            }),
            'загружает оставшиеся сниппеты вопросов': makeCase({
                id: 'marketfront-3865',
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
