import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 */
export default makeSuite('Виджет агитации оставления оценок.' +
    ' Если количество предложений оставления оценки помещается на три страницы.', {
    params: {
        polls: 'Число предложений оставить оценку',
    },
    story: mergeSuites({
        'По умолчанию': {
            'кнопка «Показать еще» отображается.': makeCase({
                id: 'marketfront-3989',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    return this.reviewPolls.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(true, 'Кнопка «Показать еще» отображается');
                },
            }),
            'отображается правильное количество вопросов': makeCase({
                id: 'marketfront-3990',
                async test() {
                    const expectedReviewPollCardCount = 6;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    return this.reviewPolls.getReviewPollCardCount()
                        .should.eventually.be.equal(expectedReviewPollCardCount,
                            'Отображается правильное количество агитаций');
                },
            }),
        },
        'При клике по кнопке «Показать еще» дважды.': {
            'Подгружается все вопросы.': makeCase({
                id: 'marketfront-3991',
                async test() {
                    const expectedReviewPollCardCount = this.params.polls;

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

                    return this.reviewPolls.getReviewPollCardCount()
                        .should.eventually.be.equal(expectedReviewPollCardCount,
                            'Отображается правильное количество агитаций');
                },
            }),
            'Кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3992',
                async test() {
                    await this.browser.yaWaitForChangeValue({
                        action: () => this.reviewPolls.clickLoadMoreButton(),
                        valueGetter: () => this.reviewPolls.getReviewPollCardCount(),
                    });

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
