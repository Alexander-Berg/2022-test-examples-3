import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import RatingStarsSuite from '@self/platform/spec/hermione/test-suites/blocks/ratingStars';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';

/**
 * Тесты на компонент ProductReview
 * Содержит в себе тесты на общие элементы
 *
 * @param {PageObject.ProductReview} productReview
 * @property {object} this.params - содежржит ожидаемое количество звезд в блоке (expectedRating)
 */
export default makeSuite('Блок с отзывом.', {
    feature: 'Отображение отзыва',
    id: 'marketfront-801',
    story: mergeSuites(
        prepareSuite(RatingStarsSuite, {
            pageObjects: {
                ratingStars() {
                    return this.createPageObject(RatingStars, {
                        parent: this.productReview,
                    });
                },
            },
        })
    ),
});
