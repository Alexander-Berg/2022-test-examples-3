import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 */
export default makeSuite('Виджет агитации оставления оценок.' +
    ' Если количество предложений оставления оценки помещается на одну страницу.', {
    params: {
        polls: 'Число предложений оставить оценку',
    },
    story: mergeSuites({
        'По умолчанию': {
            'отображается виджет агитации оставления оценок': makeCase({
                id: 'marketfront-3986',
                test() {
                    return this.reviewPolls.isVisible()
                        .should.eventually.be.equal(true, 'Виджет агитации оставления оценок отображается');
                },
            }),
            'кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-3987',
                test() {
                    return this.reviewPolls.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
            'отображается правильное количество снипетов агитаций': makeCase({
                id: 'marketfront-3988',
                test() {
                    const expectedPolls = this.params.polls;
                    return this.reviewPolls.getReviewPollCardCount()
                        .should.eventually.be.equal(expectedPolls,
                            'Отображается правильное количество снипетов агитаций');
                },
            }),
        },
    }),
});
