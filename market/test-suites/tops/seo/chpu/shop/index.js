import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';
import {DEFAULT_USER_UID, createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

// suites
import ShopHubChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/n-shop-hub/chpu';
import ReviewsChpuSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-reviews/chpu';
// page-objects
import Popup2 from '@self/platform/spec/page-objects/popup2';
import ShopHub from '@self/platform/spec/page-objects/n-shop-hub';
import ShopRatingStat from '@self/platform/widgets/content/ShopRatingStat/__pageObject__';
import RatingDistribution from '@self/platform/spec/page-objects/rating-distribution';

const SHOP_ID = 1925;
const SHOP_SLUG = 'tekhnopark';

function createShopOpinion(opinion = {}, id) {
    return {
        id,
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

export default makeSuite('Магазин', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Страница Магазина.', {
            story: prepareSuite(ShopHubChpuSuite, {
                pageObjects: {
                    shopHub() {
                        return this.createPageObject(ShopHub);
                    },
                    ratingContribution() {
                        return this.createPageObject(RatingDistribution);
                    },
                    popup2() {
                        return this.createPageObject(Popup2);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', createShopInfo({
                            id: SHOP_ID,
                            slug: SHOP_SLUG,
                            qualityRating: 4.5,
                            newGradesCount3M: 3,
                            status: 'actual',
                            oldStatus: 'actual',
                            oldCutoff: '',
                        }, SHOP_ID));

                        const recommendedOpinion = {
                            shop: {id: SHOP_ID},
                            user: {uid: DEFAULT_USER_UID},
                            cpa: true,
                        };

                        await this.browser
                            .setState('storage', {
                                users: [createUser()],
                                modelOpinions: [
                                    createShopOpinion(recommendedOpinion, 1),
                                    createShopOpinion(recommendedOpinion, 2),
                                    createShopOpinion(recommendedOpinion, 3),
                                ],
                            }).setState('schema', {
                                shopGrades: {
                                    list: [{
                                        parts: [{
                                            percent: 0.07,
                                            number: 78,
                                            value: -2,
                                        },
                                        {
                                            percent: 0.03,
                                            number: 30,
                                            value: -1,
                                        },
                                        {
                                            percent: 0.05,
                                            number: 57,
                                            value: 0,
                                        },
                                        {
                                            percent: 0.08,
                                            number: 95,
                                            value: 1,
                                        },
                                        {
                                            percent: 0.76,
                                            number: 857,
                                            value: 2,
                                        }],
                                        total: 1117,
                                        id: SHOP_ID,
                                    }],
                                },
                            });

                        return this.browser.yaOpenPage('market:shop', {shopId: SHOP_ID, slug: SHOP_SLUG});
                    },
                },
                params: {
                    shopId: SHOP_ID,
                    slug: SHOP_SLUG,
                },
            }),
        }),
        makeSuite('Страница отзывов магазина.', {
            story: prepareSuite(ReviewsChpuSuite, {
                pageObjects: {
                    shopRatingStat() {
                        return this.createPageObject(ShopRatingStat);
                    },
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', createShopInfo({
                            id: SHOP_ID,
                            slug: SHOP_SLUG,
                            qualityRating: 4.5,
                            newGradesCount: 6,
                            newGradesCount3M: 6,
                            status: 'actual',
                            oldStatus: 'actual',
                            oldCutoff: '',
                        }, SHOP_ID));

                        const recommendedOpinion = {
                            shop: {id: SHOP_ID},
                            user: {uid: DEFAULT_USER_UID},
                            cpa: true,
                        };

                        const otherOpinion = {
                            shop: {id: SHOP_ID},
                            user: {uid: DEFAULT_USER_UID},
                            cpa: false,
                        };

                        await this.browser
                            .setState('storage', {
                                users: [createUser({public_id: 'qqwweerr123afsdf'})],
                                modelOpinions: [
                                    createShopOpinion(recommendedOpinion, 1),
                                    createShopOpinion(recommendedOpinion, 2),
                                    createShopOpinion(recommendedOpinion, 3),
                                    createShopOpinion(otherOpinion, 4),
                                    createShopOpinion(otherOpinion, 5),
                                    createShopOpinion(otherOpinion, 6),
                                ],
                            }).setState('schema', {
                                shopGrades: {
                                    list: [{
                                        parts: [{
                                            percent: 0.07,
                                            number: 78,
                                            value: -2,
                                        },
                                        {
                                            percent: 0.03,
                                            number: 30,
                                            value: -1,
                                        },
                                        {
                                            percent: 0.05,
                                            number: 57,
                                            value: 0,
                                        },
                                        {
                                            percent: 0.08,
                                            number: 95,
                                            value: 1,
                                        },
                                        {
                                            percent: 0.76,
                                            number: 857,
                                            value: 2,
                                        }],
                                        total: 1117,
                                        id: SHOP_ID,
                                    }],
                                },
                            });

                        return this.browser.yaOpenPage('market:shop-reviews', {shopId: SHOP_ID, slug: SHOP_SLUG});
                    },
                },
                params: {
                    shopId: SHOP_ID,
                    slug: SHOP_SLUG,
                },
            }),
        })
    ),
});
