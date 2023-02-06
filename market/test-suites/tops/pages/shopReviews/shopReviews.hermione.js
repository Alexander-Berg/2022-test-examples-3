import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {createShopInfo} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/shop-reviews-page';
import {routes} from '@self/platform/spec/hermione/configs/routes';

// suites
import CurrentUserReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview';
import CurrentUserReviewEditReviewSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview/editReview';
import CurrentUserReviewRemoveReviewSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview/removeReview';
import CurrentUserReviewVotesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview/votes';
import CurrentUserReviewAuthorLinkSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview/authorLink';
import CurrentUserReviewExpanderSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReview/expander';
import CurrentUserReviewListMultipleReviewsSuite from
    '@self/platform/spec/hermione/test-suites/blocks/components/ShopReview/CurrentUserReviewList/multipleReviews';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';

// page-objects
import CurrentUserReview from '@self/platform/spec/page-objects/components/ShopReview/CurrentUserReview';
import CurrentUserReviewList from '@self/platform/spec/page-objects/components/ShopReview/CurrentUserReviewList';
import Review from '@self/platform/spec/page-objects/components/ShopReview/Review';
import Votes from '@self/platform/spec/page-objects/components/Votes';
import Header from '@self/platform/spec/page-objects/components/Review/Header';
import RemovePromptDialog from '@self/platform/spec/page-objects/components/RemovePromptDialog';
import Base from '@self/platform/spec/page-objects/n-base';
import ShopRatingWarningByDefaultSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/ShopRatingWarning/byDefault';
import ShopRatingWarningBadShopSuite
    from '@self/project/src/spec/hermione/test-suites/blocks/widgets/content/ShopRatingWarning/badShop';
import ShopRatingWarning from '@self/project/src/components/ShopRatingWarning/__pageObject/index.touch';
import UserExpertisePopup from '@self/platform/spec/page-objects/widgets/content/UserExpertisePopup';
import Controls from '@self/platform/spec/page-objects/components/Comment/Controls';
import ZeroStateCard from '@self/project/src/components/ZeroStateCard/__pageObject';
import ShopReviewsDegradationSuite from '@self/platform/spec/hermione/test-suites/blocks/ShopReview/degradation';

const SHOP_REVIEW_TYPE = 0;

const shopInfo = createShopInfo({
    entity: 'shop',
    id: routes.shop.shopId,
    status: 'actual',
    oldStatus: 'actual',
    slug: 'some',
    ratingToShow: 3.166666667,
    overallGradesCount: 218,
}, routes.shop.shopId);


const longText = 'lorem ipsum dolor sit amet, consectetur adipiscing elit ut aliquam, ' +
    'purus sit amet luctus venenatis, lectus magna fringilla urna, porttitor rhoncus ' +
    'dolor purus non enim praesent elementum facilisis ' +
    'lorem ipsum dolor sit amet, consectetur adipiscing elit ut aliquam, ' +
    'purus sit amet luctus venenatis, lectus magna fringilla urna, porttitor rhoncus ' +
    'dolor purus non enim praesent elementum facilisis';

function getReview({id, shopId, uid, resolved}) {
    return {
        id: id || 1,
        shop: {
            id: shopId,
        },
        created: new Date('2015-01-01').getTime(),
        averageGrade: 2,
        type: SHOP_REVIEW_TYPE,
        cpa: true,
        pro: longText,
        contra: longText,
        comment: longText,
        anonymous: 0,
        user: {
            uid,
        },
        photos: null,
        votes: {
            agree: 300,
            reject: 200,
            total: 500,
        },
        resolved,
    };
}

function getUser({uid}) {
    return {
        id: uid,
        uid: {
            value: uid,
        },
        display_name: {
            name: 'someDisplayName',
            public_name: 'somePublicDisplayName',
            display_name_empty: false,
            avatar: {
                default: '36777/515095637-1546054309',
                empty: false,
            },
        },
        login: 'login',
        regname: '100500',
        dbfields: {
            'userinfo.firstname.uid': 'Vasily',
            'userinfo.lastname.uid': 'Pupkin',
        },
    };
}

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница отзывов на магазин', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Пользователь авторизован. 1 отзыв в блоке «Ваш отзыв»', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const uid = '636368980';
                        const review = getReview({
                            shopId: routes.shop.shopId,
                            uid,
                            resolved: 0, // для отображения кнопки Изменить в меню отзыва
                        });
                        const schema = {
                            users: [getUser({uid})],
                            modelOpinions: [review],
                            gradesOpinions: [review],
                            shopGrades: [review],
                        };
                        await this.browser.setState('schema', schema);
                        await this.browser.setState('report', shopInfo);
                        await this.browser.yaProfile(
                            'ugctest3',
                            'touch:shop-reviews',
                            {shopId: routes.shop.shopId}
                        );
                    },
                },
                prepareSuite(CurrentUserReviewSuite, {
                    params: {
                        pro: longText,
                        contra: longText,
                        comment: longText,
                    },
                    pageObjects: {
                        currentUserReview() {
                            return this.createPageObject(CurrentUserReview);
                        },
                        review() {
                            return this.createPageObject(Review, {
                                parent: CurrentUserReview.root,
                            });
                        },
                        header() {
                            return this.createPageObject(Header, {
                                parent: CurrentUserReview.root,
                            });
                        },
                        votes() {
                            return this.createPageObject(Votes, {
                                parent: CurrentUserReview.root,
                            });
                        },
                    },
                }),
                prepareSuite(CurrentUserReviewEditReviewSuite, {
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header, {
                                parent: CurrentUserReview.root,
                            });
                        },
                        controls() {
                            return this.createPageObject(Controls);
                        },
                    },
                }),
                prepareSuite(CurrentUserReviewRemoveReviewSuite, {
                    pageObjects: {
                        currentUserReview() {
                            return this.createPageObject(CurrentUserReview);
                        },
                        header() {
                            return this.createPageObject(Header, {
                                parent: CurrentUserReview.root,
                            });
                        },
                        controls() {
                            return this.createPageObject(Controls);
                        },
                        removePromptDialog() {
                            return this.createPageObject(RemovePromptDialog);
                        },
                    },
                }),
                prepareSuite(CurrentUserReviewVotesSuite, {
                    pageObjects: {
                        votes() {
                            return this.createPageObject(Votes, {
                                parent: CurrentUserReview.root,
                            });
                        },
                    },
                }),

                prepareSuite(CurrentUserReviewAuthorLinkSuite, {
                    pageObjects: {
                        header() {
                            return this.createPageObject(Header, {
                                parent: CurrentUserReview.root,
                            });
                        },
                        userExpertisePopup() {
                            return this.createPageObject(UserExpertisePopup);
                        },
                    },
                }),
                prepareSuite(CurrentUserReviewExpanderSuite, {
                    pageObjects: {
                        review() {
                            return this.createPageObject(Review, {
                                parent: CurrentUserReview.root,
                            });
                        },
                    },
                })
            ),
        }),

        makeSuite('Пользователь авторизован. 2 отзыва в блоке «Ваш отзыв»', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const shopId = routes.shop.shopId;
                        const uid = '636368980';
                        const schema = {
                            users: [getUser({uid})],
                            modelOpinions: [
                                getReview({id: 1, shopId, uid}),
                                getReview({id: 2, shopId, uid}),
                            ],
                        };
                        await this.browser.setState('report', shopInfo);
                        await this.browser.setState('schema', schema);
                        await this.browser.yaProfile('ugctest3', 'touch:shop-reviews', {shopId});
                    },
                },
                prepareSuite(CurrentUserReviewListMultipleReviewsSuite, {
                    pageObjects: {
                        currentUserReviewList() {
                            return this.createPageObject(CurrentUserReviewList);
                        },
                    },
                })
            ),
        }),

        makeSuite('SEO-разметка страницы.', {
            story: createStories(
                seoTestConfigs,
                ({routeConfig, testParams}) => prepareSuite(BaseLinkCanonicalSuite, {
                    hooks: {
                        beforeEach() {
                            return this.browser
                                .yaSimulateBot()
                                .yaOpenPage('touch:shop-reviews', routeConfig);
                        },
                    },
                    pageObjects: {
                        base() {
                            return this.createPageObject(Base);
                        },
                    },
                    params: testParams,
                })
            ),
        }),

        prepareSuite(ShopRatingWarningByDefaultSuite, {
            meta: {
                id: 'm-touch-3438',
                issue: 'MARKETFRONT-13131',
            },
            hooks: {
                async beforeEach() {
                    const uid = '636368980';
                    const review = getReview({shopId: routes.shop.shopId, uid});
                    const schema = {
                        users: [getUser({uid})],
                        modelOpinions: [review],
                        gradesOpinions: [review],
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', shopInfo);
                    await this.browser.yaOpenPage('touch:shop-reviews', {
                        shopId: routes.shop.shopId,
                    });
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
                id: 'm-touch-3436',
                issue: 'MARKETFRONT-13131',
            },
            hooks: {
                async beforeEach() {
                    const uid = '636368980';
                    const review = getReview({shopId: routes.shop.shopId, uid});
                    const schema = {
                        users: [getUser({uid})],
                        modelOpinions: [review],
                        gradesOpinions: [review],
                    };
                    await this.browser.setState('schema', schema);
                    await this.browser.setState('report', createShopInfo({
                        entity: 'shop',
                        id: routes.shop.shopId,
                        status: 'actual',
                        oldStatus: 'actual',
                        slug: 'some',
                        ratingToShow: 3.166666667,
                        overallGradesCount: 218,
                        ratingType: 0,
                        cutoff: '06.07.2022',
                    }, routes.shop.shopId));
                    await this.browser.yaOpenPage('touch:shop-reviews', {
                        shopId: routes.shop.shopId,
                    });
                },
            },
            pageObjects: {
                shopRatingWarning() {
                    return this.createPageObject(ShopRatingWarning);
                },
            },
        }),
        prepareSuite(ShopReviewsDegradationSuite, {
            environment: 'testing',
            pageObjects: {
                zeroState() {
                    return this.createPageObject(ZeroStateCard);
                },
            },
            hooks: {
                async beforeEach() {
                    return this.browser
                        .yaOpenPage('touch:shop-reviews', routes.shop);
                },
            },
        })
    ),
});
