import {assign} from 'lodash/fp';
import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {mergeState, createProduct} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import {createPriceRange} from '@yandex-market/kadavr/mocks/Report/helpers/price';
import {randomString} from '@self/root/src/helpers/string';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
// configs
import {profiles} from '@self/platform/spec/hermione/configs/profiles';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import seoTestConfigs from '@self/platform/spec/hermione/configs/seo/product-page';
// suites
import ProductReviewsTenMoreReviewsSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReviews/tenMoreReviews';
import ProductReviewExpandableContentSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/expandableContent';
import RatingChubbyStarsGradeValueSuite from '@self/platform/spec/hermione/test-suites/blocks/RatingChubbyStars/gradeValue';
import BaseSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base';
import BaseLinkCanonicalSuite from '@self/platform/spec/hermione/test-suites/blocks/n-base/__link-canonical';
import ProductGradeSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductGrade';
import AuthorExpertiseSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/userExpertise';
import ProductOffersStaticListAllOffersSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersStaticList/allOffers';
import ProductOffersSnippetListSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippetList';
import ProductOffersSnippetClickOutLinkSuite from
    '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/clickOutLink';
import ProductOffersSnippetPriceSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/price';
import ProductOffersSnippetShopRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffersSnippet/shopRating';
import DefaultOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/DefaultOffer';
import TwoLevelCommentariesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/Commentaries/twoLevelCommentaries';
import UgcMediaGallerySuite from '@self/platform/spec/hermione/test-suites/blocks/UgcMediaGallery';
import HighratedSimilarProductsSuite from '@self/platform/spec/hermione/test-suites/blocks/HighratedSimilarProducts';
import ProductReviewsDegradationSuite from '@self/project/src/spec/hermione/test-suites/blocks/ProductReviews/degradation';
// page-objects
import HighratedSimilarProducts from '@self/platform/spec/page-objects/widgets/content/HighratedSimilarProducts';
import DefaultOffer from '@self/platform/spec/page-objects/components/DefaultOffer';
import ProductOffersStaticList from '@self/platform/widgets/parts/ProductOffersStaticList/__pageObject';
import ProductOffersSnippetList from '@self/platform/containers/ProductOffersSnippetList/__pageObject';
import ProductOfferSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Review from '@self/platform/spec/page-objects/b-review';
import ProductReviews from '@self/platform/spec/page-objects/widgets/parts/ProductReviews';
import AuthorExpertise from '@self/root/src/components/AuthorExpertise/__pageObject';
import UserExpertisePopup from '@self/platform/spec/page-objects/widgets/content/UserExpertisePopup';
import ProductReview from '@self/platform/spec/page-objects/ProductReview';
import ProductGrade from '@self/platform/spec/page-objects/ProductGrade';
import RatingChubbyStars from '@self/platform/spec/page-objects/RatingChubbyStars';
import Base from '@self/platform/spec/page-objects/n-base';
import ReviewContent from '@self/platform/spec/page-objects/ReviewContent';
import GalleryUgcSlider from '@self/platform/components/Gallery/GalleryUgcSlider/__pageObject';
import ProductPage from '@self/platform/spec/page-objects/ProductPage';
// helpers
import {hideSmartBannerPopup} from '@self/platform/spec/hermione/helpers/smartBannerPopup';
import {
    productWithDefaultOffer,
    phoneProductRoute,
} from '@self/platform/spec/hermione/fixtures/product';
import {
    buildProductOffersResultsState,
    URL_TO_SHOP,
    PRICE,
} from '@self/platform/spec/hermione/fixtures/product/productOffers';
import {createUserReviewWithPhotos} from '@self/platform/spec/hermione/fixtures/review';
import {picture, product, category} from './kadavrMocks';
import {
    signedUpWithSocialNetworkUser,
    productMock,
    productCfg,
    defaultReview,
    basicProductGradesSchema,
    user,
    modelOpinionLong,
    modelOpinionShort,
    userExpertise,
} from './fixtures/modelOpinions';


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница "Отзывы" карточки модели.', {
    environment: 'testing',
    story: mergeSuites(
        makeSuite('Список отзывов.', {
            environment: 'kadavr',
            story: prepareSuite(ProductReviewsTenMoreReviewsSuite, {
                hooks: {
                    async beforeEach() {
                        await hideSmartBannerPopup(this);
                        return this.browser.setState('schema', {
                            users: [user],
                            modelOpinions: new Array(15)
                                .fill(null)
                                .map((item, index) => ({
                                    ...modelOpinionShort,
                                    id: index,
                                })),
                        })
                            .then(() => this.browser.setState('report', productMock))
                            .then(() => this.browser.yaOpenPage('touch:product-reviews', productCfg))
                            .then(() => this.browser.yaClosePopup(this.createPageObject(RegionPopup)));
                    },
                },
                pageObjects: {
                    productReviews() {
                        return this.createPageObject(ProductReviews);
                    },
                },
            }),
        }),
        makeSuite('Блок отзыва.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await hideSmartBannerPopup(this);
                        return this.browser.setState('schema', {
                            users: [user],
                            modelOpinions: [modelOpinionShort, modelOpinionLong],
                        })
                            .then(() => this.browser.setState('storage', {userExpertise: [userExpertise]}))
                            .then(() => this.browser.setState('report', productMock))
                            .then(() => this.browser.yaOpenPage('touch:product-reviews', productCfg))
                            .then(() => this.browser.yaClosePopup(this.createPageObject(RegionPopup)));
                    },
                },
                prepareSuite(ProductReviewExpandableContentSuite, {
                    pageObjects: {
                        reviewContent() {
                            return this.createPageObject(ReviewContent, {
                                root: `${ProductReview.root}[data-id="${modelOpinionLong.id}"]`,
                            });
                        },
                    },
                }),
                prepareSuite(RatingChubbyStarsGradeValueSuite, {
                    pageObjects: {
                        review() {
                            return this.createPageObject(ProductReview);
                        },
                        stars() {
                            return this.createPageObject(RatingChubbyStars, {
                                parent: this.review,
                            });
                        },
                    },
                    hooks: {
                        beforeEach() {
                            return this.review
                                .getId()
                                .then(reviewId => {
                                    this.params = assign(this.params, {
                                        reviewId,
                                        slug: productCfg.slug,
                                        productId: productCfg.productId,
                                    });
                                });
                        },
                    },
                    params: {
                        expectedRatingValue: 3,
                    },
                }),
                prepareSuite(AuthorExpertiseSuite, {
                    pageObjects: {
                        authorExpertise() {
                            return this.createPageObject(AuthorExpertise);
                        },
                        userExpertisePopup() {
                            return this.createPageObject(UserExpertisePopup);
                        },
                    },
                    params: {
                        publicId: user.public_id,
                    },
                })
            ),
        }),

        makeSuite('Имя пользователя.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        await hideSmartBannerPopup(this);
                        this.setPageObjects({
                            review: () => this.createPageObject(Review, {
                                root: `[data-review-id="${defaultReview.id}"]`,
                            }),
                            productReview: () => this.createPageObject(ProductReview),
                        });
                    },
                },
                {
                    'Страница конкретного отзыва.': {
                        'Виджет комментариев': prepareSuite(TwoLevelCommentariesSuite, {
                            params: {
                                pageTemplate: 'touch:product-review',
                                pageParams: {
                                    ...productCfg,
                                    'reviewId': defaultReview.id,
                                    'no-tests': 1,
                                },
                                entityId: defaultReview.id,
                                defaultLimit: 5,
                            },
                            hooks: {
                                async beforeEach() {
                                    this.params = {
                                        ...this.params,
                                        schema: {
                                            users: [signedUpWithSocialNetworkUser],
                                            modelOpinions: [defaultReview],
                                        },
                                    };
                                    await this.browser.setState('report', productMock);
                                },
                            },
                        }),
                    },
                }
            ),
        }),

        makeSuite('Гуру-модель.', {
            environment: 'kadavr',
            story: prepareSuite(BaseSuite, {
                hooks: {
                    async beforeEach() {
                        const {routeParams, testParams, mocks} = product.guru;
                        const productWithPicture = mergeReportState([mocks.report, picture]);

                        await this.browser.setState('report', productWithPicture);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product-reviews', routeParams);

                        this.params = assign(
                            testParams,
                            this.params
                        );
                    },
                },
            }),
        }),

        makeSuite('Кластер.', {
            environment: 'kadavr',
            story: prepareSuite(BaseSuite, {
                hooks: {
                    async beforeEach() {
                        const {routeParams, testParams, mocks} = product.cluster;
                        const productWithPicture = mergeReportState([mocks.report, picture]);

                        await this.browser.setState('report', productWithPicture);
                        await this.browser.yaSimulateBot();
                        await this.browser.yaOpenPage('touch:product-reviews', routeParams);

                        this.params = assign(
                            testParams,
                            this.params
                        );
                    },
                },
            }),
        }),

        {
            beforeEach() {
                this.setPageObjects({
                    base: () => this.createPageObject(Base),
                });
            },

            'Групповая модель.': prepareSuite(BaseSuite, {
                hooks: {
                    beforeEach() {
                        const mattress = routes.product.mattress;

                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product-reviews', mattress)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                            `${mattress.slug}/${mattress.productId}/reviews`,
                                        expectedOpenGraphDescription: 'Матрас Аскона Victory: отзывы покупателей' +
                                            ' на Яндекс Маркете. Достоинства и недостатки товара.' +
                                            ' Важная информация о товаре Матрас Аскона Victory:' +
                                            ' описание, фотографии, цены, варианты доставки, магазины на карте.',
                                        expectedOpenGraphTitleRegex: '^Стоит ли покупать Матрас Аскона Victory\\? ' +
                                            'Отзывы на Яндекс\\sМаркете$',
                                    },
                                    this.params
                                );
                            });
                    },
                },
            }),

            'Книга.': prepareSuite(BaseSuite, {
                params: {
                    // MOBMARKET-10116: skip падающих тестов на opengraph:image,
                    // тикет на починку MOBMARKET-9726
                    skipOpenGraphImage: true,
                },
                hooks: {
                    beforeEach() {
                        const book = routes.product.book;

                        return this.browser
                            .yaSimulateBot()
                            .yaOpenPage('touch:product-reviews', book)
                            .then(() => {
                                this.params = assign(
                                    {
                                        expectedCanonicalLink: 'https://market.yandex.ru/product--' +
                                            `${book.slug}/${book.productId}/reviews`,
                                        expectedOpenGraphDescription: 'Пауло Коэльо "Алхимик" на Яндекс Маркете' +
                                            ' — отзывов пока нет. Цены, характеристики книги Пауло Коэльо "Алхимик"',
                                        expectedOpenGraphTitleRegex: '^Стоит ли покупать Пауло Коэльо "Алхимик"\\? ' +
                                            'Отзывы на Яндекс Маркете$',
                                    },
                                    this.params
                                );
                            });
                    },
                },
            }),

            'SEO-разметка страницы.': createStories(
                seoTestConfigs.pageCanonical,
                ({routeConfig, testParams, description}) => prepareSuite(BaseLinkCanonicalSuite, {
                    hooks: {
                        beforeEach() {
                            if (description === 'Тип "Визуальная"') {
                                // eslint-disable-next-line market/ginny/no-skip
                                return this.skip(
                                    'MOBMARKET-10116: Скипаем падающие автотесты, ' +
                                    'тикет на починку MOBMARKET-9726'
                                );
                            }

                            return this.browser
                                .yaSimulateBot()
                                .yaOpenPage('touch:product-reviews', routeConfig);
                        },
                    },
                    params: testParams.reviews,
                })
            ),
        },

        makeSuite('Оценки - отзывы без текста', {
            environment: 'kadavr',
            story: mergeSuites(
                prepareSuite(ProductGradeSuite, {
                    pageObjects: {
                        productGrade() {
                            return this.createPageObject(ProductGrade);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const schema = {
                                ...basicProductGradesSchema,
                                users: [{
                                    id: '100500',
                                    uid: {
                                        value: '100500',
                                    },
                                    login: 'fake-user',
                                    regname: '100500',
                                }],
                            };

                            return Promise.all([
                                this.browser.setState('schema', schema),
                                this.browser.setState('report', productMock),
                            ])
                                .then(() => this.browser.yaOpenPage('touch:product-reviews', productCfg))
                                .then(() => this.browser.yaClosePopup(this.createPageObject(RegionPopup)));
                        },
                    },
                })
            ),
        }),
        makeSuite('Товар с низким рейтингом.', {
            environment: 'kadavr',
            story: prepareSuite(HighratedSimilarProductsSuite, {
                pageObjects: {
                    highratedSimilarProducts() {
                        return this.createPageObject(HighratedSimilarProducts);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const productsCount = 3;
                        const productId = routes.product.nokia.productId;
                        const lowRatedProduct = {...productMock};
                        lowRatedProduct.collections.product[productId].preciseRating = 3;

                        const otherProducts = [];

                        for (let i = 0; i < productsCount; i++) {
                            otherProducts.push(createProduct({
                                showUid: `${randomString()}_${i}`,
                                slug: 'test-product',
                                categories: [category],
                                preciseRating: 5,
                            }));
                        }

                        const reportState = mergeState([
                            lowRatedProduct,
                            ...otherProducts,
                        ]);

                        await this.browser.setState('report', reportState);
                        await this.browser.yaOpenPage('touch:product', routes.product.nokia);
                    },
                },
                params: {
                    snippetsCount: 3,
                },
            }),
        }),
        makeSuite('Статичный виджет ДО и Топ6', {
            environment: 'kadavr',
            story: mergeSuites({
                async beforeEach() {
                    const state = buildProductOffersResultsState({
                        offersCount: 100,
                    });
                    await this.browser.setState('report', state);
                    await this.browser.yaSetCookie({
                        name: 'iugc',
                        value: '1',
                    });
                    await this.browser.yaOpenPage('touch:product-reviews', phoneProductRoute);
                    // Виджет с ДО загружается лениво.
                    await this.browser.yaSlowlyScroll('[data-zone-name="productReviewsNewReview"]');
                },

                'Содержимое.': mergeSuites(
                    prepareSuite(ProductOffersStaticListAllOffersSuite, {
                        params: {
                            productId: phoneProductRoute.productId,
                            slug: phoneProductRoute.slug,
                        },
                        pageObjects: {
                            productOffersStaticList() {
                                return this.createPageObject(ProductOffersStaticList);
                            },
                        },

                        hooks: {
                            async beforeEach() {
                                await this.productOffersStaticList.waitForVisible();
                            },
                        },
                    }),

                    prepareSuite(ProductOffersSnippetListSuite, {
                        pageObjects: {
                            productOffersSnippetList() {
                                return this.createPageObject(ProductOffersSnippetList);
                            },
                        },
                        hooks: {
                            async beforeEach() {
                                await this.productOffersSnippetList.waitForVisible();
                            },
                        },
                    })
                ),
                'Сниппет Топ 6.': mergeSuites(
                    prepareSuite(ProductOffersSnippetClickOutLinkSuite, {
                        meta: {
                            id: 'm-touch-2886',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedHref: URL_TO_SHOP,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetPriceSuite, {
                        meta: {
                            id: 'm-touch-2888',
                            issue: 'MOBMARKET-12583',
                        },
                        params: {
                            expectedPriceValue: PRICE.value,
                            expectedPriceCurrency: PRICE.currency,
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    }),
                    prepareSuite(ProductOffersSnippetShopRatingSuite, {
                        meta: {
                            id: 'm-touch-2796',
                            issue: 'MOBMARKET-12244',
                        },
                        pageObjects: {
                            offerSnippet() {
                                return this.createPageObject(ProductOfferSnippet);
                            },
                        },
                    })
                ),
            }),
        }),
        makeSuite('Дефолтный оффе', {
            environment: 'kadavr',
            story: prepareSuite(DefaultOfferSuite, {
                pageObjects: {
                    defaultOffer() {
                        return this.createPageObject(DefaultOffer);
                    },
                },
                hooks: {
                    async beforeEach() {
                        const state = mergeState([
                            productWithDefaultOffer,
                            buildProductOffersResultsState({
                                offersCount: 6,
                            }),
                        ]);

                        await this.browser.setState('report', state);
                        await this.browser.yaSetCookie({
                            name: 'iugc',
                            value: '1',
                        });
                        await this.browser.yaOpenPage('touch:product-reviews', phoneProductRoute);
                        // Виджет с ДО загружается лениво.

                        await this.browser.yaSlowlyScroll('[data-zone-name="productReviewsNewReview"]');

                        await this.defaultOffer.waitForVisible();
                    },
                },
            }),
        }),
        makeSuite('Виджет UGC Медиа галереи.', {
            environment: 'kadavr',
            story: prepareSuite(UgcMediaGallerySuite, {
                hooks: {
                    async beforeEach() {
                        const testProductId = 1722193751;
                        const testSlug = 'test-slug';
                        const reviewUser = profiles.ugctest3.uid;
                        const testReview = createUserReviewWithPhotos(testProductId, reviewUser);
                        const testProduct = createProduct({
                            type: 'model',
                            categories: [{
                                id: 123,
                            }],
                            slug: testSlug,
                            deletedId: null,
                            prices: createPriceRange(300, 400, 'RUB'),
                        }, testProductId);

                        await this.browser
                            .setState('report', testProduct)
                            .setState('schema', {
                                gradesOpinions: [testReview],
                                modelOpinions: [testReview],
                            });

                        const testUser = profiles['pan-topinambur'];
                        await this.browser.yaLogin(
                            testUser.login,
                            testUser.password
                        );

                        await this.browser.yaOpenPage('touch:product-reviews', {
                            productId: testProductId,
                            slug: testSlug,
                        });
                        await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                        await this.browser.yaSlowlyScroll(GalleryUgcSlider.root);
                    },
                },
                pageObjects: {
                    galleryUgcSlider() {
                        return this.createPageObject(GalleryUgcSlider);
                    },
                },
                params: {
                    productId: 1722193751,
                    slug: 'test-slug',
                },
            }),
        }),
        prepareSuite(ProductReviewsDegradationSuite, {
            environment: 'kadavr',
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage('touch:product-reviews', productCfg);
                },
            },
            pageObjects: {
                productReviewsPage() {
                    return this.createPageObject(ProductPage);
                },
            },
        })
    ),
});
