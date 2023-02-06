import {makeSuite, makeCase} from 'ginny';


import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';

/**
 * Тесты на блок n-review-form при выставлении ТОЛЬКО оценки
 * а также на блок "Спасибо за оценку"
 *
 * @param {PageObject.ReviewForm} reviewForm
 * @param {PageObject.ReviewFormProduct} reviewFormProduct
 */
export default makeSuite('Форма отзыва на товар. Добавление оценки на товар.', {
    feature: 'Оставление оценки на товар',
    issue: 'MARKETVERSTKA-24800',
    id: 'marketfront-864',
    params: {
        productId: 'Id товара',
        slug: 'Слаг',
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: ProductReviewForm.stepOne}),
                reviewItemRating: () => this.createPageObject(RatingStars),
            });
        },

        afterEach() {
            return this.browser.yaRemovePreventQuit();
        },

        'Форма только с заполненной оценкой': {
            'должна вести обратно на товар': makeCase({
                async test() {
                    const {reviewForm, reviewItemRating, browser, params} = this;

                    await reviewForm.isVisible()
                        .should.eventually.equal(true, 'Форма оставления оценки видна');

                    await browser.yaScenario(this, 'productReviews.fillForm.onlyGrade');
                    await reviewForm.submitFirstStep();
                    await reviewForm.waitForSecondStep();
                    const currentUrl = await this.browser.allure.runStep(
                        'Сохраняем оценку',
                        () => this.browser.yaWaitForChangeUrl(() => this.reviewForm.submitSecondStep())
                    );
                    const expectedUrl = await this.browser.yaBuildURL('market:product', {
                        productId: params.productId,
                        slug: params.slug,
                    });

                    await this.expect(currentUrl).to.be.link(expectedUrl, {
                        skipProtocol: true,
                        skipHostname: true,
                    });

                    const rating = await reviewItemRating.getRating();
                    return this.expect(rating).to.be.equal(4);
                },
            }),
        },
    },
});
