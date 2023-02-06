import {makeCase, makeSuite, mergeSuites} from 'ginny';

/**
 * @param {PageObject.widgets.content.ReviewPolls} reviewPolls
 */
export default makeSuite('Виджет агитации оставления оценок.', {
    story: mergeSuites({
        'По умолчанию не отображается.': makeCase({
            id: 'marketfront-4005',
            test() {
                return this.reviewPolls.isVisible()
                    .should.eventually.be.equal(false, 'Виджет агитации оставления оценок не отображается');
            },
        }),
    }),
});
