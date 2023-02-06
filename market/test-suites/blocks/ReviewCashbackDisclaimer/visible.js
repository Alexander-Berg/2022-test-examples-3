import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductReviewForm} productReviewForm
 */
export default makeSuite('Дисклеймер говорящий об истекшем кэшбеке.', {
    story: {
        'По умолчанию': {
            'виден': makeCase({
                id: 'marketfront-4710',
                issue: 'MARKETFRONT-40974',
                test() {
                    return this.productReviewForm.isCashbackDisclaimerVisible()
                        .should.eventually.to.be.equal(true, 'Дисклеймер отображается');
                },
            }),
        },
    },
});
