import _ from 'lodash';
import {makeSuite, prepareSuite, mergeSuites} from 'ginny';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';
import {createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import ProductReviewsDegradationSuite from '@self/project/src/spec/hermione/test-suites/blocks/ProductReviews/degradation.js';

// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {reviewProduct} from '@self/platform/spec/hermione/configs/reviews/product';
import {
    productReview as currentUserReview,
    randomProductReview,
    reviewWithCommentAndPhotos,
} from '@self/platform/spec/hermione/configs/reviews/mocks';
// suites
import ProductReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview';
import ProductReviewNonUserReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/nonUserReview';
import CommentAuthorSuite from '@self/platform/spec/hermione/test-suites/blocks/n-comment-author';
import UserProfilePopupSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/UserProfilePopupSnippet';
import UserMiniprofileAnonymousUserSuite from '@self/platform/spec/hermione/test-suites/blocks/n-user-miniprofile/anonymousUser';
import UserMiniprofileDeletedUserSuite from '@self/platform/spec/hermione/test-suites/blocks/n-user-miniprofile/deletedUser';
import ProductReviewDeleteAcceptedSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/deleteAccepted';
import ProductReviewDeleteDisclaimedSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/deleteDisclaimed';
import CurrentUserReviewWithoutCommentSuite
    from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/currentUserReview/withoutComment';
import ProductReviewProviderInfoSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/providerInfo';
import ProductReviewWithPhotoSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/withPhoto';
import ProductReviewWithUserCpaSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/withUserCpa';
import ProductReviewWithUserExpertiseSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/withUserExpertise';
import ReviewsPhotoFilterReviewWithPhotoSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewsPhotoFilter/reviewWithPhoto';
import ReviewsPhotoFilterNoneReviewWithPhotoSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ReviewsPhotoFilter/noneReviewWithPhoto';
import ProductHeadlineNewStickerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-headline/newSticker';
import BreadcrumbsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs';
import BreadcrumbsItemClickableYesSuite from '@self/platform/spec/hermione/test-suites/blocks/n-breadcrumbs/__item_clickable_yes';
import HeadBannerProductAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/HeadBanner/productAbsence';
import ProductGradesTitleSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductGrades/title';
import AddReviewButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/addReviewButton';
import ReviewsPageSidebarAddReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewsPageSidebar/addReview';
import AdultWarningDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/default';
import AdultWarningAcceptSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/accept';
import AdultWarningDeclineSuite from '@self/platform/spec/hermione/test-suites/blocks/AdultWarning/decline';
import UgcMediaGallerySuite from '@self/platform/spec/hermione/test-suites/blocks/UgcMediaGallery';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import DsbsFullOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/n-product-top-offer/dsbsFullOffer';

// page-objects
import ProductReviewsPlaceholder from '@self/platform/widgets/content/ProductReviewsPlaceholder/__pageObject';
import ProductReview from '@self/platform/spec/page-objects/components/ProductReview';
import UserAchievementsModal from '@self/platform/spec/page-objects/UserAchievementsModal';
import Comment from '@self/platform/spec/page-objects/n-comment';
import UserMiniprofile from '@self/platform/spec/page-objects/n-user-miniprofile';
import Breadcrumbs from '@self/platform/components/Breadcrumbs/__pageObject';
import NoveltyBadge from '@self/platform/components/NoveltyBadge/__pageObject__';
import ProductHeadline from '@self/platform/widgets/content/ProductCardTitle/__pageObject';
import ReviewsToolbar from '@self/platform/spec/page-objects/components/ReviewsToolbar';
import ProductReviewsPage from '@self/platform/widgets/pages/ProductReviewsPage/__pageObject';
import AuthorExpertise from '@self/root/src/components/AuthorExpertise/__pageObject';
import CommentAuthor from '@self/platform/spec/page-objects/n-comment-author';
import UserProfilePopupSnippet from '@self/platform/spec/page-objects/components/ProductReview/UserProfilePopupSnippet';
import AdultConfirmationPopup from '@self/platform/widgets/content/AdultWarning/components/AdultWarning/__pageObject';
import UgcMediaGallery from '@self/platform/widgets/content/UgcMediaGallery/__pageObject';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import TopOfferSnippet from '@self/platform/spec/page-objects/components/TopOfferSnippet';

// fixtures
import productWithTop6OfferDSBS from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-product/fixtures/productWithTop6OfferDSBS';

import {
    guruMock as newBadgeGuruMock,
    productId as newBadgeProductId,
    slug as newBadgeSlug,
} from './seo/mocks/product.mock';

import seo from './seo';
import defaultOffer from './defaultOffer';

const currentUser = createUser({ // Текущий залогинненный пользователь
    id: profiles['pan-topinambur'].uid,
    uid: {
        value: profiles['pan-topinambur'].uid,
    },
    login: profiles['pan-topinambur'].login,
    public_id: profiles['pan-topinambur'].publicId,
    public_name: 'pan-topinambur',
});
const user = createUser({ // Автор отзыва
    id: '911',
    uid: {
        value: '911',
    },
    display_name: {
        public_name: 'ugctest3',
    },
});
const userExpertise = {
    userId: currentUser.id,
    expertiseId: 9,
    value: 33,
    levelValue: 13,
    level: 2,
};
const commentator = createUser({ // Автор отзыва
    id: '1',
    uid: {
        value: '1',
    },
    display_name: {
        public_name: 'Pupkin Vasily',
    },
});
const preparedProductState = createProduct({
    showUid: 'testProductShowUid',
    type: 'model',
    categories: [{
        id: 123,
    }],
    slug: 'random-fake-slug',
    deletedId: null,
}, randomProductReview.productId);

const modificationProductId = randomProductReview.productId + 321;

const currentUserReviewMock = {
    id: currentUserReview.gradeId,
    type: 1,
    product: {
        id: randomProductReview.productId,
    },
    region: {
        id: 213,
    },
    user: {
        uid: currentUser.id,
    },
    anonymous: 0,
    provider: null,
    averageGrade: 3,
};

const otherUserReviewMock = {
    id: randomProductReview.gradeId,
    type: 1,
    product: {
        id: randomProductReview.productId,
    },
    region: {
        id: 213,
    },
    user: {
        uid: user.id,
    },
    anonymous: 0,
    provider: null,
    averageGrade: 2,
};

const PRODUCT_REVIEW_TYPE = 1;

const productWithGradesOnly = {
    users: [user],
    modelOpinions: [],
    gradesOpinions: [{
        id: randomProductReview.gradeId,
        product: {
            id: randomProductReview.productId,
        },
        region: {
            id: 213,
        },
        type: PRODUCT_REVIEW_TYPE,
        pro: null,
        contra: null,
        comment: null,
        user: {
            uid: user.id,
        },
        photos: null,
        anonymous: 0,
        averageGrade: 1,
        created: new Date('2015-4-4 UTC').getTime(),
    }, {
        id: 2,
        product: {
            id: randomProductReview.productId,
        },
        region: {
            id: 213,
        },
        type: PRODUCT_REVIEW_TYPE,
        pro: null,
        contra: null,
        comment: null,
        user: {
            uid: user.id,
        },
        photos: null,
        anonymous: 0,
        averageGrade: 5,
    }],
};

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница отзывов на товар.', {
    environment: 'testing',
    issue: 'MARKETVERSTKA-24456',
    story: mergeSuites(
        makeSuite('Диалог подтверждения возраста. Adult контент.', {
            environment: 'kadavr',
            feature: 'Диалог подтверждения возраста',
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            adultConfirmationPopup() {
                                return this.createPageObject(AdultConfirmationPopup);
                            },
                        });

                        const state = mergeState([
                            preparedProductState,
                            {
                                data: {
                                    search: {adult: true},
                                },
                            },
                        ]);
                        await this.browser.setState('report', state);

                        return this.browser.yaOpenPage('market:product-reviews', {
                            productId: randomProductReview.productId,
                            slug: randomProductReview.slug,
                        });
                    },
                },
                prepareSuite(AdultWarningDefaultSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4034',
                    },
                }),
                prepareSuite(AdultWarningAcceptSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4039',
                    },
                }),
                prepareSuite(AdultWarningDeclineSuite, {
                    meta: {
                        issue: 'MARKETFRONT-7130',
                        id: 'marketfront-4044',
                    },
                })
            ),
        }),
        makeSuite('Список отзывов.', {
            environment: 'kadavr',
            story: {
                'Неавторизованный пользователь.': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                productReview: () => this.createPageObject(ProductReview),
                            });

                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    users: [user, commentator],
                                    modelOpinions: [otherUserReviewMock],
                                    comments: [{
                                        id: `child-0-${otherUserReviewMock.id}`,
                                        entity: `root-9-0-${otherUserReviewMock.id}`,
                                        parent: `root-9-0-${otherUserReviewMock.id}`,
                                        user: Number.parseInt(commentator.id, 10),
                                        comment: {
                                            body: 'Tro-lo-lo',
                                        },
                                        deleted: false,
                                    }],
                                });

                            return this.browser.yaOpenPage('market:product-reviews', {
                                productId: randomProductReview.productId,
                                slug: randomProductReview.slug,
                            });
                        },
                    },

                    prepareSuite(ProductReviewSuite, {
                        params: {
                            expectedRating: otherUserReviewMock.averageGrade,
                        },
                    }),
                    prepareSuite(ProductReviewNonUserReviewSuite, {
                        params: {
                            isAuthorized: false,
                        },
                    }),

                    prepareSuite(CommentAuthorSuite, {
                        hooks: {
                            beforeEach() {
                                // eslint-disable-next-line market/ginny/no-skip
                                return this.skip(
                                    'MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения, ' +
                                    'тикет на починку MARKETVERSTKA-32345'
                                );

                                // eslint-disable-next-line no-unreachable
                                const {
                                    product,
                                    page,
                                    reviewId,
                                    commentId,
                                    slug = 'random-fake-slug',
                                } = reviewProduct.comment.authorWithAchievements;


                                this.setPageObjects({
                                    productReviewItem: () => this.createPageObject(ProductReview, {
                                        root: `[data-review-id="${reviewId}"]`,
                                    }),
                                    userAchievementsModal: () => this.createPageObject(UserAchievementsModal),
                                });
                                this.setPageObjects({
                                    comment: () => this.createPageObject(
                                        Comment,
                                        {
                                            parent: this.productReviewItem,
                                            root: `[comment-id="${commentId}"]`,
                                        }
                                    ),
                                });
                                this.setPageObjects({
                                    commentAuthor: () => this.createPageObject(CommentAuthor, {
                                        parent: this.comment,
                                        root: '.n-comment__content',
                                    }),
                                });
                                this.setPageObjects({
                                    userMiniprofile: () => this.createPageObject(UserMiniprofile, {
                                        parent: this.commentAuthor,
                                        root: this.commentAuthor.container,
                                    }),
                                });

                                return this.browser.yaOpenPage('market:product-reviews', {
                                    productId: product,
                                    slug,
                                    page,
                                })
                                    .then(() => this.userMiniprofile.getUID())
                                    .then(uid => this.setPageObjects({
                                        userProfilePopupSnippet: () => this.createPageObject(
                                            UserProfilePopupSnippet,
                                            {
                                                root: `[data-popup-user="${uid}"]`,
                                            }
                                        ),
                                    }));
                            },
                        },
                    }),
                    prepareSuite(UserProfilePopupSnippetSuite, {
                        hooks: {
                            beforeEach() {
                                // eslint-disable-next-line market/ginny/no-skip
                                return this.skip(
                                    'MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения, ' +
                                    'тикет на починку MARKETVERSTKA-32345'
                                );

                                // eslint-disable-next-line no-unreachable
                                const {productId, page, reviewId, slug} = reviewProduct.userTooltips.plainUser;

                                return this.browser.yaOpenPage('market:product-reviews', {productId, slug, page})
                                    .then(() => this.setPageObjects({
                                        productReviewItem: () => this.createPageObject(
                                            ProductReview,
                                            {
                                                root: `[data-review-id="${reviewId}"]`,
                                            }
                                        ),
                                    }))
                                    .then(() => this.setPageObjects({
                                        userMiniprofile: () => this.createPageObject(
                                            UserMiniprofile,
                                            {
                                                parent: this.productReviewItem,
                                                root: this.productReviewItem.profile,
                                            }
                                        ),
                                    }))
                                    .then(() => this.userMiniprofile.getUID())
                                    .then(uid => this.setPageObjects({
                                        userProfilePopupSnippet: () => this.createPageObject(
                                            UserProfilePopupSnippet,
                                            {
                                                root: `[data-popup-user="${uid}"]`,
                                            }
                                        ),
                                    }));
                            },
                        },
                        params: {
                            willTooltipOpen: true,
                            hoverOnRootNode() {
                                return this.productReviewItem.hoverOnReviewProfile();
                            },
                            userName: 'Pupkin Vasily',
                            reviewLink: 'Написал 9 отзывов',
                            isExpertiseBlockVisible: true,
                        },
                    }),
                    {
                        'Тултипы для отзывов, владельцы которых скрыли свои профили.': prepareSuite(
                            UserMiniprofileAnonymousUserSuite,
                            {
                                hooks: {
                                    beforeEach() {
                                        // eslint-disable-next-line market/ginny/no-skip
                                        return this.skip(
                                            'MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения, ' +
                                            'тикет на починку MARKETVERSTKA-32345'
                                        );

                                        // eslint-disable-next-line no-unreachable
                                        const {
                                            productId,
                                            page,
                                            reviewId,
                                            slug,
                                        } = reviewProduct.userTooltips.anonymousUser;

                                        return this.browser.yaOpenPage(
                                            'market:product-reviews',
                                            {productId, slug, page}
                                        )
                                            .then(() => this.setPageObjects({
                                                productReviewItem: () => this.createPageObject(
                                                    ProductReview,
                                                    {
                                                        root: `[data-review-id="${reviewId}"]`,
                                                    }
                                                ),
                                            }))
                                            .then(() => this.setPageObjects({
                                                userMiniprofile: () => this.createPageObject(
                                                    UserMiniprofile,
                                                    {
                                                        parent: this.productReviewItem,
                                                        root: this.productReviewItem.profile,
                                                    }
                                                ),
                                            }));
                                    },
                                },
                            }
                        ),
                        'Тултипы для отзывов, владельцы которых удалили свой профиль.': prepareSuite(
                            UserMiniprofileDeletedUserSuite,
                            {
                                hooks: {
                                    beforeEach() {
                                        // eslint-disable-next-line market/ginny/no-skip
                                        return this.skip(
                                            'MARKETVERSTKA-31797 скипаем упавшие тесты для озеленения, ' +
                                            'тикет на починку MARKETVERSTKA-32345'
                                        );

                                        // eslint-disable-next-line no-unreachable
                                        const {
                                            productId,
                                            page,
                                            reviewId,
                                            slug,
                                        } = reviewProduct.userTooltips.deletedUser;

                                        return this.browser.yaOpenPage(
                                            'market:product-reviews',
                                            {productId, slug, page}
                                        )
                                            .then(() => this.setPageObjects({
                                                productReviewItem: () => this.createPageObject(
                                                    ProductReview,
                                                    {
                                                        root: `[data-review-id="${reviewId}"]`,
                                                    }
                                                ),
                                            }))
                                            .then(() => this.setPageObjects({
                                                userMiniprofile: () => this.createPageObject(
                                                    UserMiniprofile,
                                                    {
                                                        parent: this.productReviewItem,
                                                        root: this.productReviewItem.profile,
                                                    }
                                                ),
                                            }));
                                    },
                                },
                            }
                        ),
                    }
                ),

                'Авторизованный пользователь.': mergeSuites(
                    {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    users: [user, currentUser],
                                    gradesOpinions: [currentUserReviewMock],
                                    modelOpinions: [
                                        currentUserReviewMock,
                                        otherUserReviewMock,
                                    ],
                                });
                        },

                        afterEach() {
                            return this.browser.yaLogout();
                        },
                    },

                    {
                        'Отзыв текущего пользователя.': mergeSuites(
                            {
                                beforeEach() {
                                    this.setPageObjects({
                                        productReview: () => this.createPageObject(ProductReview),
                                    });
                                },
                            },

                            prepareSuite(ProductReviewSuite, {
                                hooks: {
                                    beforeEach() {
                                        return this.browser.yaProfile('pan-topinambur', 'market:product-reviews', {
                                            productId: randomProductReview.productId,
                                            slug: randomProductReview.slug,
                                        });
                                    },
                                },
                                params: {
                                    expectedRating: currentUserReviewMock.averageGrade,
                                },
                            }),
                            prepareSuite(CurrentUserReviewWithoutCommentSuite, {
                                hooks: {
                                    beforeEach() {
                                        return this.browser.yaProfile('pan-topinambur', 'market:product-reviews', {
                                            productId: randomProductReview.productId,
                                            slug: randomProductReview.slug,
                                        });
                                    },
                                },
                                params: {
                                    path: 'market:product-reviews-add',
                                    query: {
                                        productId: randomProductReview.productId,
                                    },
                                },
                            }),
                            {
                                'Удаление отзыва.': mergeSuites(
                                    {
                                        beforeEach() {
                                            return this.browser.yaProfile('pan-topinambur', 'market:product-reviews', {
                                                productId: randomProductReview.productId,
                                                slug: randomProductReview.slug,
                                            });
                                        },
                                    },

                                    prepareSuite(ProductReviewDeleteAcceptedSuite, {
                                        suiteName: 'Удаление с подтверждением.',
                                        meta: {
                                            id: 'marketfront-2329',
                                            issue: 'MARKETVERSTKA-27803',
                                        },
                                    }),

                                    prepareSuite(ProductReviewDeleteDisclaimedSuite, {
                                        suiteName: 'Отказ от удаления.',
                                        meta: {
                                            id: 'marketfront-2328',
                                            issue: 'MARKETVERSTKA-27803',
                                        },
                                    })
                                ),
                            }
                        ),
                    },

                    {
                        'Отзыв текущего пользователя на модификацию.': mergeSuites(
                            {
                                async beforeEach() {
                                    const modificationReview = {...currentUserReviewMock};
                                    modificationReview.product = {id: modificationProductId};
                                    await this.browser
                                        .setState('schema', {
                                            users: [user, currentUser],
                                            gradesOpinions: [modificationReview],
                                            modelOpinions: [currentUserReviewMock],
                                        });
                                    this.setPageObjects({
                                        productReview: () => this.createPageObject(ProductReview),
                                    });
                                },
                            },
                            prepareSuite(CurrentUserReviewWithoutCommentSuite, {
                                hooks: {
                                    beforeEach() {
                                        return this.browser.yaProfile('pan-topinambur', 'market:product-reviews', {
                                            productId: randomProductReview.productId,
                                            slug: randomProductReview.slug,
                                        });
                                    },
                                },
                                params: {
                                    path: 'market:product-reviews-add',
                                    query: {
                                        productId: modificationProductId,
                                    },
                                },
                            })
                        ),
                    },

                    {
                        'Отзыв другого пользователя.': mergeSuites(
                            {
                                beforeEach() {
                                    this.setPageObjects({
                                        productReview: () => this.createPageObject(ProductReview, {
                                            parent: ProductReview.getReviewByIndex(1),
                                        }),
                                    });

                                    return this.browser.yaProfile('pan-topinambur', 'market:product-reviews', {
                                        productId: randomProductReview.productId,
                                        slug: randomProductReview.slug,
                                    });
                                },
                            },

                            prepareSuite(ProductReviewSuite, {
                                params: {
                                    expectedRating: otherUserReviewMock.averageGrade,
                                },
                            }),
                            prepareSuite(ProductReviewNonUserReviewSuite, {
                                params: {
                                    isAuthorized: true,
                                },
                            })),
                    }
                ),

                'Импортированный отзыв.': prepareSuite(ProductReviewProviderInfoSuite, {
                    pageObjects: {
                        productReview() {
                            return this.createPageObject(ProductReview);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    users: [user],
                                    modelOpinions: [{
                                        id: randomProductReview.gradeId,
                                        type: 1,
                                        product: {
                                            id: randomProductReview.productId,
                                        },
                                        region: {
                                            id: 213,
                                        },
                                        user: {
                                            uid: user.uid.value,
                                        },
                                        provider: {
                                            type: 'vendor',
                                            name: 'Sony',
                                        },
                                    }],
                                });

                            return this.browser.yaOpenPage('market:product-reviews', {
                                productId: randomProductReview.productId,
                                slug: randomProductReview.slug,
                            });
                        },
                    },
                }),

                'Отзыв с фотографией.': prepareSuite(ProductReviewWithPhotoSuite, {
                    pageObjects: {
                        productReview() {
                            return this.createPageObject(ProductReview);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    users: [user],
                                    modelOpinions: [{
                                        ...otherUserReviewMock,
                                        photos: [{
                                            entity: 'photo',
                                            namespace: 'market-ugc',
                                            groupId: '65432',
                                            imageName: 'slkhakbfiahgier',
                                        }],
                                    }],
                                });

                            return this.browser.yaOpenPage('market:product-reviews', {
                                'productId': randomProductReview.productId,
                                'slug': randomProductReview.slug,
                                'with-photo': '1',
                            });
                        },
                    },
                }),

                'Отзыв от проверенного покупателя.': prepareSuite(ProductReviewWithUserCpaSuite, {
                    pageObjects: {
                        productReview() {
                            return this.createPageObject(ProductReview);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    users: [user],
                                    modelOpinions: [{
                                        ...otherUserReviewMock,
                                        cpa: true,
                                    }],
                                });

                            return this.browser.yaOpenPage('market:product-reviews', {
                                'productId': randomProductReview.productId,
                                'slug': randomProductReview.slug,
                            });
                        },
                    },
                }),

                'Отзыв с экпертизой.': prepareSuite(ProductReviewWithUserExpertiseSuite, {
                    pageObjects: {
                        productReview() {
                            return this.createPageObject(ProductReview);
                        },
                        authorExpertise() {
                            return this.createPageObject(AuthorExpertise);
                        },
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('storage', {userExpertise: [userExpertise]})
                                .setState('schema', {
                                    users: [currentUser],
                                    gradesOpinions: [currentUserReviewMock],
                                    modelOpinions: [currentUserReviewMock],
                                });

                            return this.browser.yaOpenPage('market:product-reviews', {
                                'productId': randomProductReview.productId,
                                'slug': randomProductReview.slug,
                            });
                        },
                    },
                }),
            },
        }),

        makeSuite('Панель фильтров.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        _.assign(this.params, {
                            query: {
                                productId: randomProductReview.productId,
                                slug: randomProductReview.slug,
                            },
                            path: 'market:product-reviews',
                        });

                        await this.browser.setState('report', preparedProductState);
                    },
                },

                prepareSuite(ReviewsPhotoFilterReviewWithPhotoSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('schema', {
                                users: [user],
                                modelOpinions: [{
                                    ...otherUserReviewMock,
                                    photos: [{
                                        entity: 'photo',
                                        namespace: 'market-ugc',
                                        groupId: '65432',
                                        imageName: 'slkhakbfiahgier',
                                    }],
                                }],
                            });
                        },
                    },
                }),
                prepareSuite(ReviewsPhotoFilterNoneReviewWithPhotoSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('schema', {
                                users: [user],
                                modelOpinions: [{
                                    ...otherUserReviewMock,
                                    photos: null,
                                }],
                            });
                        },
                    },
                })
            ),
        }),

        prepareSuite(ProductHeadlineNewStickerSuite, {
            pageObjects: {
                sticker() {
                    return this.createPageObject(NoveltyBadge, {
                        root: ProductHeadline.root,
                    });
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', newBadgeGuruMock);

                    return this.browser.yaOpenPage('market:product-reviews', {
                        productId: newBadgeProductId,
                        slug: newBadgeSlug,
                    });
                },
            },
        }),

        {
            'Хлебные крошки.': mergeSuites(
                {
                    beforeEach() {
                        this.setPageObjects({
                            headline: () => this.createPageObject(ProductHeadline),
                            breadcrumbs: () => this.createPageObject(Breadcrumbs),
                        });

                        return this.browser.yaOpenPage('market:product-reviews', {
                            'productId': routes.reviews.product.withUserCpa.productId,
                            'slug': routes.reviews.product.withUserCpa.slug,
                        });
                    },
                },
                prepareSuite(BreadcrumbsSuite),
                prepareSuite(BreadcrumbsItemClickableYesSuite, {
                    params: {
                        categoryName: 'Фотоаппараты',
                    },
                })
            ),
        },
        prepareSuite(HeadBannerProductAbsenceSuite, {
            meta: {
                id: 'marketfront-3388',
                issue: 'MARKETVERSTKA-33961',
            },
            params: {
                pageId: 'market:product-reviews',
                slug: randomProductReview.slug,
                productId: randomProductReview.productId,
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', preparedProductState);
                    await this.browser.setState('schema', {
                        users: [user],
                        modelOpinions: [{
                            ...otherUserReviewMock,
                            photos: null,
                        }],
                    });
                },
            },
        }),
        seo,
        defaultOffer,

        makeSuite('Отзывы без текста.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser
                            .setState('report', preparedProductState)
                            .setState('schema', productWithGradesOnly);

                        this.setPageObjects({
                            productReview: () => this.createPageObject(ProductReview, {
                                parent: ProductReview.getReviewByIndex(1),
                            }),
                        });

                        return this.browser.yaOpenPage('market:product-reviews', {
                            'productId': randomProductReview.productId,
                            'slug': randomProductReview.slug,
                        });
                    },
                },

                prepareSuite(ProductGradesTitleSuite, {
                    hooks: {
                        beforeEach() {
                            this.setPageObjects({
                                reviewsToolbar: () => this.createPageObject(ReviewsToolbar),
                            });
                        },
                    },
                }),
                prepareSuite(ProductReviewSuite, {
                    params: {
                        expectedRating: productWithGradesOnly.gradesOpinions[1].averageGrade,
                    },
                })
            ),
        }),

        makeSuite('Создание отзыва.', {
            id: 'marketfront-1137',
            environment: 'kadavr',
            feature: 'Создание отзыва',
            story: {
                'Пустой список отзывов.': prepareSuite(AddReviewButtonSuite, {
                    pageObjects: {
                        addReviewButton() {
                            return this.createPageObject(ProductReviewsPlaceholder);
                        },
                    },

                    params: {
                        productId: randomProductReview.productId,
                    },

                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState);

                            await this.browser.yaOpenPage('market:product-reviews', {
                                productId: randomProductReview.productId,
                                slug: randomProductReview.slug,
                            });
                        },
                    },
                }),
                'Непустой список отзывов.': prepareSuite(ReviewsPageSidebarAddReviewSuite, {
                    pageObjects: {
                        productReviewsPage() {
                            return this.createPageObject(ProductReviewsPage);
                        },
                    },

                    params: {
                        productId: randomProductReview.productId,
                    },

                    hooks: {
                        async beforeEach() {
                            await this.browser
                                .setState('report', preparedProductState)
                                .setState('schema', {
                                    modelOpinions: [otherUserReviewMock],
                                });

                            await this.browser.yaOpenPage('market:product-reviews', {
                                productId: randomProductReview.productId,
                                slug: randomProductReview.slug,
                            });
                        },
                    },
                }),
            },
        }),
        makeSuite('Виджет UGC Медиа галереи.', {
            environment: 'kadavr',
            story: prepareSuite(UgcMediaGallerySuite, {
                hooks: {
                    async beforeEach() {
                        await this.browser
                            .setState('report', reviewWithCommentAndPhotos.reportProduct)
                            .setState('schema', reviewWithCommentAndPhotos.oneReviewSchemaWithCommentary);

                        const testUser = profiles['pan-topinambur'];
                        await this.browser.yaLogin(
                            testUser.login,
                            testUser.password
                        );

                        await this.browser.yaOpenPage('market:product-reviews', {
                            productId: reviewWithCommentAndPhotos.product.productId,
                            slug: reviewWithCommentAndPhotos.product.slug,
                        });

                        // await this.browser.yaSlowlyScroll(UgcMediaGallery.root);
                    },
                },
                pageObjects: {
                    ugcMediaGallery() {
                        return this.createPageObject(UgcMediaGallery);
                    },
                },
                params: {
                    productId: reviewWithCommentAndPhotos.product.productId,
                    slug: reviewWithCommentAndPhotos.product.slug,
                },
            }),
        }),
        makeSuite('Блок "Топ 6"', {
            environment: 'kadavr',
            story: mergeSuites(
                makeSuite('DSBS-оффер в топ-6', {
                    story: mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', productWithTop6OfferDSBS.state);

                                return this.browser.yaOpenPage('market:product-reviews', productWithTop6OfferDSBS.route);
                            },
                        },
                        prepareSuite(ItemCounterCartButtonSuite, {
                            params: {
                                counterStep: 1,
                                offerId: productWithTop6OfferDSBS.dsbsOffer.wareId,
                                withCartPopup: true,
                            },
                            meta: {
                                id: 'marketfront-4353',
                            },
                            pageObjects: {
                                parent() {
                                    return this.createPageObject(TopOfferSnippet);
                                },
                                cartButton() {
                                    return this.createPageObject(CartButton, {
                                        parent: TopOfferSnippet.root,
                                    });
                                },
                                counterCartButton() {
                                    return this.createPageObject(CounterCartButton, {
                                        parent: TopOfferSnippet.root,
                                    });
                                },
                                cartPopup() {
                                    return this.createPageObject(CartPopup);
                                },
                            },
                        }),
                        prepareSuite(DsbsFullOfferSuite, {
                            params: {
                                urls: productWithTop6OfferDSBS.dsbsOffer.urls,
                            },
                        })
                    ),
                })
            ),
        }),
        prepareSuite(ProductReviewsDegradationSuite, {
            environment: 'kadavr',
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage('market:product-reviews', {
                        productId: randomProductReview.productId,
                        slug: randomProductReview.slug,
                    });
                },
            },
            pageObjects: {
                productReviewsPage() {
                    return this.createPageObject(ProductReviewsPage);
                },
            },
        })
    ),
});
