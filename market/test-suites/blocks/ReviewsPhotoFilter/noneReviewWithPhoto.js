import {makeSuite, makeCase} from 'ginny';
import ReviewsPhotoFilter from '@self/platform/spec/page-objects/components/ReviewsPhotoFilter';

/**
 * Тест виджета "Фильтр по фото"
 */
export default makeSuite('Нет отзывов с фото.', {
    feature: 'Фильтр с фото',
    story: {
        beforeEach() {
            this.setPageObjects({
                reviewsPhotoFilter: () => this.createPageObject(ReviewsPhotoFilter),
            });

            return this.browser.yaOpenPage(this.params.path, this.params.query);
        },

        'Фильтр "С фото" должен быть доступным (enabled)': makeCase({
            id: 'marketfront-517',
            issue: 'MARKETVERSTKA-23314',
            test() {
                return this.reviewsPhotoFilter
                    .isDisabled()
                    .should.eventually.to.be.equal(true, 'Фильтра "С фото" должен быть disabled');
            },
        }),
    },
});
