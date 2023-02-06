import {makeCase, makeSuite, mergeSuites, prepareSuite} from 'ginny';

import ProductReviewForm from '@self/platform/components/ProductReviewForm/__pageObject/ProductReviewForm';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';

import AgitationCardMenu from '../AgitationCardMenu/AgitationCardMenu.js';

/**
 * @param {PageObject.components.ReviewPollCard} reviewPollCard
 * @param {PageObject.components.RatingStars} ratingStars
 * @param {PageObject.components.AgitationCard} agitationCard
 * @param {PageObject.components.AgitationCardMenu} agitationCardMenu
 */
export default makeSuite('Карточка агитации оставления оценки.', {
    params: {
        ratingHeading: 'Ожидаемое значение заголовка агитации',
        reviewUrl: 'Страница оставления отзыва',
        expectedAverageGrade: 'Ожидаемое значение оценки',
    },
    story: mergeSuites({
        'По умолчанию.': {
            'Агитация оставления оценки отображается.': makeCase({
                id: 'marketfront-3998',
                test() {
                    return this.reviewPollCard.isVisible()
                        .should.eventually.be.equal(true, 'Карточка агитации оставления оценки отображается');
                },
            }),
            'Содержит правильный заголовок агитации.': makeCase({
                id: 'marketfront-3999',
                async test() {
                    return this.reviewPollCard.ratingHeading.getText()
                        .should.eventually.be.equal(this.params.ratingHeading, 'В заголовке агитации правильный текст');
                },
            }),
        },
        'Клик по звездам оценки.': {
            async beforeEach() {
                await this.ratingStars.waitForVisible();

                await this.browser.allure.runStep(
                    'Нажимаем на звёзды и ждем когда страница обновится',
                    () => this.browser.yaWaitForPageReloadedExtended(
                        () => this.ratingStars.setRating(this.params.expectedAverageGrade),
                        20000
                    )
                );

                return this.setPageObjects({
                    productReviewForm: () => this.createPageObject(ProductReviewForm),
                    shopReviewForm: () => this.createPageObject(ShopReviewForm),
                    reviewRatingStars: () => this.createPageObject(RatingStars),
                });
            },
            'Открылась страница оставления отзыва': makeCase({
                id: 'marketfront-4001',
                async test() {
                    return this.browser.getUrl()
                        .should.eventually.be.link({
                            pathname: this.params.reviewUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
            'Отображается то количество желтых звезд, которое мы указали.': makeCase({
                id: 'marketfront-4002',
                async test() {
                    await this.reviewRatingStars.getRating()
                        .should.eventually.equal(this.params.expectedAverageGrade, 'значение общей оценки верно');
                },
            }),
        }},
    prepareSuite(AgitationCardMenu)
    ),
});
