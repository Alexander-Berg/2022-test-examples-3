import {makeCase, makeSuite} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';

/**
 * Тесты на виджет ShopReviewForm.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 */
export default makeSuite('Форма отзыва на магазин. GET-параметр orderId.', {
    id: 'marketfront-4016',
    issue: 'MARKETFRONT-8691',
    feature: 'Оставление отзыва',
    environment: 'kadavr',
    params: {
        expectedOrderId: 'значение параметра orderId',
    },
    story: {
        beforeEach() {
            return this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ShopReviewForm.rating}),
            });
        },
        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'При переданном параметре': {
            'должно устанавливать значение Номер заказа': makeCase({
                async test() {
                    const {expectedOrderId} = this.params;

                    await this.ratingStars.waitForVisible();
                    await this.ratingStars.setRating(5);
                    await this.reviewForm.getOrderIdInputValue()
                        .should.eventually.equal(expectedOrderId, 'Значение в поле ввода совпадает с переданным');
                },
            }),
        },
    },
});
