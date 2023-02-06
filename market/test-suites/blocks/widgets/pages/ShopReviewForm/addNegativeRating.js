import {makeSuite, makeCase} from 'ginny';

import {shopReview} from '@self/platform/spec/hermione/configs/reviews/shop/add';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';

/**
 * Тесты на виджет ShopReviewForm. Отзывы на магазин с негативной оценкой.
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 */
export default makeSuite('Форма создания отзыва на магазин с негативной оценкой.', {
    story: {
        beforeEach() {
            this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: this.reviewForm.averageGrade}),
            });
        },
        'Отзыв содержит низкий грейд (1 звезду) и отстуствует текст, то отзыв сохранить нельзя.': makeCase({
            issue: 'MARKETVERSTKA-33875',
            id: 'marketfront-3370',
            async test() {
                await this.ratingStars.setRating(1);
                const urlBeforeSubmit = await this.browser.getUrl();

                const formData = shopReview.firstStep;
                await this.reviewForm.setProInputValue(formData.pro);
                await this.reviewForm.submitFirstStep();

                await this.reviewForm.waitForContraError()
                    .should.eventually.equal(true, 'Показывает сообщение с ошибкой');

                const urlAfterSubmit = await this.browser.getUrl();

                await this.allure.runStep(
                    'Не происходит перехода на другую страницу',
                    () => this.expect(urlAfterSubmit, 'Проверяем что URL на шаге 2 не изменился')
                        .to.be.link(urlBeforeSubmit)
                );
            },
        }),
    },
});
