import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';

import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// page objects
import ReviewAgitation from '@self/platform/widgets/content/ReviewAgitation/__pageObject';
import RatingStars from '@self/platform/spec/page-objects/components/RatingStars';
import ShopReviewForm from '@self/platform/components/ShopReviewForm/__pageObject';


// suites
import ShopReviewFormSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm';
import ShopReviewFormAddSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/add';
import ShopReviewFormAddNegativeRatingSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/addNegativeRating';
import ShopReviewFormOnlyRating from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/onlyRating';
import ShopReviewFormAverageGradeParamSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/averageGradeParam';
import ShopReviewFormOrderIdParamSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/orderId';
import ShopReviewFormFeedbackOrderIdParamPositiveSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/feedbackOrderIdPositive';
import ShopReviewFormFeedbackOrderIdParamNegativeSuite
    from '@self/platform/spec/hermione/test-suites/blocks/widgets/pages/ShopReviewForm/feedbackOrderIdNegative';
import GainedExpertiseSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/GainedExpertise';


const FEEDBACK_ORDER_ID = '100500';

const positiveDeliveryQuestions = [
    {
        id: '3',
        title: 'Курьер был приветлив ("Держи краба!")',
    },
    {
        id: '4',
        title: 'У курьера был размен с долларов',
    },
    {
        id: '5',
        title: 'Курьер поговорил со мной за жизнь (душевно)',
    },
];

const negativeDeliveryQuestions = [
    {
        id: '1',
        title: 'Курьер был пьян',
    },
    {
        id: '2',
        title: 'Курьер делал изысканные комплименты соседке по лестничной площадке (а мне нет!)',
    },
];

const persFeedback = {
    'orderId': FEEDBACK_ORDER_ID,
    'grade': 0,
    'isCallbackRequired': false,
    'isReviewDenied': false,
    'isReviewSubmitted': false,
    'comment': '',
    'answers': [],
};

const user = profiles['pan-topinambur'];

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница оставления отзыва на магазин.', {
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                reviewForm: () => this.createPageObject(ShopReviewForm),
                reviewAgitation: () => this.createPageObject(ReviewAgitation),
            });
        },

        async afterEach() {
            await this.browser.yaRemovePreventQuit();
        },

        'Неавторизованный пользователь.': prepareSuite(ShopReviewFormSuite, {
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage('market:shop-reviews-add', routes.reviews.shop.add);
                },
            },
        }),

        'Авторизованный пользователь.': mergeSuites(
            {
                async beforeEach() {
                    const users = [createUser({
                        ...profiles['pan-topinambur'],
                        uid: {
                            value: profiles['pan-topinambur'].uid,
                        },
                        id: profiles['pan-topinambur'].uid,
                    })];
                    await this.browser.setState('schema', {
                        users,
                    });
                    return this.browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                        shopId: routes.reviews.shop.add.shopId,
                    });
                },
                afterEach() {
                    return this.browser.yaLogout();
                },
            },

            prepareSuite(ShopReviewFormSuite),
            prepareSuite(ShopReviewFormAddSuite),
            prepareSuite(ShopReviewFormAddNegativeRatingSuite),
            prepareSuite(ShopReviewFormOnlyRating)
        ),

        'Неавторизованный пользователь с параметром averageGrade': prepareSuite(
            ShopReviewFormAverageGradeParamSuite,
            {
                params: {
                    expectedAverageGrade: 4,
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaOpenPage('market:shop-reviews-add', {
                            ...routes.reviews.shop.add,
                            averageGrade: 4,
                        });
                    },
                },

            }),

        'Авторизованный пользователь с параметром orderId': prepareSuite(ShopReviewFormOrderIdParamSuite,
            {
                params: {
                    expectedOrderId: 'order-12345',
                },
                hooks: {
                    beforeEach() {
                        return this.browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                            shopId: routes.reviews.shop.add.shopId,
                            orderId: 'order-12345',
                        });
                    },
                },

            }),

        'Авторизованный пользователь с параметром feedbackOrderId': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.setState('OrderFeedback.feedback', [
                        persFeedback,
                    ]);

                    return this.browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                        shopId: routes.reviews.shop.add.shopId,
                        feedbackOrderId: FEEDBACK_ORDER_ID,
                    });
                },
            },
            prepareSuite(
                ShopReviewFormFeedbackOrderIdParamPositiveSuite,
                {
                    params: {
                        expectedPositiveDeliveryQuestions: positiveDeliveryQuestions.map(q => q.title),
                    },
                }
            ),
            prepareSuite(
                ShopReviewFormFeedbackOrderIdParamNegativeSuite,
                {
                    params: {
                        expectedNegativeDeliveryQuestions: negativeDeliveryQuestions.map(q => q.title),
                    },
                }
            )
        ),
        'Авторизованный пользователь без отзыва на магазин': mergeSuites(
            {
                'при заполнении только оценки': prepareSuite(GainedExpertiseSuite, {
                    meta: {
                        id: 'marketfront-4115',
                        issue: 'MARKETFRONT-16111',
                    },
                    params: {
                        expectedBadgeText: '',
                    },
                    hooks: {
                        async beforeEach() {
                            this.setPageObjects({
                                ratingStars: () => this.createPageObject(
                                    RatingStars,
                                    {parent: ShopReviewForm.averageGrade}
                                ),
                            });

                            const {browser, ratingStars} = this;

                            const agitationType = 3; // тип агитации Оценка очтена
                            const gained = 1; // количество баллов

                            const gainExpertise = createGainExpertise(agitationType, gained, user.uid);
                            await browser.setState('storage', {gainExpertise});

                            await browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                                shopId: routes.reviews.shop.add.shopId,
                            });

                            await ratingStars.waitForVisible();
                            await browser.yaScenario(this, 'shopReviewsAdd.fillForm.onlyGrade');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitFirstStep(),
                                valueGetter: () => this.reviewForm.isSecondStepVisible(),
                            });

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitSecondStep(),
                                valueGetter: () => this.reviewAgitation.isVisible(),
                            });
                        },
                    },
                }),
                'при заполнении положительного отзыва': prepareSuite(GainedExpertiseSuite, {
                    meta: {
                        id: 'marketfront-4109',
                        issue: 'MARKETFRONT-16112',
                    },
                    params: {
                        expectedBadgeText: 'Вы достигли 2 уровня',
                    },
                    hooks: {
                        async beforeEach() {
                            this.setPageObjects({
                                ratingStars: () => this.createPageObject(
                                    RatingStars,
                                    {parent: ShopReviewForm.averageGrade}
                                ),
                                reviewItemRating: () => this.createPageObject(RatingStars, {parent: this.reviewItem}),
                            });

                            const {browser, ratingStars} = this;

                            const agitationType = 4; // тип агитации Спасибо за отзыв + текст про модерацию
                            const gained = 20; // количество баллов

                            const gainExpertise = createGainExpertise(agitationType, gained, user.uid);
                            await browser.setState('storage', {gainExpertise});

                            await browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                                shopId: routes.reviews.shop.add.shopId,
                            });

                            await ratingStars.waitForVisible();
                            await browser.yaScenario(this, 'shopReviewsAdd.fillForm.positive');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitFirstStep(),
                                valueGetter: () => this.reviewForm.isSecondStepVisible(),
                            });

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitSecondStep(),
                                valueGetter: () => this.reviewAgitation.isVisible(),
                            });
                        },
                    },
                }),
                'при заполнении отрицательного отзыва': prepareSuite(GainedExpertiseSuite, {
                    meta: {
                        id: 'marketfront-4110',
                        issue: 'MARKETFRONT-16110',
                    },
                    params: {
                        expectedBadgeText: 'Вы достигли 2 уровня',
                    },
                    hooks: {
                        async beforeEach() {
                            this.setPageObjects({
                                ratingStars: () => this.createPageObject(
                                    RatingStars,
                                    {parent: ShopReviewForm.averageGrade}
                                ),
                                reviewItemRating: () => this.createPageObject(RatingStars, {parent: this.reviewItem}),
                            });

                            const {browser, ratingStars} = this;

                            const agitationType = 4; // тип агитации Спасибо за отзыв + текст про модерацию
                            const gained = 20; // количество баллов

                            const gainExpertise = createGainExpertise(agitationType, gained, user.uid);
                            await browser.setState('storage', {gainExpertise});

                            await browser.yaProfile('pan-topinambur', 'market:shop-reviews-add', {
                                shopId: routes.reviews.shop.add.shopId,
                            });

                            await ratingStars.waitForVisible();
                            await browser.yaScenario(this, 'shopReviewsAdd.fillForm.negative');

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitFirstStep(),
                                valueGetter: () => this.reviewForm.isSecondStepVisible(),
                            });

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.reviewForm.submitSecondStep(),
                                valueGetter: () => this.reviewAgitation.isVisible(),
                            });
                        },
                    },
                }),
            }
        ),
    },
});
