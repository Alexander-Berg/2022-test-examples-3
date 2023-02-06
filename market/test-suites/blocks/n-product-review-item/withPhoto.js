import {makeSuite, makeCase} from 'ginny';
import ReviewPhotos from '@self/platform/spec/page-objects/n-review-photos';

/**
 * Тесты на блок n-product-review-item
 * Для отзыва с фотографией
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
export default makeSuite('Блок с фотографией для отзыва.', {
    feature: 'Отображение отзыва',
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                reviewPhotos: () => this.createPageObject(ReviewPhotos, {
                    parent: this.productReviewItem,
                }),
            });
        },

        'По умолчанию': {
            'должен отображаться': makeCase({
                id: 'marketfront-518',
                issue: 'MARKETVERSTKA-23314',
                test() {
                    return this.reviewPhotos.isVisible()
                        .should.eventually.to.be.equal(true, 'Отзыв должен иметь фотографии');
                },
            }),
        },
    },
});
