import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import RatingChubbyStarsSuite from '@self/platform/spec/hermione/test-suites/blocks/RatingChubbyStars';
import RatingChubbyStars from '@self/platform/spec/page-objects/RatingChubbyStars';

/**
 * Тест на компонент с отзывом о продукте.
 * @param {PageObject.ProductReview} review
 */
export default makeSuite('Блок с отзывом на продукт.', {
    params: {
        productId: 'ID продукта, который отображен в сниппете.',
        slug: 'Slug продукта',
        reviewId: 'ID отзыва.',
    },
    story: mergeSuites(
        prepareSuite(RatingChubbyStarsSuite, {
            pageObjects: {
                stars() {
                    return this.createPageObject(RatingChubbyStars, {
                        parent: this.review,
                    });
                },
            },
        })
    ),
});
