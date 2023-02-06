import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.ProductReviewForm} productReviewForm
 */
export default makeSuite('Дисклеймер говорящий об истекшем кэшбеке.', {
    story: {
        'По умолчанию': {
            'не виден': makeCase({
                id: 'marketfront-4711',
                issue: 'MARKETFRONT-40974',
                test() {
                    return this.browser.allure.runStep('Проверяем видимость дисклеймера', () =>
                        this.productReviewForm.cashbackDisclaimer.isVisible()
                            .catch(() => false).should.eventually.to.be.equal(false, 'Дисклеймер не отображается')
                    );
                },
            }),
        },
    },
});
