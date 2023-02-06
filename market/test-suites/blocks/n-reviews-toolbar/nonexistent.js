import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-reviews-toolbar
 *
 * @param {PageObject.ReviewsToolbar} reviewsToolbar
 */
export default makeSuite('Блок с тулбаром.', {
    feature: 'Тулбар отзывов',
    environment: 'kadavr',
    story: {
        beforeEach() {
            return this.browser.yaOpenPage(this.params.path, this.params.query);
        },

        'При отсутствии рекомендованных отзывов': {
            'не должен отображаться': makeCase({
                id: 'marketfront-2648',
                issue: 'MARKETVERSTKA-29912',
                test() {
                    return this.browser.allure.runStep(
                        'Ищем на странице бок с тулбаром',
                        () => this.reviewsToolbar
                            .isExisting()
                            .should.eventually.to.be.equal(false,
                                'Тулбар отзывов отсутствует на странице'
                            )
                    );
                },
            }),
        },
    },
});
