import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.widgets.content.UserAnswers} userAnswers
 */

export default makeSuite('Если ответов меньше 10.', {
    params: {
        answersCount: 'Количество отображаемых ответов пользователя',
    },
    story: {
        'По умолчанию': {
            'кнопка "Показать" еще не отображается': makeCase({
                id: 'marketfront-3860',
                async test() {
                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка "Показать еще" не отображается');
                },
            }),
            'отображается верное количество сниппетов': makeCase({
                id: 'marketfront-3861',
                async test() {
                    const expectedAnswersCount = this.params.answersCount;
                    const visibleAnswersCount = await this.userAnswers.getAnswerSnippetsCount();

                    return this.expect(visibleAnswersCount).be.equal(expectedAnswersCount,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
    },
});
