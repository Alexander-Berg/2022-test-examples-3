import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductReviewFormMicroMainFields} productReviewFormMicroMainFields
 */
export default makeSuite('Дисклеймер говорящий об истекшем кэшбеке.', {
    story: {
        'По умолчанию': {
            'виден': makeCase({
                id: 'm-touch-3590',
                issue: 'MARKETFRONT-40974',
                test() {
                    return this.productReviewFormMicroMainFields.isCashbackDisclaimerVisible()
                        .should.eventually.to.be.equal(true, 'Дисклеймер отображается');
                },
            }),
        },
    },
});
