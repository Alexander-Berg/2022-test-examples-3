import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 */
export default makeSuite('Виджет агитации оставления оценок с конкретным SKU.' +
    ' Если количество предложений оставления оценки помещается на одну страницу.', {
    params: {
        polls: 'Число предложений оставить оценку',
    },
    story: mergeSuites({
        'По умолчанию': {
            'отображается виджет агитации оставления оценок': makeCase({
                id: 'marketfront-4194',
                issue: 'MARKETFRONT-19739',
                test() {
                    return this.reviewPolls.isVisible()
                        .should.eventually.be.equal(true, 'Виджет агитации оставления оценок отображается');
                },
            }),
            'отображается заголовок SKU': makeCase({
                id: 'marketfront-4194',
                issue: 'MARKETFRONT-19739',
                async test() {
                    const expectedTitle = this.params.title;

                    return this.reviewPolls.getFirstReviewPollCardTitle()
                        .should.eventually.be.equal(expectedTitle, 'Заголовок соответствует ожидаемому');
                },
            }),
            'кнопка «Показать еще» не отображается.': makeCase({
                id: 'marketfront-4194',
                issue: 'MARKETFRONT-19739',
                test() {
                    return this.reviewPolls.isLoadMoreButtonVisible()
                        .should.eventually.be.equal(false, 'Кнопка «Показать еще» не отображается');
                },
            }),
            'отображается правильное количество снипетов агитаций': makeCase({
                id: 'marketfront-4194',
                issue: 'MARKETFRONT-19739',
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
