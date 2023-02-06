import {makeSuite, makeCase} from 'ginny';

import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';

/**
 * Тесты на виджет ShopReviewForm
 *
 * @param {PageObject.ShopReviewForm} reviewForm
 * @param {PageObject.ReviewAgitation} reviewAgitation
 */

export default makeSuite('Форма добавления нового отзыва на магазин. Добавление оценки на магазин.', {
    feature: 'Страница Спасибо',
    issue: 'MARKETVERSTKA-23667',
    id: 'marketfront-588',
    story: {
        beforeEach() {
            this.setPageObjects({
                ratingStars: () => this.createPageObject(RatingStars, {parent: this.reviewForm.averageGrade}),
                reviewItemRating: () => this.createPageObject(RatingStars, {parent: this.reviewItem}),
            });
        },

        'Заполненная форма выставления оценки': {
            'Должна вести на страницу "Спасибо, оценка учтена"': makeCase({
                async test() {
                    const {reviewForm, reviewAgitation, reviewItemRating, browser} = this;

                    await reviewForm.isVisible()
                        .should.eventually.equal(true, 'Форма оставления оценки видна');

                    await browser.yaScenario(this, 'shopReviewsAdd.fillForm.onlyGrade');

                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitFirstStep(),
                        valueGetter: () => reviewForm.isSecondStepVisible(),
                    });

                    await browser.yaWaitForChangeValue({
                        action: () => reviewForm.submitSecondStep(),
                        valueGetter: () => reviewAgitation.isVisible(),
                    });

                    await browser.allure.runStep(
                        'Проверяем, что оценка была сохранена',
                        () => browser.yaOpenPage('market:my-reviews')
                            .then(() => browser.yaWaitForPageLoadedExtended(5000))
                            .then(() => reviewItemRating.getRating())
                            .should.eventually.equal(4)
                    );
                },
            }),
        },
    },
});
