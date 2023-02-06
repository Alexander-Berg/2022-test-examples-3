import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 */
export default makeSuite('Виджет агитации оставления оценок.' +
    ' Если количество предложений оставления оценки помещается на две страницы.', {
    params: {
        polls: 'Число предложений оставить оценку',
    },
    story: mergeSuites({
        'По умолчанию': {
            'кнопка «Показать еще» отображается.': makeCase({
                id: 'marketfront-3993',
                test() {
                    return this.reviewPolls.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка «Показать еще» отображается');
                },
            }),
            'отображается правильное количество вопросов': makeCase({
                id: 'marketfront-3994',
                test() {
                    const expectedPollCardCount = 3;
                    return this.reviewPolls.getReviewPollCardCount()
                        .should.eventually.be.equal(expectedPollCardCount,
                            'Отображается правильное количество агитаций');
                },
            }),
        },
        'При клике по кнопке «Показать еще».': {
            'Подгружается еще одна страница.': makeCase({
                id: 'marketfront-3995',
                async test() {
                    const expectedPollCardCount = this.params.polls;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    return this.reviewPolls.getReviewPollCardCount()
                        .should.eventually.be.equal(expectedPollCardCount,
                            'Отображается правильное количество вопросов');
                },
            }),
            'Кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3996',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    return this.reviewPolls.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
        },
    }),
});
