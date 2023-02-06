import {makeSuite, makeCase} from 'ginny';
import ReviewPhotoGallery from '@self/platform/spec/page-objects/components/ProductReview/ReviewPhotoGallery';

/**
 * Для отзыва с фотографией
 *
 * @param {PageObject.ProductReview} productReview
 */
export default makeSuite('Блок с фотографией для отзыва.', {
    feature: 'Отображение отзыва',
    environment: 'kadavr',
    story: {
        beforeEach() {
            this.setPageObjects({
                reviewPhotoGallery: () => this.createPageObject(ReviewPhotoGallery, {
                    parent: this.productReview,
                }),
            });
        },

        'По умолчанию': {
            'должен отображаться': makeCase({
                id: 'marketfront-518',
                issue: 'MARKETVERSTKA-23314',
                test() {
                    return this.reviewPhotoGallery.isVisible()
                        .should.eventually.to.be.equal(true, 'Отзыв должен иметь фотографии');
                },
            }),
        },
    },
});
