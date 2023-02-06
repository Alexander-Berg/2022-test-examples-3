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
                id: 'm-touch-3131',
                async test() {
                    return this.userAnswers.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка "Показать еще" не отображается');
                },
            }),
            'отображается верное количество сниппетов': makeCase({
                id: 'm-touch-3132',
                async test() {
                    const expectedCount = this.params.answersCount;
                    const visibleAnswersCount = await this.userAnswers.getAnswerSnippetsCount();

                    return this.expect(visibleAnswersCount).be.equal(expectedCount,
                        'Отображается верное количество сниппетов');
                },
            }),
        },
    },
});
