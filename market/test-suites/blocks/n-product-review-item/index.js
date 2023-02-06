import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import RatingSuite from '@self/platform/spec/hermione/test-suites/blocks/rating';
import Rating from '@self/platform/spec/page-objects/n-rating';
/**
 * Тесты на блок n-product-review-item
 * Содержит в себе тесты на общие элементы
 *
 * @param {PageObject.ProductReviewItem} productReviewItem
 */
// TODO: Удалить
export default makeSuite('Блок с отзывом.', {
    feature: 'Отображение отзыва',
    id: 'marketfront-801',
    story: mergeSuites(
        prepareSuite(RatingSuite, {
            pageObjects: {
                rating() {
                    return this.createPageObject(Rating, {
                        parent: this.productReviewItem,
                    });
                },
            },
        })
    ),
});
