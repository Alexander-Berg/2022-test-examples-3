import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {createShopInfo, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {reviewStates} from '@self/root/src/entities/review/constants';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {productWithPicture} from '@self/platform/spec/hermione/fixtures/product';

// suites
import RemovableSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserReviews/removableSnippet';
import HeadLineSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/content/UserReviews/HeadLine';
import UserReviewFooterSuite from '@self/platform/spec/hermione/test-suites/blocks/components/UserReview/footer';

// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import UserReviews from '@self/platform/spec/page-objects/widgets/content/UserReviews';
import UserReview from '@self/platform/spec/page-objects/components/UserReview';
import ProductHeadline from '@self/platform/components/UserReview/ProductHeadline/__pageObject';
import ShopHeadline from '@self/platform/components/UserReview/ShopHeadline/__pageObject';

const USER_PROFILE_CONFIG = profiles.ugctest3;
const DEFAULT_USER = createUser({
    id: USER_PROFILE_CONFIG.uid,
    uid: {
        value: USER_PROFILE_CONFIG.uid,
    },
    login: USER_PROFILE_CONFIG.login,
    public_id: USER_PROFILE_CONFIG.publicId,
    display_name: {
        name: 'Willy Wonka',
        public_name: 'Willy W.',
        avatar: {
            default: '61207/462703116-1544492602',
            empty: false,
        },
    },
    dbfields: {
        'userinfo.firstname.uid': 'Willy',
        'userinfo.lastname.uid': 'Wonka',
    },
});

const DEFAULT_VOTES_COUNT = 10;

const PRO = 'Pro';
const CONTRA = 'Contra';
const COMMENT = 'Comment';

const DEFAULT_AVERAGE_GRADE = 5;
const DEFAULT_REVIEW_ID = 12221;
const REVIEW_TYPES = {
    shop: 0,
    product: 1,
};
const DEFAULT_MODERATION_REASON = 'ololo-trololo';

const DEFAULT_PRODUCT = {
    id: 14206682, // id продукта совпадает с id productWithPicture,
    slug: 'smartfon-apple-iphone-7-128gb', // slug продукта совпадает с slug productWithPicture
    entity: 'product',
};
const DEFAULT_SHOP_INFO = {
    entity: 'shop',
    id: 14341,
    shopName: 'Магазин магазин',
    slug: 'magazin-magazin',
};

const reportShopInfo = createShopInfo(DEFAULT_SHOP_INFO, DEFAULT_SHOP_INFO.id);

const createReview = (params = {}) => {
    const {id, productId, shopId, anonymous, gradesOnly, moderationState, spam} = params;

    return ({
        id,
        type: productId ? REVIEW_TYPES.product : REVIEW_TYPES.shop,
        product: productId ? {id: productId} : null,
        shop: shopId ? {id: shopId} : null,
        user: {
            uid: DEFAULT_USER.id,
        },
        anonymous,
        averageGrade: DEFAULT_AVERAGE_GRADE,
        cpa: true,
        pro: gradesOnly ? null : PRO,
        contra: gradesOnly ? null : CONTRA,
        comment: gradesOnly ? null : COMMENT,
        photos: null,
        votes: {
            agree: DEFAULT_VOTES_COUNT,
            reject: DEFAULT_VOTES_COUNT,
        },
        moderationState,
        moderationReasonRecommendation: DEFAULT_MODERATION_REASON,
        spam,
    });
};

const prepareUserReviewsTabPage = async (ctx, params = {}) => {
    const {
        reviewsCount = 1,
        productId = DEFAULT_PRODUCT.id,
        shopId = DEFAULT_SHOP_INFO.id,
        anonymous = 0,
        gradesOnly = false,
        moderationState = reviewStates.APPROVED,
        spam = false,
    } = params;

    let reviews = [];
    if (reviewsCount > 0) {
        reviews = [...Array(reviewsCount)].map((_, index) => createReview({
            id: DEFAULT_REVIEW_ID + index,
            productId,
            shopId,
            anonymous,
            gradesOnly,
            moderationState,
            spam,
        }));
    }

    await ctx.browser.setState('report', mergeState([productWithPicture, reportShopInfo]));
    await ctx.browser.setState('ShopInfo.collections', {
        shopNames: {
            [DEFAULT_SHOP_INFO.id]: {
                id: DEFAULT_SHOP_INFO.id,
                name: DEFAULT_SHOP_INFO.shopName,
                slug: DEFAULT_SHOP_INFO.slug,
            },
        },
    });
    await ctx.browser.setState('schema', {
        users: [DEFAULT_USER],
        gradesOpinions: reviews,
    });

    await ctx.browser.yaProfile('ugctest3', 'market:my-reviews');
    return ctx.browser.yaClosePopup(ctx.createPageObject(RegionPopup));
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница личного кабинета. Вкладка с отзывами пользователя.', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-6447',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    userReviews: () => this.createPageObject(UserReviews),
                    userReview: () => this.createPageObject(UserReview),
                });
            },
            afterEach() {
                return this.browser.yaLogout();
            },
        },
        prepareSuite(RemovableSnippetSuite, {
            hooks: {
                async beforeEach() {
                    await prepareUserReviewsTabPage(this);
                },
            },
        }),
        {
            'Товарный сниппет отзыва.': mergeSuites(
                {
                    async beforeEach() {
                        await this.setPageObjects({
                            headline: () => this.createPageObject(ProductHeadline),
                        });

                        await prepareUserReviewsTabPage(this);

                        this.params = {
                            ...this.params,
                            displayedName: DEFAULT_USER.display_name.public_name,
                            grade: DEFAULT_AVERAGE_GRADE,
                            description: 'Отличный товар',
                            pro: PRO,
                            contra: CONTRA,
                            comment: COMMENT,
                            totalLikesCount: DEFAULT_VOTES_COUNT,
                            totalDislikesCount: DEFAULT_VOTES_COUNT,
                            expectedReviewTypeLink: await this.browser.yaBuildURL('touch:product', {
                                slug: DEFAULT_PRODUCT.slug,
                                productId: DEFAULT_PRODUCT.id,
                            }),
                            expectedModifyReviewLink: await this.browser.yaBuildURL('market:product-reviews-add', {
                                slug: DEFAULT_PRODUCT.slug,
                                productId: DEFAULT_PRODUCT.id,
                            }),
                            expectedReviewPageLink: await this.browser.yaBuildURL('touch:product-review', {
                                slug: DEFAULT_PRODUCT.slug,
                                productId: DEFAULT_PRODUCT.id,
                                reviewId: DEFAULT_REVIEW_ID,
                            }),
                        };
                    },
                },
                prepareSuite(HeadLineSuite),
                prepareSuite(UserReviewFooterSuite)
            ),
        },
        {
            'Магазинный сниппет отзыва.': mergeSuites(
                {
                    async beforeEach() {
                        await this.setPageObjects({
                            headline: () => this.createPageObject(ShopHeadline),
                        });

                        await prepareUserReviewsTabPage(this, {
                            productId: null,
                        });

                        this.params = {
                            ...this.params,
                            displayedName: DEFAULT_USER.display_name.public_name,
                            grade: DEFAULT_AVERAGE_GRADE,
                            description: 'Отличный магазин',
                            pro: PRO,
                            contra: CONTRA,
                            comment: COMMENT,
                            totalLikesCount: DEFAULT_VOTES_COUNT,
                            totalDislikesCount: DEFAULT_VOTES_COUNT,
                            expectedReviewTypeLink: await this.browser.yaBuildURL('touch:shop', {
                                slug: DEFAULT_SHOP_INFO.slug,
                                shopId: DEFAULT_SHOP_INFO.id,
                            }),
                            expectedModifyReviewLink: await this.browser.yaBuildURL('market:shop-reviews-add', {
                                shopId: DEFAULT_SHOP_INFO.id,
                                slug: DEFAULT_SHOP_INFO.slug,
                            }),
                        };
                    },
                },
                prepareSuite(HeadLineSuite)
            ),
        }
    ),
});
