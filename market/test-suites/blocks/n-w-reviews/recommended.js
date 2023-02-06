import {makeSuite, makeCase} from 'ginny';

import ShopReviewsList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import ShopRatingSummary from '@self/platform/widgets/content/ShopReviews/components/ShopRatingSummary/__pageObject';

import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {DEFAULT_USER_UID, createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

const SHOP_ID = 774;
const SHOP_SLUG = 'santehnic-market';

// количество оценок с текстом
const TEXT_REVIEWS_COUNT = 5;
// количество оценок без текста
const WITHOUT_TEXT_REVIEWS_COUNT = 7;
// всего отзывов (с текстом + без текста)
const ALL_TIME_REVIEWS_COUNT = TEXT_REVIEWS_COUNT + WITHOUT_TEXT_REVIEWS_COUNT;

function createShopOpinion(opinion = {}, id) {
    return {
        id: String(id),
        type: 0,
        shop: {
            id: SHOP_ID,
        },
        cpa: true,
        region: {
            entity: 'region',
            id: 213,
            name: 'Москва',
        },
        user: {
            uid: DEFAULT_USER_UID,
        },
        ...opinion,
    };
}

/**
 * Тест на блок n-w-reviews.
 * @param {PageObject.ShopReviewsList} reviews
 */
export default makeSuite('Блок отзывов.', {
    environment: 'kadavr',
    feature: 'Структура страницы',
    story: {
        async beforeEach() {
            this.setPageObjects({
                reviews: () => this.createPageObject(ShopReviewsList),
                ratingSummary: () => this.createPageObject(ShopRatingSummary),
            });
        },

        'Если есть оценки с текстом и нет оценок без текста': {
            async beforeEach() {
                await this.browser.setState('report', createShopInfo({
                    entity: 'shopInfo',
                    newGradesCount3M: TEXT_REVIEWS_COUNT,
                    newGradesCount: TEXT_REVIEWS_COUNT,
                    overallGradesCount: TEXT_REVIEWS_COUNT,
                    status: 'actual',
                    oldStatus: 'actual',
                    oldCutoff: '',
                    name: 'shop',
                    slug: 'shop',
                }, SHOP_ID));

                const recommendedOpinion = {
                    shop: {id: SHOP_ID},
                    user: {uid: DEFAULT_USER_UID},
                    cpa: false,
                    averageGrade: 4,
                    resolved: 0,
                    created: Date.now(),
                };

                const modelOpinions = [];

                for (let id = 1; id <= TEXT_REVIEWS_COUNT; id++) {
                    modelOpinions.push(
                        createShopOpinion(recommendedOpinion, id)
                    );
                }

                await this.browser.setState('schema', {
                    users: [createUser()],
                    modelOpinions,
                });

                await this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID, slug: SHOP_SLUG});
            },

            'отображается список отзывов с текстом': makeCase({
                id: 'marketfront-2601',
                issue: 'MARKETVERSTKA-29509',
                async test() {
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount();

                    await this.expect(
                        reviewsWithTextCount,
                        'Отображаются отзывы с текстом'
                    )
                        .to.be.equal(TEXT_REVIEWS_COUNT);
                },
            }),

            'не отображается блок с количеством отзывов без текста': makeCase({
                id: 'marketfront-4196',
                issue: 'MARKETFRONT-9486',
                test() {
                    return this.reviews.isGradesBlockVisible()
                        .should.eventually.to.be.equal(
                            false,
                            'блок с количеством отзывов без текста не отображается'
                        );
                },
            }),

            'количество отзывов за все время равно количеству отзывов с текстом': {
                id: 'marketfront-2601',
                issue: 'MARKETVERSTKA-29509',
                async test() {
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount();

                    const allTimeReviewCount = parseInt(
                        await this.ratingSummary.getAllTimeReviewCount(),
                        10
                    );

                    await this.expect(reviewsWithTextCount).to.be.equal(
                        allTimeReviewCount,
                        'Количество отзывов за все время равно количеству отзывов с текстом на странице'
                    );
                },
            },
        },

        'Если есть оценки с текстом и без текста': {
            async beforeEach() {
                await this.browser.setState('report', createShopInfo({
                    entity: 'shopInfo',
                    newGradesCount3M: ALL_TIME_REVIEWS_COUNT,
                    newGradesCount: ALL_TIME_REVIEWS_COUNT,
                    // в PersQa будет <TEXT_REVIEWS_COUNT> = 5 отзыва с текстом,
                    // а в репорте <ALL_TIME_REVIEWS_COUNT> = 12
                    // 12 - 5 = 7 - значит 7 отзывов без текста
                    overallGradesCount: ALL_TIME_REVIEWS_COUNT,
                    status: 'actual',
                    oldStatus: 'actual',
                    oldCutoff: '',
                    name: 'shop',
                    slug: 'shop',
                }, SHOP_ID));

                const recommendedOpinion = {
                    shop: {id: SHOP_ID},
                    user: {uid: DEFAULT_USER_UID},
                    cpa: false,
                    averageGrade: 4,
                    resolved: 0,
                    created: Date.now(),
                };

                const modelOpinions = [];

                for (let id = 1; id <= TEXT_REVIEWS_COUNT; id++) {
                    modelOpinions.push(
                        createShopOpinion(recommendedOpinion, id)
                    );
                }

                await this.browser.setState('schema', {
                    users: [createUser()],
                    modelOpinions,
                });

                await this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID, slug: SHOP_SLUG});
            },

            'отображается список отзывов': {
                id: 'marketfront-2601',
                issue: 'MARKETVERSTKA-29509',
                async test() {
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount();

                    await this.expect(
                        reviewsWithTextCount,
                        'Отображаются отзывы с текстом'
                    )
                        .to.be.equal(TEXT_REVIEWS_COUNT);
                },
            },

            'отображается блок отзывов без текста': {
                id: 'marketfront-4197',
                issue: 'MARKETFRONT-9486',
                test() {
                    return this.reviews.isGradesBlockVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'блок с количеством отзывов без текста отображается'
                        );
                },
            },

            'в блоке отзывов без текста выводится правильное количество отзывов без текста': {
                id: 'marketfront-4198',
                issue: 'MARKETFRONT-9486',
                test() {
                    return this.reviews.getGradesCount()
                        .should.eventually.to.be.equal(
                            String(WITHOUT_TEXT_REVIEWS_COUNT),
                            'Количество отзывов без текста выводится правильно'
                        );
                },
            },

            'количество отзывов за все время равно сумме количества отзывов с текстом и без текста ': {
                id: 'marketfront-2601',
                issue: 'MARKETVERSTKA-29509',
                async test() {
                    // Количество отзывов с текстом
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount()
                        .then(count => parseInt(count, 10));

                    // Количество оценок без текста
                    const gradesCount = await this.reviews.getGradesCount()
                        .then(count => parseInt(count, 10));

                    return this.expect(reviewsWithTextCount + gradesCount)
                        .to.be.equal(
                            ALL_TIME_REVIEWS_COUNT,
                            'Количество отзывов за все время выводится правильно'
                        );
                },
            },
        },

        'Если нет оценок с текстом и есть оценки без текста': {
            async beforeEach() {
                await this.browser.setState('report', createShopInfo({
                    entity: 'shopInfo',
                    newGradesCount3M: WITHOUT_TEXT_REVIEWS_COUNT,
                    newGradesCount: WITHOUT_TEXT_REVIEWS_COUNT,
                    // в PersQa будет <TEXT_REVIEWS_COUNT> = 5 отзыва с текстом,
                    // а в репорте <ALL_TIME_REVIEWS_COUNT> = 12
                    // 12 - 5 = 7 - значит 7 отзывов без текста
                    overallGradesCount: WITHOUT_TEXT_REVIEWS_COUNT,
                    status: 'actual',
                    oldStatus: 'actual',
                    oldCutoff: '',
                    name: 'shop',
                    slug: 'shop',
                }, SHOP_ID));

                await this.browser.setState('schema', {
                    users: [createUser()],
                    modelOpinions: [],
                });

                await this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID, slug: SHOP_SLUG});
            },

            'не отображается список отзывов': {
                id: 'marketfront-4199',
                issue: 'MARKETFRONT-9486',
                async test() {
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount();

                    await this.expect(reviewsWithTextCount, 'Не отображаются отзывы с текстом')
                        .to.be.equal(0);
                },
            },

            'отображается блок отзывов без текста': {
                id: 'marketfront-4200',
                issue: 'MARKETFRONT-9486',
                test() {
                    return this.reviews.isGradesBlockVisible()
                        .should.eventually.to.be.equal(
                            true,
                            'блок с количеством отзывов без текста отображается'
                        );
                },
            },

            'в блоке отзывов без текста выводится правильное количество отзывов без текста': {
                id: 'marketfront-4201',
                issue: 'MARKETFRONT-9486',
                test() {
                    return this.reviews.getGradesCount()
                        .should.eventually.to.be.equal(
                            String(WITHOUT_TEXT_REVIEWS_COUNT),
                            'Количество отзывов без текста выводится правильно'
                        );
                },
            },

            'количество отзывов за все время равно количеству отзывов без текстом': {
                id: 'marketfront-2601',
                issue: 'MARKETVERSTKA-29509',
                async test() {
                    // Количество отзывов с текстом
                    const reviewsWithTextCount = await this.reviews.getReviewWithTextCount()
                        .then(count => parseInt(count, 10));

                    // Количество оценок без текста
                    const gradesCount = await this.reviews.getGradesCount()
                        .then(count => parseInt(count, 10));

                    return this.expect(reviewsWithTextCount + gradesCount)
                        .to.be.equal(
                            WITHOUT_TEXT_REVIEWS_COUNT,
                            'Количество отзывов за все время выводится правильно'
                        );
                },
            },
        },
    },
});
