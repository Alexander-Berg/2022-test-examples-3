import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductReviewFormMicroMainFields} productReviewFormMicroMainFields
 */
export default makeSuite('Дисклеймер говорящий об истекшем кэшбеке.', {
    story: {
        'По умолчанию': {
            'не виден': makeCase({
                id: 'm-touch-3591',
                issue: 'MARKETFRONT-40974',
                test() {
                    return this.browser.allure.runStep('Проверяем видимость дисклеймера', () =>
                        this.productReviewFormMicroMainFields.cashbackDisclaimer.isVisible()
                            .catch(() => false).should.eventually.to.be.equal(false, 'Дисклеймер не отображается')
                    );
                },
            }),
        },
    },
});
