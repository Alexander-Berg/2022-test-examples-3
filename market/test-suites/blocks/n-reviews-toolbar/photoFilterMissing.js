import {makeSuite, makeCase} from 'ginny';


/**
 * Тесты на блок n-reviews-toolbar
 *
 * @param {PageObject.ReviewsToolbar} reviewsToolbar
 */
export default makeSuite('Тулбар отзывов. Фильтр "С фото".', {
    feature: 'Фильтр с фото',
    story: {
        beforeEach() {
            return this.browser.yaOpenPage(this.params.path, this.params.query);
        },

        'При отсутствии отзывов от проверенных пользователей': {
            'не должен отображаться': makeCase({
                id: 'marketfront-517',
                issue: 'MARKETVERSTKA-23314',
                test() {
                    return this.reviewsToolbar
                        .hasFilterTypePhoto()
                        .should.eventually.to.be.equal(false, 'Фильтра "С фото" не должно быть');
                },
            }),
        },
    },
});
