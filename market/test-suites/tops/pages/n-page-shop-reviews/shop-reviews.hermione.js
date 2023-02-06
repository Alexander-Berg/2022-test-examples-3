import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {DEFAULT_USER_UID, createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import userReviewsConfig from '@self/platform/spec/hermione/configs/reviews/shop/userReviews';
import {shopFeedbackFormId} from '@self/platform/spec/hermione/configs/forms';

// suites
import ReviewsEmptySuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-reviews/empty';
import ProductReviewItemDeleteAcceptedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-product-review-item/deleteAccepted';
import ProductReviewItemDeleteDisclaimedSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-product-review-item/deleteDisclaimed';
import ReviewsToolbarNonexistentSuite from '@self/platform/spec/hermione/test-suites/blocks/n-reviews-toolbar/nonexistent';
import ReviewsToolbarReviewAddButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/n-reviews-toolbar/reviewAddButton';
import UserProfilePopupSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/UserProfilePopupSnippet';
import UserMiniprofileAnonymousUserSuite from '@self/platform/spec/hermione/test-suites/blocks/n-user-miniprofile/anonymousUser';
import UserMiniprofileDeletedUserSuite from '@self/platform/spec/hermione/test-suites/blocks/n-user-miniprofile/deletedUser';
import PopupComplainSuite from '@self/platform/spec/hermione/test-suites/blocks/PopupComplain/shopAndOffer';
import ReviewsRecommendedSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-reviews/recommended';
import ReviewsOthersSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-reviews/others';
import ShopRatingWarningByDefaultSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/ShopRatingWarning/byDefault';
import ShopRatingWarningBadShopSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/ShopRatingWarning/badShop';
import ShopReviewsDegradationSuite from '@self/project/src/spec/hermione/test-suites/blocks/ShopReviewsSuite/degradation.js';

// page-objects
import {createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
import ShopReview from '@self/platform/components/ShopReview/Review/__pageObject';
import ShopReviews from '@self/platform/spec/page-objects/widgets/content/ShopReviews';
import ShopReviewsList from '@self/platform/spec/page-objects/widgets/content/ShopReviewsList';
import UserProfilePopupSnippet from '@self/platform/spec/page-objects/UserProfilePopupSnippet';
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';
import ShopReviewsToolbar from '@self/platform/spec/page-objects/widgets/content/ShopReviewsToolbar';
import ShopInfoComplain from '@self/platform/widgets/content/ShopReviews/components/ShopInfo/__pageObject/Complain';
import ComplainPopup from '@self/platform/spec/page-objects/components/ComplainPopup';
import ShopRatingWarning from '@self/project/src/components/ShopRatingWarning/__pageObject/index.desktop';

import ReviewAuthor from '@self/platform/components/Review/Author/__pageObject';

import seo from './seo';

const shopConfig = routes.reviews.shop;

const currentUserReviewMock = {
    id: 1,
    type: 0,
    shop: {id: shopConfig.randomShop.shopId},
    cpa: true,
    region: {
        entity: 'region',
        id: 213,
        name: 'Москва',
    },
    user: {uid: profiles['pan-topinambur'].uid},
    resolved: 0,
};

const otherUserReviewMock = {
    id: 2,
    type: 0,
    shop: {id: shopConfig.randomShop.shopId},
    cpa: true,
    region: {
        entity: 'region',
        id: 213,
        name: 'Москва',
    },
    user: {uid: DEFAULT_USER_UID},
    resolved: 0,
};

const currentUser = createUser({
    id: profiles['pan-topinambur'].uid,
    uid: {
        value: profiles['pan-topinambur'].uid,
    },
    public_id: profiles['pan-topinambur'].publicId,
});

const userPublicId = 'zz2x3xccv5q6wer';
const otherUser = createUser();

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница отзывов на магазин.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(ReviewsEmptySuite, {
            pageObjects: {
                reviews() {
                    return this.createPageObject(ShopReviewsList);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage('market:shop-reviews', {
                        shopId: 305882,
                        slug: 'santehnic-market',
                    });
                },
            },
        }),

        {
            'Список отзывов.': {
                'Авторизованный пользователь.': mergeSuites(
                    {
                        async beforeEach() {
                            const url = await this.browser.yaBuildURL('market:index', {});
                            await this.browser.yaLogin(
                                profiles['pan-topinambur'].login,
                                profiles['pan-topinambur'].password,
                                url
                            );
                        },
                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },
                    {
                        'Отзыв текущего пользователя.': mergeSuites(
                            makeSuite('Удаление отзыва с подтверждением удаления.', {
                                id: 'marketfront-2313',
                                issue: 'MARKETVERSTKA-23663',
                                environment: 'kadavr',
                                story: prepareSuite(ProductReviewItemDeleteAcceptedSuite, {
                                    hooks: {
                                        async beforeEach() {
                                            await this.browser.setState('schema', {
                                                users: [
                                                    currentUser,
                                                    otherUser,
                                                ],
                                                modelOpinions: [
                                                    currentUserReviewMock,
                                                    otherUserReviewMock,
                                                ],
                                                gradesOpinions: [
                                                    currentUserReviewMock,
                                                ],
                                            });

                                            await this.browser.yaOpenPage(
                                                'market:shop-reviews',
                                                {
                                                    ...shopConfig.randomShop,
                                                }
                                            );

                                            await this.setPageObjects({
                                                productReviewItem: () => this.createPageObject(
                                                    ShopReview,
                                                    {
                                                        root: `[data-review-id="${currentUserReviewMock.id}"]`,
                                                    }
                                                ),
                                            });
                                        },
                                    },
                                }),
                            }),
                            makeSuite('Удаление отзыва с отказом от удаления.', {
                                id: 'marketfront-574',
                                issue: 'MARKETVERSTKA-23663',
                                environment: 'kadavr',
                                story: prepareSuite(ProductReviewItemDeleteDisclaimedSuite, {
                                    hooks: {
                                        async beforeEach() {
                                            await this.browser.setState('schema', {
                                                users: [
                                                    currentUser,
                                                    otherUser,
                                                ],
                                                modelOpinions: [
                                                    currentUserReviewMock,
                                                    otherUserReviewMock,
                                                ],
                                                gradesOpinions: [
                                                    currentUserReviewMock,
                                                ],
                                            });

                                            await this.browser.yaOpenPage(
                                                'market:shop-reviews',
                                                {
                                                    ...shopConfig.randomShop,
                                                }
                                            );

                                            await this.setPageObjects({
                                                productReviewItem: () => this.createPageObject(
                                                    ShopReview,
                                                    {
                                                        root: `[data-review-id="${currentUserReviewMock.id}"]`,
                                                    }
                                                ),
                                            });
                                        },
                                    },
                                }),
                            })
                        ),
                    }
                ),

            },

            'Панель сортировок и фильтров.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            reviewsToolbar: () =>
                                this.createPageObject(ShopReviewsToolbar),
                        });
                    },
                },

                prepareSuite(ReviewsToolbarNonexistentSuite, {
                    params: {
                        path: 'market:shop-reviews',
                        shopId: shopConfig.ordinary.shopId,
                        slug: shopConfig.ordinary.slug,
                        query: {
                            shopId: shopConfig.ordinary.shopId,
                            slug: shopConfig.ordinary.slug,
                        },
                    },
                })
            ),

            'Создание отзыва': mergeSuites(
                prepareSuite(ReviewsToolbarReviewAddButtonSuite, {
                    pageObjects: {
                        shopReviews() {
                            return this.createPageObject(ShopReviews);
                        },
                    },

                    params: {
                        shopId: shopConfig.ordinary.shopId,
                        slug: shopConfig.ordinary.slug,
                        path: 'market:shop-reviews',
                    },
                })
            ),
        },

        makeSuite('Тултипы профилей пользователей.', {
            environment: 'kadavr',
            story: mergeSuites({
                'Тултипы профилей пользователей.': prepareSuite(UserProfilePopupSnippetSuite, {
                    hooks: {
                        async beforeEach() {
                            await preparePageForTooltipSuites(
                                this,
                                {userType: 'plainUser'}
                            );

                            const uid = await this.userMiniprofile.getUID();

                            await this.setPageObjects({
                                userProfilePopupSnippet: () => this.createPageObject(
                                    UserProfilePopupSnippet,
                                    {
                                        root: `[data-popup-user="${uid}"]`,
                                    }
                                ),
                            });
                        },
                    },
                    pageObjects: {
                        userAchievementsModal() {
                            return this.createPageObject(UserAchievementsModal);
                        },
                    },
                    params: {
                        willTooltipOpen: true,
                        hoverOnRootNode() {
                            return this.shopReview.hoverOnReviewProfile();
                        },
                        userName: 'fake U.',
                        // флаг нужен чтобы в тесте проверялся блок NoReviews
                        noReviews: true,
                        // для реализации теста с количеством отзывов нужен мок buker'a
                        reviewLink: 'Не написал отзывов',
                        isExpertiseBlockVisible: true,
                        publicId: userPublicId,
                    },
                }),
                'Тултипы для отзывов, владельцы которых скрыли свои профили.': prepareSuite(
                    UserMiniprofileAnonymousUserSuite,
                    {
                        hooks: {
                            beforeEach() {
                                return preparePageForTooltipSuites(
                                    this,
                                    {userType: 'anonymousUser'}
                                );
                            },
                        },
                    }),
                'Тултипы для отзывов, владельцы которых удалили свой профиль.': prepareSuite(
                    UserMiniprofileDeletedUserSuite,
                    {
                        hooks: {
                            beforeEach() {
                                return preparePageForTooltipSuites(
                                    this,
                                    {userType: 'deletedUser'}
                                );
                            },
                        },
                    }),
            }),
        }),

        prepareSuite(PopupComplainSuite, {
            pageObjects: {
                popup() {
                    return this.createPageObject(ShopInfoComplain);
                },
                popupForm() {
                    return this.createPageObject(ComplainPopup);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('Forms.data.collections.forms', {
                        [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                    });

                    return this.browser.yaOpenPage('market:shop-reviews', {shopId: 774, slug: 'test-yandex'});
                },
            },
            params: {
                skipFirstScreen: true,
            },
            only: ['Жалоба на магазин'],
        }),
        prepareSuite(ReviewsRecommendedSuite),
        prepareSuite(ReviewsOthersSuite),
        prepareSuite(ShopRatingWarningByDefaultSuite, {
            meta: {
                id: 'marketfront-4210',
                issue: 'MARKETFRONT-13131',
            },
            hooks: {
                async beforeEach() {
                    await prepareStateForShopRatingWarning(this, {});
                },
            },
            pageObjects: {
                shopRatingWarning() {
                    return this.createPageObject(ShopRatingWarning);
                },
            },
        }),
        prepareSuite(ShopRatingWarningBadShopSuite, {
            meta: {
                id: 'marketfront-4208',
                issue: 'MARKETFRONT-13131',
            },
            hooks: {
                async beforeEach() {
                    await prepareStateForShopRatingWarning(this, {
                        entity: 'shop',
                        ratingType: 0,
                        cutoff: '06.07.2022',
                    });
                },
            },
            pageObjects: {
                shopRatingWarning() {
                    return this.createPageObject(ShopRatingWarning);
                },
            },
        }),
        seo,
        prepareSuite(ShopReviewsDegradationSuite, {
            environment: 'testing',
            pageObjects: {
                shopReviews() {
                    return this.createPageObject(ShopReviews);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', createShopInfo({
                        entity: 'shopInfo',
                        status: 'actual',
                        oldStatus: 'actual',
                        oldCutoff: '',
                        name: 'shop',
                        slug: 'shop',
                    }, shopConfig.withUserCpa.shopId));

                    await this.browser.yaOpenPage(
                        'market:shop-reviews',
                        {
                            ...shopConfig.withUserCpa,
                        }
                    );
                },
            },
        })
    ),
});


async function preparePageForTooltipSuites(ctx, {userType}) {
    const {shopId, page, reviewId, slug} = userReviewsConfig[userType];

    const anonymous = userType === 'anonymousUser' ? 1 : 0;
    const __deleted = userType === 'deletedUser';

    const user = createUser({
        id: '1',
        uid: {
            value: '1',
        },
        display_name: {
            name: 'fake User',
            public_name: 'fake U.',
            display_name_empty: false,
            avatar: {
                default: '36777/515095637-1546054309',
                empty: false,
            },
        },
        public_id: userPublicId,
        __deleted,
    });

    await ctx.browser.setState('schema', {
        users: [
            user,
        ],
        userAchievements: {
            [user.uid.value]: [{
                achievementId: 0,
                confirmedEventsCount: 1,
                pendingEventsCount: 0,
            }, {
                achievementId: 1,
                confirmedEventsCount: 2,
                pendingEventsCount: 1,
            }],
        },
        modelOpinions: [
            {
                id: Number(reviewId),
                type: 0,
                shop: {
                    id: Number(shopId)},
                region: {
                    id: 213,
                },
                user: {
                    uid: user.uid.value,
                },
                cpa: true,
                recommend: true,
                anonymous,
            },
        ],
        modelAnswers: [],
    });

    await ctx.browser.setState('storage', {
        userExpertise: [
            {
                userId: Number(user.id),
                expertiseId: 0,
                value: 4,
                levelValue: 4,
                level: 1,
            },
        ],
    });

    await ctx.browser.yaOpenPage('market:shop-reviews', {shopId, page, slug});

    await ctx.setPageObjects({
        shopReview: () => ctx.createPageObject(
            ShopReview,
            {
                root: `[data-review-id="${reviewId}"]`,
            }
        ),
    });

    await ctx.setPageObjects({
        userMiniprofile: () => ctx.createPageObject(ReviewAuthor, {
            parent: ctx.shopReview,
        }),
    });
}


async function prepareStateForShopRatingWarning(ctx, shopInfo = {}) {
    await ctx.browser.setState('schema', {
        users: [
            currentUser,
            otherUser,
        ],
        modelOpinions: [
            otherUserReviewMock,
        ],
        gradesOpinions: [
            currentUserReviewMock,
        ],
    });

    await ctx.browser.setState('report', createShopInfo(shopInfo, shopConfig.randomShop.shopId));

    await ctx.browser.yaOpenPage(
        'market:shop-reviews',
        {
            ...shopConfig.randomShop,
        }
    );
}
