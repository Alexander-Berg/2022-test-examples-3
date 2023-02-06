import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';

import {createGainExpertise} from '@yandex-market/kadavr/mocks/PersAuthor/helpers';

// page objects
import RatingInput from '@self/root/src/components/RatingInput/__pageObject';
import ShopReviewNew from '@self/platform/spec/page-objects/widgets/parts/ShopReviewNew';

// test suites
import ReviewFormShopFieldsSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/ShopFields';
import ReviewFormShopShopFieldsOrderIdSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/ShopFields/orderId';
import ReviewFormShopFeedbackOrderIdPositive
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/feedbackOrderIdPositive';
import ReviewFormShopFeedbackOrderIdNegative
    from '@self/platform/spec/hermione/test-suites/blocks/ReviewForm/feedbackOrderIdNegative';
import GainedExpertiseSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/GainedExpertise';

import {shopInfo, buildReview} from './kadavrMocks';

const userUid = profiles.ugctest3.uid;
const users = [createUser({
    ...profiles.ugctest3,
    uid: {
        value: userUid,
    },
    id: userUid,
})];

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

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Новый отзыв" к магазину.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                const review = buildReview({shopId: routes.shop.shopId, uid: userUid});
                const schema = {
                    users,
                    modelOpinions: [review],
                    gradesOpinions: [review],
                };
                await this.browser.setState('schema', schema);
                await this.browser.setState('report', shopInfo);

                this.setPageObjects({
                    shopReviewNew: () => this.createPageObject(ShopReviewNew),
                });
            },
        },
        prepareSuite(ReviewFormShopFieldsSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaProfile(
                        'ugctest3',
                        'market:shop-reviews-add',
                        {shopId: routes.shop.shopId}
                    );
                },
            },
        }),
        prepareSuite(ReviewFormShopShopFieldsOrderIdSuite, {
            params: {
                expectedOrderId: 'order-12345',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaProfile(
                        'ugctest3',
                        'market:shop-reviews-add',
                        {
                            shopId: routes.shop.shopId,
                            orderId: 'order-12345',
                        }
                    );
                },
            },
        }),
        prepareSuite(ReviewFormShopFeedbackOrderIdPositive, {
            params: {
                expectedNegativeDeliveryQuestions: positiveDeliveryQuestions.map(q => q.title),
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('OrderFeedback.feedback', [
                        persFeedback,
                    ]);

                    await this.browser.yaProfile(
                        'ugctest3',
                        'market:shop-reviews-add',
                        {
                            shopId: routes.shop.shopId,
                            feedbackOrderId: FEEDBACK_ORDER_ID,
                        }
                    );
                },
            },
        }),
        prepareSuite(ReviewFormShopFeedbackOrderIdNegative, {
            params: {
                expectedNegativeDeliveryQuestions: negativeDeliveryQuestions.map(q => q.title),
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('OrderFeedback.feedback', [
                        persFeedback,
                    ]);

                    await this.browser.yaProfile(
                        'ugctest3',
                        'market:shop-reviews-add',
                        {
                            shopId: routes.shop.shopId,
                            feedbackOrderId: FEEDBACK_ORDER_ID,
                        }
                    );
                },
            },
        }),
        {
            'Авторизованный пользователь без отзыва на магазин': mergeSuites({
                'при заполнении только оценки': prepareSuite(GainedExpertiseSuite, {
                    meta: {
                        id: 'm-touch-3397',
                        issue: 'MARKETFRONT-16106',
                    },
                    params: {
                        expectedBadgeText: '',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.setPageObjects({
                                ratingStars: () => this.createPageObject(RatingInput, {parent: ShopReviewNew.rating}),
                            });

                            const {browser} = this;

                            const agitationType = 3; // тип агитации Оценка очтена
                            const gained = 1; // количество баллов

                            const gainExpertise = createGainExpertise(agitationType, gained, userUid);
                            await browser.setState('storage', {gainExpertise});

                            await browser.yaProfile(
                                'ugctest3',
                                'market:shop-reviews-add',
                                {
                                    shopId: routes.shop.shopId,
                                }
                            );

                            await this.ratingStars.waitForVisible();
                            await this.ratingStars.setRating(4);

                            await this.shopReviewNew.submitFirstStep();
                            await this.shopReviewNew.submitForm();
                        },
                    },
                }),
            }),
            'при заполнении положительного отзыва': prepareSuite(GainedExpertiseSuite, {
                meta: {
                    id: 'm-touch-3392',
                    issue: 'MARKETFRONT-16116',
                },
                params: {
                    expectedBadgeText: 'Вы достигли 2 уровня',
                },
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            ratingStars: () => this.createPageObject(RatingInput, {parent: ShopReviewNew.rating}),
                        });

                        const {browser} = this;

                        const agitationType = 4; // тип агитации Спасибо за отзыв + текст про модерацию
                        const gained = 30; // количество баллов

                        const gainExpertise = createGainExpertise(agitationType, gained, userUid);
                        await browser.setState('storage', {gainExpertise});

                        await browser.yaProfile(
                            'ugctest3',
                            'market:shop-reviews-add',
                            {
                                shopId: routes.shop.shopId,
                            }
                        );

                        await this.ratingStars.waitForVisible();
                        await this.ratingStars.setRating(4);
                        await this.shopReviewNew.clearProTextField();
                        await this.shopReviewNew.setProTextField('1'.repeat(50));

                        await this.shopReviewNew.submitFirstStep();
                        await this.shopReviewNew.submitForm();
                    },
                },
            }),
            'при заполнении отрицательного отзыва': prepareSuite(GainedExpertiseSuite, {
                meta: {
                    id: 'm-touch-3391',
                    issue: 'MARKETFRONT-16115',
                },
                params: {
                    expectedBadgeText: 'Вы достигли 2 уровня',
                },
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            ratingStars: () => this.createPageObject(RatingInput, {parent: ShopReviewNew.rating}),
                        });

                        const {browser} = this;

                        const agitationType = 4; // тип агитации Спасибо за отзыв + текст про модерацию
                        const gained = 30; // количество баллов

                        const gainExpertise = createGainExpertise(agitationType, gained, userUid);
                        await browser.setState('storage', {gainExpertise});

                        await browser.yaProfile(
                            'ugctest3',
                            'market:shop-reviews-add',
                            {
                                shopId: routes.shop.shopId,
                            }
                        );

                        await this.ratingStars.waitForVisible();
                        await this.ratingStars.setRating(2);
                        await this.shopReviewNew.clearContraTextField();
                        await this.shopReviewNew.setContraTextField('0'.repeat(50));

                        await this.shopReviewNew.submitFirstStep();
                        await this.shopReviewNew.submitForm();
                    },
                },
            }),
        }
    ),
});
