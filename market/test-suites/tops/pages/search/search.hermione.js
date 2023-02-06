import {assign} from 'ambar';
import schema from 'js-schema';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import {
    mergeState,
    createOffer,
    createProduct,
    priceSort,
    popularitySort,
    createPrice,
    createPriceRange,
    createFilter,
    createFilterValue,
    createOfferForProduct,
    createRecipe,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';

import nodeConfig from '@self/platform/configs/current/node';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {randomString} from '@self/root/src/helpers/string';

import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {applyCompoundState} from '@self/project/src/spec/hermione/helpers/metakadavr';

// suites
import SearchSnippetClickoutUrls from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/clickoutUrls';
import SearchSnippetOfferRatingSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/withRating';

import ItemCounterCartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/itemCounterCartButton';
import SearchResultsSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults';
import SearchResultsListSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults/list';
import SearchProductReviewsHubSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchProduct/reviewsHub';
import ProductReviewExpandableContentSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview/expandableContent';
import SearchResultsGridSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults/grid';
import SearchProductSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchProduct';
import SearchOfferSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer';
import SearchOfferTileSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOfferTile';
import SearchOptionsPriceSortSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOptions/priceSort';
import SearchOptionsOpinionsSortSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOptions/opinionsSort';
import FilterValuesPreviewVisibilitySuite from '@self/platform/spec/hermione/test-suites/blocks/FilterValuesPreview/visibility';
import FilterValuesPreviewApplySuite from '@self/platform/spec/hermione/test-suites/blocks/FilterValuesPreview/apply';
import FilterValuesPreviewHiddenSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterValuesPreview/hidden';
import FilterPopupRangeSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterPopup/range';
import SearchResultsOpinionsSortSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults/opinionsSort';
import SearchOptionsFiltersSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOptions/filters';
import FilterCompoundSearchSuite from '@self/platform/spec/hermione/test-suites/blocks/FilterCompound/search';
import CreditFilterSearchSuite from '@self/platform/spec/hermione/test-suites/blocks/CreditFilter/search';
import ClarifyingCategoriesSuite from '@self/platform/spec/hermione/test-suites/blocks/ClarifyingCategories';
import SearchResultsInfiniteSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchResults/infinite';
import ConditionTypeSuite from '@self/platform/spec/hermione/test-suites/blocks/ConditionType';
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';
import FooterSuite from '@self/platform/spec/hermione/test-suites/blocks/Footer';
import SearchOfferRussianPostSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/russianPost';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import MetaRobotsSuite from '@self/project/src/spec/hermione/test-suites/blocks/MetaRobots';
import SearchPageDegradationSuite from '@self/platform/spec/hermione/test-suites/tops/pages/search/degradation';
import CartPopupSuite from '@self/platform/spec/hermione/test-suites/blocks/CartPopup';
import CartPopupSearchSpecificSuite from '@self/platform/spec/hermione/test-suites/blocks/CartPopup/searchSpecific';
import growingCashbackIncut from '@self/root/src/spec/hermione/test-suites/touch.blocks/growingCashback/incutSuites';
import searchSnippetUnitInfo from '@self/platform/spec/hermione/test-suites/blocks/SearchSnippet/unitInfo';
import PurchaseListSuite from '@self/root/src/spec/hermione/test-suites/blocks/PurchaseList';
import EstimatedOffersSuite from '@self/platform/spec/hermione/test-suites/tops/pages/search/estimated';

// page-objects
import SearchSnippet from '@self/platform/spec/page-objects/containers/SearchSnippet';
import SearchSnippetClickoutButton from '@self/platform/spec/page-objects/containers/SearchSnippet/ClickoutButton';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import SearchSnippetPrice from '@self/platform/spec/page-objects/containers/SearchSnippet/Price';
import SearchSnippetRating from '@self/platform/spec/page-objects/containers/SearchSnippet/Rating';
import SearchSnippetShopInfo from '@self/platform/spec/page-objects/containers/SearchSnippet/ShopInfo';
import SearchSnippetWarnings from '@self/platform/spec/page-objects/containers/SearchSnippet/Warnings';
import SearchSnippetContainer from '@self/platform/spec/page-objects/SearchSnippetContainer';

import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import SearchResults from '@self/platform/spec/page-objects/SearchResults';
import ReviewContent from '@self/platform/spec/page-objects/ReviewContent';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import Footer from '@self/platform/spec/page-objects/Footer';
import ClarifyingCategories from '@self/platform/spec/page-objects/ClarifyingCategories';
import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';
import FilterValuesPreview from '@self/platform/components/FilterValuesPreview/__pageObject';
import RangeFilter from '@self/platform/components/RangeFilter/__pageObject';
import PageMeta from '@self/platform/spec/page-objects/PageMeta';
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';
import {EnumFilterGenerator} from '@self/root/src/spec/hermione/kadavr-mock/report/filters/enum';

// fixtures
import {
    priceFilter,
    priceFilterValues,
    priceFilterValuesWithCheckedValue,
} from '@self/platform/spec/hermione/test-suites/tops/pages/fixtures/filters';
import {
    searchResultsWithOfferState as cutpriceSearchResultsWithOfferState,
    searchResultsWithProductState as cutpriceSearchResultsWithProductState,
} from '@self/platform/spec/hermione/fixtures/cutprice';
import {searchResultsWithProductState as creditSearchResultsWithProductState} from '@self/platform/spec/hermione/fixtures/credit';
import {russianPostOffer} from '@self/platform/spec/hermione/fixtures/offer';
import {cpaOffer, cpaOfferMock} from '@self/project/src/spec/hermione/fixtures/offer/cpaOffer';
import offerFarma from '@self/root/src/spec/hermione/kadavr-mock/report/offer/farma';

import clarifyingCategoriesMock from './fixtures/clarifyingCategories';
import categoryWithGridViewNavTreeMock from './fixtures/categoryWithGridViewNavTree';
import reportStateWithOpinions from './fixtures/reportStateWithOpinions';
import defaultReview from './fixtures/defaultReview';

// imports
import ageConfirmation from './ageConfirmation';
import banners from './banners';
import automaticallyCalculatedDelivery from './automaticallyCalculatedDelivery';
import drugsDisclaimer from './drugsDisclaimer';
import wishlist from './wishlist';
import compare from './compare';
import preorder from './preorder';
import order60days from './order60days';
import shopSnippetIncut from './spVendors/shopSnippetIncut';
import promoBadge from './spVendors/promoBadge';
import placementTypes from './placementTypes';
import filters from './filters';
import delivery from './delivery';
import expressDelivery from './delivery/express';

const ENCRYPTED_URL = '/redir/encrypted';

const enumFilters = new EnumFilterGenerator({
    filterLabel: 'Производитель',
    values: [
        {value: 'Abibas', checked: true},
        {value: 'Benbok'},
    ],
});


// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница поисковой выдачи.', {
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    regionPopup: () => this.createPageObject(RegionPopup),
                    searchResults: () => this.createPageObject(SearchResults),
                });
            },
        },

        makeSuite('Хаб отзывов.', {
            environment: 'kadavr',
            story: {
                'Выдача.': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                snippet: () => this.createPageObject(SearchSnippet, {
                                    parent: this.snippetContainer,
                                    root: SearchSnippet.listRoot,
                                }),
                                snippetContainer: () => this.createPageObject(SearchSnippetContainer, {
                                    parent: this.searchResults,
                                }),
                            });

                            const productWithReview = createProduct({
                                showUid: randomString(),
                                review: defaultReview,
                                reviews: [defaultReview],
                                showReview: true,
                                categories: [
                                    {
                                        entity: 'category',
                                        id: 91491,
                                        name: 'Мобильные телефоны',
                                        fullName: 'Мобильные телефоны',
                                        slug: 'mobilnye-telefony',
                                        type: 'guru',
                                        isLeaf: true,
                                    },
                                ],
                            }, 100500);

                            const productScrollCount = 7;
                            const productsPerScrollCount = 16;
                            const productsCount = productScrollCount * productsPerScrollCount;

                            const products = [];
                            for (let i = 0; i < productsCount; i++) {
                                products.push(createProduct({
                                    showUid: `${randomString()}_${i}`,
                                    categories: [
                                        {
                                            entity: 'category',
                                            id: 91491,
                                            name: 'Мобильные телефоны',
                                            fullName: 'Мобильные телефоны',
                                            slug: 'mobilnye-telefony',
                                            type: 'guru',
                                            isLeaf: true,
                                        },
                                    ],
                                }, i));
                            }

                            const totalMixin = {
                                data: {
                                    search: {
                                        total: productsCount,
                                        totalModels: productsCount,
                                    },
                                },
                            };

                            const reportState = mergeState([
                                productWithReview,
                                ...products,
                                totalMixin,
                            ]);

                            await this.browser.setState('report', reportState);

                            return this.browser
                                .yaOpenPage('touch:search', routes.catalog.reviewsHub)
                                .yaClosePopup(this.regionPopup);
                        },
                    },
                    prepareSuite(SearchResultsSuite),
                    prepareSuite(SearchResultsListSuite),
                    prepareSuite(SearchProductReviewsHubSuite, {
                        hooks: {
                            async beforeEach() {
                                const productId = await this.snippetContainer.getId();
                                const productSlug = await this.snippetContainer.getSlug();

                                this.params = assign({productId, slug: productSlug}, this.params);
                            },
                        },
                    }),
                    prepareSuite(ProductReviewExpandableContentSuite, {
                        pageObjects: {
                            reviewContent() {
                                return this.createPageObject(ReviewContent);
                            },
                        },
                    })
                ),
            },
        }),

        makeSuite('Выдача.', {
            story: mergeSuites(
                makeSuite('Результаты.', {
                    environment: 'kadavr',
                    story: mergeSuites(
                        createStories({
                            catalogList: {
                                description: 'Категорийная выдача (лист)',
                                route: 'touch:list',
                                routeParams: routes.catalog.list,
                            },
                            catalogTile: {
                                description: 'Категорийная выдача (тайлы)',
                                route: 'touch:list',
                                routeParams: routes.catalog.tile,
                            },
                            search: {
                                description: 'Поисковая выдача',
                                route: 'touch:search',
                                routeParams: routes.search.default,
                            },
                            multisearch: {
                                description: 'Мультипоиск',
                                route: 'touch:multisearch',
                                routeParams: routes.multisearch.default,
                            },
                            listWithoutHid: {
                                description: 'Категорийная выдача без hid',
                                route: 'touch:list',
                                routeParams: routes.catalog.listWithoutHid,
                            },
                        }, ({route, routeParams}) => prepareSuite(SearchResultsSuite, {
                            hooks: {
                                beforeEach() {
                                    const offersCount = 120;
                                    const offers = [];
                                    for (let i = 0; i < offersCount; i++) {
                                        offers.push(createOffer({
                                            cpc: `${randomString()}_${i}`,
                                            urls: {
                                                encrypted: '/redir/',
                                            },
                                        }));
                                    }

                                    const totalMixin = {
                                        data: {
                                            search: {
                                                total: offersCount,
                                                totalOffers: offersCount,
                                            },
                                        },
                                    };

                                    const reportState = mergeState([
                                        ...offers,
                                        totalMixin,
                                    ]);

                                    return this.browser.setState('report', reportState)
                                        .then(() => this.browser.yaOpenPage(route, routeParams))
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                        }))
                    ),
                }),

                makeSuite('Выдача вида List.', {
                    story: mergeSuites(
                        makeSuite('Продуктовый сниппет', {
                            story:
                                prepareSuite(SearchResultsListSuite, {
                                    hooks: {
                                        beforeEach() {
                                            return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                                .yaClosePopup(this.regionPopup);
                                        },
                                    },
                                    pageObjects: {
                                        snippet() {
                                            return this.createPageObject(SearchSnippet, {
                                                root: SearchSnippet.listRoot,
                                                parent: this.searchResults,
                                            });
                                        },
                                    },
                                }),
                        }),
                        makeSuite('Офферный сниппет.', {
                            story: prepareSuite(SearchSnippetClickoutUrls, {
                                meta: {
                                    id: 'm-touch-3435',
                                    issue: 'MARKETFRONT-22542',
                                },
                                params: {
                                    url: ENCRYPTED_URL,
                                },
                                pageObjects: {
                                    snippet() {
                                        return this.createPageObject(SearchSnippet, {
                                            parent: this.searchResults,
                                        });
                                    },
                                    snippetClickoutButton() {
                                        return this.createPageObject(SearchSnippetClickoutButton);
                                    },
                                    snippetShopInfo() {
                                        return this.createPageObject(SearchSnippetShopInfo);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        const offer = createOffer({
                                            cpc: randomString(),
                                            urls: {
                                                encrypted: ENCRYPTED_URL,
                                                decrypted: '/redir/decrypted',
                                                geo: '/redir/geo',
                                                offercard: '/redir/offercard',
                                            },
                                        });
                                        const dataMixin = {
                                            data: {
                                                search: {
                                                    total: 1,
                                                    totalOffers: 1,
                                                },
                                            },
                                        };
                                        const reportState = mergeReportState([offer, dataMixin]);

                                        await this.browser.setState('report', reportState);

                                        return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                            .yaClosePopup(this.regionPopup);
                                    },
                                },
                            }),
                        }),
                        prepareSuite(SearchPageDegradationSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                        })
                    ),
                }),

                makeSuite('Выдача вида Grid.', {
                    environment: 'kadavr',
                    story: mergeSuites(
                        makeSuite('Продуктовый сниппет.', {
                            story: prepareSuite(SearchResultsGridSuite, {
                                hooks: {
                                    async beforeEach() {
                                        const productsCount = 5;
                                        const products = [];

                                        for (let i = 0; i < productsCount; i++) {
                                            products.push(createProduct({
                                                showUid: `${randomString()}_${i}`,
                                                slug: 'test-product',
                                                categories: [
                                                    {
                                                        entity: 'category',
                                                        id: 91491,
                                                        name: 'Мобильные телефоны',
                                                        fullName: 'Мобильные телефоны',
                                                        slug: 'mobilnye-telefony',
                                                        type: 'guru',
                                                        isLeaf: true,
                                                    },
                                                ],
                                            }));
                                        }

                                        const totalMixin = {
                                            data: {
                                                search: {
                                                    total: productsCount,
                                                    totalModels: productsCount,
                                                    totalOffers: productsCount,
                                                    view: 'grid',
                                                },
                                            },
                                        };

                                        const reportState = mergeState([
                                            ...products,
                                            totalMixin,
                                        ]);

                                        await this.browser.setState('report', reportState);

                                        return this.browser.yaOpenPage('touch:list', routes.catalog.phones)
                                            .yaClosePopup(this.regionPopup);
                                    },
                                },
                                pageObjects: {
                                    snippet() {
                                        return this.createPageObject(SearchSnippet, {
                                            parent: this.searchResults,
                                            root: SearchSnippet.gridRoot,
                                        });
                                    },
                                },
                            }),
                        }),
                        makeSuite('Офферный сниппет.', {
                            story: prepareSuite(SearchSnippetClickoutUrls, {
                                meta: {
                                    id: 'm-touch-3435',
                                    issue: 'MARKETFRONT-22542',
                                },
                                params: {
                                    url: ENCRYPTED_URL,
                                },
                                pageObjects: {
                                    snippet() {
                                        return this.createPageObject(SearchSnippet, {
                                            parent: this.searchResults,
                                        });
                                    },
                                    snippetClickoutButton() {
                                        return this.createPageObject(SearchSnippetClickoutButton);
                                    },
                                    snippetShopInfo() {
                                        return this.createPageObject(SearchSnippetShopInfo);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        const offer = createOffer({
                                            cpc: randomString(),
                                            urls: {
                                                encrypted: ENCRYPTED_URL,
                                                decrypted: '/redir/decrypted',
                                                geo: '/redir/geo',
                                                offercard: '/redir/offercard',
                                            },
                                        });
                                        const offer2 = createOffer({
                                            cpc: randomString(),
                                            urls: {
                                                encrypted: ENCRYPTED_URL,
                                                decrypted: '/redir/decrypted',
                                                geo: '/redir/geo',
                                                offercard: '/redir/offercard',
                                            },
                                        });
                                        const dataMixin = {
                                            data: {
                                                search: {
                                                    total: 1,
                                                    totalOffers: 1,
                                                    view: 'grid',
                                                },
                                            },
                                        };
                                        const offers = [offer, offer2];
                                        const reportState = mergeReportState([...offers, dataMixin]);

                                        await this.browser.setState('report', reportState);

                                        return this.browser.yaOpenPage('touch:list', routes.catalog.tile)
                                            .yaClosePopup(this.regionPopup);
                                    },
                                },
                            }),
                        }),
                        prepareSuite(SearchPageDegradationSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                        })
                    ),
                }),

                makeSuite('Сниппеты на выдаче.', {
                    story: mergeSuites(
                        createStories({
                            list: {
                                description: 'Поисковая выдача списком',
                                routeParams: routes.catalog.list,
                                pageObject: SearchSnippet,
                            },
                            tile: {
                                description: 'Поисковая выдача плиткой',
                                routeParams: routes.catalog.tile,
                                pageObject: SearchSnippet,
                            },
                        }, ({routeParams, pageObject}) => prepareSuite(SearchProductSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser.yaOpenPage('touch:list', routeParams)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },

                            pageObjects: {
                                snippet() {
                                    return this.createPageObject(pageObject, {
                                        parent: this.searchResults,
                                    });
                                },
                            },
                        })),
                        createStories({
                            catalog: {
                                description: 'Категорийная выдача',
                                route: 'touch:list',
                                routeParams: routes.catalog.offers,
                                meta: {
                                    id: 'm-touch-2023',
                                    issue: 'MOBMARKET-7809',
                                },
                            },
                            search: {
                                description: 'Поисковая выдача',
                                route: 'touch:search',
                                routeParams: routes.search.krossi,
                                meta: {
                                    id: 'm-touch-2329',
                                    issue: 'MOBMARKET-9420',
                                },
                            },
                        }, ({route, routeParams, meta}) => prepareSuite(SearchOfferSuite, {
                            meta,
                            hooks: {
                                async beforeEach() {
                                    await this.browser.setState('report', createOffer({
                                        urls: {encrypted: 'https://ya.ru'},
                                        cpc: randomString(),
                                    }));

                                    return this.browser.yaOpenPage(route, routeParams)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },

                            pageObjects: {
                                snippet() {
                                    return this.createPageObject(SearchSnippet, {
                                        parent: this.searchResults,
                                    });
                                },
                            },
                        })),
                        prepareSuite(SearchSnippetOfferRatingSuite, {
                            suiteName: 'Листовой сниппет оффера. Рейтинг и отзывы магазина',
                            hooks: {
                                async beforeEach() {
                                    await this.browser.setState('report', createOffer({
                                        cpc: randomString(),
                                        shop: {
                                            id: 1925,
                                            slug: 'tekhnopark',
                                            ratingToShow: 4.364615821,
                                        },
                                        urls: {
                                            encrypted: '/redir/',
                                        },
                                    }));

                                    return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                            pageObjects: {
                                snippetShopInfo() {
                                    return this.createPageObject(SearchSnippetShopInfo);
                                },
                                snippetClickoutButton() {
                                    return this.createPageObject(SearchSnippetClickoutButton);
                                },
                            },
                            params: {
                                shopId: 1925,
                                slug: 'tekhnopark',
                            },
                        }),
                        prepareSuite(SearchOfferRussianPostSuite, {
                            meta: {
                                id: 'm-touch-3108',
                                issue: 'MARKETFRONT-6448',
                            },
                            hooks: {
                                async beforeEach() {
                                    await this.browser.setState('report', russianPostOffer);

                                    return this.browser.yaOpenPage('touch:list', routes.catalog.list)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                            pageObjects: {
                                snippetDelivery() {
                                    return this.createPageObject(SearchSnippetDelivery, {
                                        parent: this.searchResults,
                                    });
                                },
                            },
                        }),
                        prepareSuite(SearchSnippetOfferRatingSuite, {
                            suiteName: 'Тайловый сниппет оффера. Рейтинг и отзывы магазина',
                            hooks: {
                                async beforeEach() {
                                    const dataMixin = {
                                        data: {
                                            search: {
                                                total: 1,
                                                totalOffers: 1,
                                                view: 'grid',
                                            },
                                        },
                                    };
                                    const offer = createOffer({
                                        cpc: randomString(),
                                        shop: {
                                            id: 1925,
                                            slug: 'tekhnopark',
                                            ratingToShow: 4.364615821,
                                        },
                                        urls: {
                                            encrypted: '/redir/',
                                        },
                                    });
                                    const reportState = mergeReportState([offer, dataMixin]);

                                    await this.browser.setState('report', reportState);

                                    return this.browser.yaOpenPage('touch:list', routes.catalog.tile)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                            pageObjects: {
                                snippetShopInfo() {
                                    return this.createPageObject(SearchSnippetShopInfo);
                                },
                                snippetClickoutButton() {
                                    return this.createPageObject(SearchSnippetClickoutButton);
                                },
                            },
                            params: {
                                shopId: 1925,
                                slug: 'tekhnopark',
                            },
                        })

                    ),
                }),

                makeSuite('Сниппеты на выдаче (кадавр).', {
                    environment: 'kadavr',
                    story: mergeSuites(
                        prepareSuite(SearchOfferTileSuite, {
                            hooks: {
                                async beforeEach() {
                                    const offersCount = 10;
                                    const searchResults = [];
                                    for (let i = 0; i < offersCount; i++) {
                                        searchResults.push(createOffer({
                                            cpc: `${randomString()}_${i}`,
                                            urls: {
                                                encrypted: '/redir/',
                                            },
                                        }));
                                    }
                                    // Добавляем хотя бы 1 продукт, чтобы выдача осталась гридовой (если будут только
                                    // офферы, выдача становится листовой независимо от параметров категории)
                                    searchResults.push(createProduct({
                                        showUid: randomString(),
                                        categories: [
                                            {
                                                entity: 'category',
                                                id: 91491,
                                                name: 'Мобильные телефоны',
                                                fullName: 'Мобильные телефоны',
                                                slug: 'mobilnye-telefony',
                                                type: 'guru',
                                                isLeaf: true,
                                            },
                                        ],
                                        slug: 'test-product',
                                    }));

                                    const totalMixin = {
                                        data: {
                                            search: {
                                                total: offersCount,
                                                totalOffers: offersCount,
                                                totalModels: 1,
                                            },
                                        },
                                    };

                                    const reportState = mergeState([
                                        ...searchResults,
                                        totalMixin,
                                    ]);

                                    await this.browser.setState('report', reportState);
                                    await this.browser.setState('Cataloger.tree', categoryWithGridViewNavTreeMock);

                                    return this.browser.yaOpenPage('touch:list', routes.catalog.tile)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                            pageObjects: {
                                snippet() {
                                    return this.createPageObject(SearchSnippet, {
                                        parent: this.searchResults,
                                    });
                                },
                            },
                        })
                    ),
                }),

                makeSuite('Сортировка по цене.', {
                    environment: 'kadavr',
                    story: createStories({
                        search: {
                            description: 'Поисковая выдача',
                            route: 'touch:search',
                            routeParams: routes.search.default,
                        },
                        catalog: {
                            description: 'Каталог',
                            route: 'touch:list',
                            routeParams: routes.catalog.list,
                        },
                        reviewsHub: {
                            description: 'Хаб отзывов',
                            route: 'touch:search',
                            routeParams: routes.catalog.reviewsHub,
                        },
                        multisearch: {
                            description: 'Мультипоиск',
                            route: 'touch:multisearch',
                            routeParams: routes.multisearch.default,
                        },
                    }, ({route, routeParams}) => prepareSuite(SearchOptionsPriceSortSuite, {
                        hooks: {
                            beforeEach() {
                                const minPrice = 250;

                                const product = createProduct({
                                    showUid: randomString(),
                                    prices: createPriceRange(600, 1000, 'RUB'),
                                    slug: 'test-product',
                                    categories: [
                                        {
                                            entity: 'category',
                                            id: 91491,
                                            name: 'Мобильные телефоны',
                                            fullName: 'Мобильные телефоны',
                                            slug: 'mobilnye-telefony',
                                            type: 'guru',
                                            isLeaf: true,
                                        },
                                    ],
                                });
                                const offer = createOffer({
                                    cpc: randomString(),
                                    prices: createPrice(minPrice, 'RUB', minPrice, false, {
                                        /**
                                         * Из-за бага в json-schema-faker явно указываем отсутствие скидки
                                         */
                                        discount: null,
                                    }),
                                    shop: {
                                        id: 1,
                                        slug: 'shop',
                                        name: 'shop',
                                    },
                                    urls: {
                                        encrypted: '/redir/',
                                    },
                                });

                                const reportState = mergeState([
                                    product,
                                    offer,
                                    popularitySort,
                                    priceSort,
                                ]);

                                this.params.price = `${minPrice}₽`;

                                return this.browser.setState('report', reportState)
                                    .then(() => this.browser.yaOpenPage(route, routeParams))
                                    .yaClosePopup(this.regionPopup);
                            },
                        },
                        pageObjects: {
                            searchOptions() {
                                return this.createPageObject(SearchOptions);
                            },
                            snippetPrice() {
                                return this.createPageObject(SearchSnippetPrice);
                            },
                        },
                        params: {
                            sortValue: 'aprice',
                        },
                        meta: {
                            id: 'm-touch-2028',
                            issue: 'MOBMARKET-7814',
                        },
                    })),
                }),

                prepareSuite(PurchaseListSuite, {
                    params: {
                        // ₽ без пробела, так как верстке нет nbsp
                        expectedPrice: 'от 107₽',
                    },
                    meta: {
                        environment: 'kadavr',
                        id: 'marketfront-5767',
                        issue: 'MARKETFRONT-81473',
                    },
                    hooks: {
                        async beforeEach() {
                            const offer = createOffer(offerFarma, offerFarma.wareId);

                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                    },
                                },
                            };

                            const state = mergeState([
                                offer,
                                dataMixin,
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('touch:search', routes.search.medicinePurchaseList);
                        },
                    },
                    pageObjects: {
                        snippetPrice() {
                            return this.createPageObject(SearchSnippetPrice);
                        },
                    },
                }),

                makeSuite('Сортировка по отзывам.', {
                    story: createStories({
                        catalog: {
                            description: 'Каталог',
                            route: 'touch:list',
                            routeParams: routes.catalog.list,
                        },
                        reviewsHubCategory: {
                            description: 'Хаб отзывов',
                            route: 'touch:search',
                            routeParams: routes.catalog.reviewsHubCategory,
                        },
                    }, ({route, routeParams}) => prepareSuite(SearchOptionsOpinionsSortSuite, {
                        hooks: {
                            async beforeEach() {
                                this.params.opinions = 3;

                                await this.browser.setState('schema', reportStateWithOpinions.opinions);

                                return this.browser.setState('report', reportStateWithOpinions.reportState)
                                    .then(() => this.browser.yaOpenPage(route, routeParams))
                                    .yaClosePopup(this.regionPopup);
                            },
                        },
                        pageObjects: {
                            searchOptions() {
                                return this.createPageObject(SearchOptions);
                            },
                            snippetRating() {
                                return this.createPageObject(SearchSnippetRating);
                            },
                        },
                        meta: {
                            id: 'm-touch-1835',
                            issue: 'MOBMARKET-6739',
                        },
                    })),
                }),
                prepareSuite(FilterValuesPreviewVisibilitySuite, {
                    meta: {
                        environment: 'kadavr',
                        id: 'm-touch-3073',
                        issue: 'MARKETFRONT-5265',
                    },
                    hooks: {
                        async beforeEach() {
                            const offersCount = 1;
                            const offer = createOffer({cpc: randomString()});
                            const glPriceFilter = createFilter(priceFilter, 'glprice');
                            const glPriceFilterValues = priceFilterValues.map(filter => (
                                createFilterValue(filter, 'glprice', filter.id)
                            ));

                            const totalMixin = {
                                data: {
                                    search: {
                                        total: offersCount,
                                        totalOffers: offersCount,
                                    },
                                },
                            };

                            const reportState = mergeState([
                                offer,
                                totalMixin,
                                glPriceFilter,
                                ...glPriceFilterValues,
                            ]);

                            await this.browser.setState('report', reportState);

                            return this.browser.yaOpenPage('touch:list-filters', routes.listFilters.catalog);
                        },
                    },
                    pageObjects: {
                        filterValuesPreview() {
                            return this.createPageObject(FilterValuesPreview);
                        },
                    },
                }),
                prepareSuite(FilterValuesPreviewApplySuite, {
                    meta: {
                        environment: 'kadavr',
                        id: 'm-touch-3074',
                        issue: 'MARKETFRONT-5266',
                    },
                    hooks: {
                        async beforeEach() {
                            const offersCount = 1;
                            const offer = createOffer({cpc: randomString()});
                            const glPriceFilter = createFilter(priceFilter, 'glprice');
                            const glPriceFilterValues = priceFilterValues.map(filter => (
                                createFilterValue(filter, 'glprice', filter.id)
                            ));

                            const totalMixin = {
                                data: {
                                    search: {
                                        total: offersCount,
                                        totalOffers: offersCount,
                                    },
                                },
                            };

                            const reportState = mergeState([
                                offer,
                                totalMixin,
                                glPriceFilter,
                                ...glPriceFilterValues,
                            ]);

                            await this.browser.setState('report', reportState);

                            return this.browser.yaOpenPage('touch:list-filters', routes.listFilters.catalog);
                        },
                    },
                    params: {
                        filterValueId: priceFilter.presetValues[0].id,
                        filterValueText: '100 – 1 000',
                    },
                    pageObjects: {
                        filterCompound() {
                            return this.createPageObject(FilterCompound, {
                                root: '[data-autotest-id="glprice"]',
                            });
                        },

                        filterValuesPreview() {
                            return this.createPageObject(FilterValuesPreview, {
                                parent: this.filterCompound,
                            });
                        },
                    },
                }),
                prepareSuite(FilterValuesPreviewHiddenSuite, {
                    meta: {
                        environment: 'kadavr',
                        id: 'm-touch-3075',
                        issue: 'MARKETFRONT-5267',
                    },
                    hooks: {
                        async beforeEach() {
                            const offersCount = 1;
                            const offer = createOffer({cpc: randomString()});

                            const glPriceFilter = createFilter(priceFilter, 'glprice');
                            const glPriceFilterValues = priceFilterValuesWithCheckedValue.map(filter => (
                                createFilterValue(filter, 'glprice', filter.id)
                            ));

                            const totalMixin = {
                                data: {
                                    search: {
                                        total: offersCount,
                                        totalOffers: offersCount,
                                    },
                                },
                            };

                            const reportState = mergeState([
                                offer,
                                totalMixin,
                                glPriceFilter,
                                ...glPriceFilterValues,
                            ]);

                            await this.browser.setState('report', reportState);

                            return this.browser.yaOpenPage('touch:list-filters', routes.listFilters.catalog);
                        },
                    },
                    pageObjects: {
                        filterCompound() {
                            return this.createPageObject(FilterCompound, {
                                root: '[data-autotest-id="glprice"]',
                            });
                        },

                        filterValuesPreview() {
                            return this.createPageObject(FilterValuesPreview, {
                                parent: this.filterCompound,
                            });
                        },
                    },
                }),
                prepareSuite(FilterPopupRangeSuite, {
                    meta: {
                        environment: 'kadavr',
                        id: 'm-touch-3076',
                        issue: 'MARKETFRONT-5268',
                    },
                    params: {
                        valueFrom: '100',
                        valueTo: '1000',
                        expectedValue: '100 – 1 000',
                    },
                    hooks: {
                        async beforeEach() {
                            const offersCount = 1;
                            const offer = createOffer({cpc: randomString()});

                            const glPriceFilter = createFilter(priceFilter, 'glprice');
                            const glPriceFilterValues = priceFilterValues.map(filter => (
                                createFilterValue(filter, 'glprice', filter.id)
                            ));

                            const totalMixin = {
                                data: {
                                    search: {
                                        total: offersCount,
                                        totalOffers: offersCount,
                                    },
                                },
                            };

                            const reportState = mergeState([
                                offer,
                                totalMixin,
                                glPriceFilter,
                                ...glPriceFilterValues,
                            ]);

                            await this.browser.setState('report', reportState);

                            return this.browser.yaOpenPage('touch:list-filters', routes.listFilters.catalog);
                        },
                    },
                    pageObjects: {
                        filterCompound() {
                            return this.createPageObject(FilterCompound, {
                                root: '[data-autotest-id="glprice"]',
                            });
                        },

                        filters() {
                            return this.createPageObject(Filters);
                        },

                        filterPopup() {
                            return this.createPageObject(FilterPopup);
                        },

                        rangeFilter() {
                            return this.createPageObject(RangeFilter);
                        },
                    },
                }),
                prepareSuite(SearchResultsOpinionsSortSuite, {
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('schema', reportStateWithOpinions.opinions);

                            return this.browser.setState('report', reportStateWithOpinions.reportState)
                                .then(() => this.browser.yaOpenPage(
                                    'touch:search',
                                    assign(routes.catalog.reviewsHubCategory, {how: 'opinions'})
                                ))
                                .yaClosePopup(this.regionPopup);
                        },
                    },
                    pageObjects: {
                        searchOptions() {
                            return this.createPageObject(SearchOptions);
                        },
                        searchResults() {
                            return this.createPageObject(SearchResults);
                        },
                    },
                }),

                makeSuite('Фильтры на серче.', {
                    story: mergeSuites(
                        createStories({
                            search: {
                                description: 'Поисковая выдача',
                                route: 'touch:search',
                                routeParams: routes.search.default,
                                params: {
                                    pageRoot: 'search',
                                },
                            },
                            catalog: {
                                description: 'Каталог',
                                route: 'touch:list',
                                routeParams: routes.catalog.list,
                                params: {
                                    pageRoot: 'catalog--bloki-pitaniia-dlia-kompiuterov/55313',
                                },
                            },
                            reviewsHub: {
                                description: 'Хаб отзывов',
                                route: 'touch:search',
                                routeParams: routes.catalog.reviewsHub,
                                params: {
                                    pageRoot: 'search',
                                },
                            },
                            multisearch: {
                                description: 'Мультипоиск',
                                route: 'touch:multisearch',
                                routeParams: routes.multisearch.default,
                                params: {
                                    pageRoot: 'multisearch',
                                },
                            },
                        }, ({route, routeParams, params}) => prepareSuite(SearchOptionsFiltersSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser.yaOpenPage(route, routeParams)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },
                            pageObjects: {
                                searchOptions() {
                                    return this.createPageObject(SearchOptions);
                                },
                            },
                            meta: {
                                id: 'm-touch-2105',
                                issue: 'MOBMARKET-8065',
                            },
                            params,
                        })),

                        prepareSuite(FilterCompoundSearchSuite, {
                            hooks: {
                                beforeEach() {
                                    return this.browser.yaOpenPage('touch:search-filters', routes.searchFilters.paddle)
                                        .yaClosePopup(this.regionPopup);
                                },
                            },

                            pageObjects: {
                                filterCompound() {
                                    return this.createPageObject(
                                        FilterCompound,
                                        {root: '[data-autotest-id=\'7893318\']'}
                                    );
                                },
                            },
                        }),

                        makeSuite('Фильтр «Покупка в кредит»', {
                            environment: 'kadavr',
                            story: prepareSuite(CreditFilterSearchSuite, {
                                hooks: {
                                    async beforeEach() {
                                        await this.browser.setState(
                                            'report',
                                            creditSearchResultsWithProductState
                                        );

                                        await this.browser.yaOpenPage(
                                            'touch:search-filters',
                                            routes.searchFilters.paddle
                                        );
                                        await this.browser.yaClosePopup(this.regionPopup);
                                    },
                                },
                                pageObjects: {
                                    filterCompound() {
                                        return this.createPageObject(FilterCompound, {
                                            root: '[data-autotest-id="credit-type"]',
                                        });
                                    },
                                    snippet() {
                                        return this.createPageObject(SearchSnippet);
                                    },
                                    selectFilter() {
                                        return this.createPageObject(SelectFilter);
                                    },
                                    filterPopup() {
                                        return this.createPageObject(FilterPopup);
                                    },
                                    filters() {
                                        return this.createPageObject(Filters);
                                    },
                                },
                            }),
                        })
                    ),
                }),
                makeSuite('CPA-сниппет.', {
                    meta: {
                        issue: 'MARKETFRONT-13236',
                    },
                    environment: 'kadavr',
                    story: createStories([
                        {
                            description: 'Гридовая выдача',
                            queryParams: routes.catalog.tile,
                        },
                        {
                            description: 'Листовая выдача',
                            queryParams: routes.catalog.list,
                        },
                    ], ({queryParams}) => mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('Carter.items', []);
                                await this.browser.setState('report', cpaOffer);

                                return this.browser.yaOpenPage('touch:list', queryParams)
                                    .yaClosePopup(this.regionPopup);
                            },
                        },

                        prepareSuite(CartButtonSuite, {
                            pageObjects: {
                                cartButton() {
                                    return this.createPageObject(SearchSnippetCartButton);
                                },
                            },
                        }),
                        prepareSuite(CartButtonCounterSuite, {
                            pageObjects: {
                                cartButton() {
                                    return this.createPageObject(SearchSnippetCartButton);
                                },
                            },
                        }),
                        prepareSuite(ItemCounterCartButtonSuite, {
                            meta: {
                                id: 'm-touch-3430',
                            },
                            params: {
                                counterStep: cpaOfferMock.bundleSettings.quantityLimit.step,
                                offerId: cpaOfferMock.wareId,
                            },
                            pageObjects: {
                                snippetCartButton() {
                                    return this.createPageObject(SearchSnippetCartButton);
                                },
                                snippetCounterCartButton() {
                                    return this.createPageObject(SearchSnippetCartButton, {
                                        root: SearchSnippetCartButton.counter,
                                    });
                                },
                            },
                        })
                    )),
                }),

                makeSuite('Блок уточнения.', {
                    environment: 'kadavr',
                    story: createStories({
                        reviewsHub: {
                            description: 'Хаб отзывов',
                            route: 'touch:search',
                            routeParams: routes.search.reviewsHubWithClarifyingCategories,
                        },
                        multisearch: {
                            description: 'Мультипоиск',
                            route: 'touch:multisearch',
                            routeParams: routes.multisearch.withClarifyingCategories,
                        },
                    }, ({route, routeParams}) => prepareSuite(ClarifyingCategoriesSuite, {
                        hooks: {
                            beforeEach() {
                                return this.browser.setState('report', clarifyingCategoriesMock)
                                    .then(() => this.browser.yaOpenPage(route, routeParams))
                                    .then(() => this.browser.yaClosePopup(this.regionPopup));
                            },
                        },

                        pageObjects: {
                            clarifyingCategories() {
                                return this.createPageObject(ClarifyingCategories);
                            },
                        },
                    })),
                }),

                makeSuite('Бесконечная выдача', {
                    environment: 'kadavr',
                    story: mergeSuites(
                        prepareSuite(SearchResultsInfiniteSuite, {
                            pageObjects: {
                                searchResults() {
                                    return this.createPageObject(SearchResults);
                                },
                            },
                            hooks: {
                                async beforeEach() {
                                    const productsCount = 16 * 7; // 7 страниц по 16 офферов
                                    const products = [];

                                    for (let i = 0; i < productsCount; i++) {
                                        products.push(createProduct({
                                            showUid: `${randomString()}_${i}`,
                                            slug: 'test-product',
                                            categories: [
                                                {
                                                    entity: 'category',
                                                    id: 91491,
                                                    name: 'Мобильные телефоны',
                                                    fullName: 'Мобильные телефоны',
                                                    slug: 'mobilnye-telefony',
                                                    type: 'guru',
                                                    isLeaf: true,
                                                },
                                            ],
                                        }));
                                    }

                                    const totalMixin = {
                                        data: {
                                            search: {
                                                total: productsCount,
                                                totalOffers: productsCount,
                                            },
                                            sorts: [{text: 'по популярности'}],
                                        },
                                    };

                                    const reportState = mergeState([
                                        ...products,
                                        totalMixin,
                                    ]);

                                    await this.browser.setState('report', reportState);
                                    await this.browser.yaOpenPage('touch:search', routes.search.default);
                                },
                            },
                        })
                    ),
                }),

                makeSuite('Уценённые товары', {
                    environment: 'kadavr',
                    story: mergeSuites(
                        makeSuite('Лейбл «Уценённый»', {
                            story: createStories({
                                catalogListWithOffers: {
                                    description: 'Категорийная офферная выдача (лист)',
                                    route: 'touch:list',
                                    routeParams: routes.catalog.list,
                                    reportState: cutpriceSearchResultsWithOfferState,
                                    meta: {
                                        id: 'm-touch-2790',
                                        issue: 'MOBMARKET-12238',
                                    },
                                },
                                catalogTileWithOffers: {
                                    description: 'Категорийная офферная выдача (тайлы)',
                                    route: 'touch:list',
                                    routeParams: routes.catalog.tile,
                                    reportState: cutpriceSearchResultsWithOfferState,
                                    meta: {
                                        id: 'm-touch-2791',
                                        issue: 'MOBMARKET-12239',
                                    },
                                },
                                catalogListWithProducts: {
                                    description: 'Категорийная модельная выдача (лист)',
                                    route: 'touch:list',
                                    routeParams: routes.catalog.list,
                                    reportState: cutpriceSearchResultsWithProductState,
                                    meta: {
                                        id: 'm-touch-2790',
                                        issue: 'MOBMARKET-12238',
                                    },
                                },
                                catalogTileWithProducts: {
                                    description: 'Категорийная модельная выдача (тайлы)',
                                    route: 'touch:list',
                                    routeParams: routes.catalog.tile,
                                    reportState: cutpriceSearchResultsWithProductState,
                                    meta: {
                                        id: 'm-touch-2791',
                                        issue: 'MOBMARKET-12239',
                                    },
                                },
                            }, ({meta, route, routeParams, reportState}) => prepareSuite(ConditionTypeSuite, {
                                meta,
                                hooks: {
                                    async beforeEach() {
                                        await this.browser.setState('report', reportState);
                                        this.params.expectedConditionType = 'Уцененный товар';

                                        return this.browser.yaOpenPage(route, routeParams);
                                    },
                                },
                                pageObjects: {
                                    conditionType() {
                                        return this.createPageObject(SearchSnippetWarnings);
                                    },
                                },
                            })),
                        }),
                        makeSuite('Кнопка перехода в магазин', {
                            story: prepareSuite(MetricaClickSuite, {
                                meta: {
                                    issue: 'MOBMARKET-12247',
                                    id: 'm-touch-2799',
                                },
                                params: {
                                    counterId: nodeConfig.yaMetrika.market.id,
                                    expectedGoalName: 'search-page_search-results_offer-snippet_' +
                                    'search-result-snippet-tap',
                                    payloadSchema: schema({
                                        isCutPrice: true,
                                    }),
                                },
                                pageObjects: {
                                    offerSnippet() {
                                        return this.createPageObject(SearchSnippet);
                                    },
                                },
                                hooks: {
                                    async beforeEach() {
                                        await this.browser.setState('report', cutpriceSearchResultsWithOfferState);

                                        await this.browser.yaOpenPage('touch:list', routes.catalog.list);

                                        this.params.selector = await this.offerSnippet.getSelector(
                                            this.offerSnippet.clickOutButton
                                        );
                                    },
                                },
                            }),
                        })
                    ),
                }),
                makeSuite('Кнопка добавления в корзину', {
                    story: createStories({
                        offer: {
                            description: 'Сниппет офера',
                            createReportMock:
                                    offerParams => createOffer({...cpaOfferMock, ...offerParams}, cpaOfferMock.wareId),
                        },
                        product: {
                            description: 'Сниппет продукта',
                            createReportMock:
                                    offerParams => createOfferForProduct({...cpaOfferMock, ...offerParams}, 1),
                        },

                    },
                    ({createReportMock, pageObject}) => prepareSuite(CartPopupSuite, {
                        meta: {
                            environment: 'kadavr',
                            id: 'm-touch-3693',
                            issue: 'MARKETFRONT-52899',
                        },
                        pageObjects: {
                            snippet() {
                                return this.createPageObject(pageObject);
                            },

                            cartButton() {
                                return this.createPageObject(SearchSnippetCartButton);
                            },
                        },
                        params: {
                            createReportMock,
                            pageId: 'touch:list',
                            routeParams: routes.catalog.list,
                        },

                    })),
                }),
                prepareSuite(CartPopupSearchSpecificSuite, {
                    meta: {
                        environment: 'kadavr',
                        id: 'm-touch-3694',
                        issue: 'MARKETFRONT-52899',
                    },
                    hooks: {
                        async beforeEach() {
                            await this.browser.setState('Carter.items', [{
                                offerId: 1234,
                                id: '12345456',
                                creationTime: Date.now(),
                                label: 'g06judyquh9',
                                objType: 'OFFER',
                                objId: 1234,
                                count: 1,
                            }]);
                            await this.browser.setState('report', createOfferForProduct(cpaOfferMock, 1));

                            return this.browser.yaOpenPage('touch:list', routes.catalog.list);
                        },
                    },
                    pageObjects: {
                        cartButton() {
                            return this.createPageObject(SearchSnippetCartButton);
                        },
                    },
                    params: {
                        pageId: 'touch:list',
                        routeParams: routes.catalog.list,
                    },
                })
            ),
        }),

        makeSuite('Футер страницы.', {
            story: createStories({
                search: {
                    description: 'Поисковая выдача',
                    route: 'touch:search',
                    routeParams: routes.search.default,
                },
                catalog: {
                    description: 'Каталог',
                    route: 'touch:list',
                    routeParams: routes.catalog.list,
                },
                reviewsHub: {
                    description: 'Хаб отзывов',
                    route: 'touch:search',
                    routeParams: routes.catalog.reviewsHub,
                },
                multisearch: {
                    description: 'Мультипоиск',
                    route: 'touch:multisearch',
                    routeParams: routes.multisearch.withClarifyingCategories,
                },
            }, ({route, routeParams}) => prepareSuite(FooterSuite, {
                meta: {
                    id: 'm-touch-1126',
                    issue: 'MOBMARKET-4798',
                },
                hooks: {
                    beforeEach() {
                        return this.browser
                            .yaOpenPage(route, routeParams)
                            .yaClosePopup(this.regionPopup);
                    },
                },
                pageObjects: {
                    footer() {
                        return this.createPageObject(Footer);
                    },
                },
            })),
        }),

        ageConfirmation,

        banners,

        automaticallyCalculatedDelivery,

        drugsDisclaimer,

        wishlist,

        compare,

        preorder,

        order60days,

        shopSnippetIncut,

        promoBadge,

        placementTypes,

        filters,

        delivery,

        expressDelivery,

        makeSuite('Мета разметка.', {
            environment: 'kadavr',
            story: mergeSuites(
                makeSuite('Robots', {
                    story: createStories({
                        notEmptyResultWithoutText: {
                            description: 'Не пустая выдача без текста',
                            meta: {
                                id: 'm-touch-3753',
                                issue: 'MARKETFRONT-54003',
                            },
                            routeParams: {
                                nid: 57390,
                                slug: 'zimnie-muzhskie-kurtki',
                                page: 2,
                            },
                            compoundMock: {
                                'Cataloger.tree': makeCatalogerTree('Мужские куртки', 7812186, 57390, {categoryType: 'guru'}),
                                'report': mergeReportState([
                                    createProduct({slug: 'product'}, '1'),
                                    {data: {search: {total: 6}}},
                                ]),
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: '',
                            },
                        },
                        notEmptyResultWithText: {
                            description: 'Не пустая выдача с текстом',
                            meta: {
                                id: 'm-touch-3754',
                                issue: 'MARKETFRONT-54003',
                            },
                            routeParams: {
                                nid: 57390,
                                slug: 'zimnie-muzhskie-kurtki',
                                test: 'test',
                                page: 2,
                            },
                            compoundMock: {
                                'Cataloger.tree': makeCatalogerTree('Мужские куртки', 7812186, 57390, {categoryType: 'guru'}),
                                'report': mergeReportState([
                                    createProduct({slug: 'product'}, '1'),
                                    {data: {search: {total: 6}}},
                                ]),
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: '',
                            },
                        },
                        emptyResultWithoutText: {
                            description: 'пустая выдача без текста',
                            meta: {
                                id: 'm-touch-3756',
                                issue: 'MARKETFRONT-54003',
                            },
                            routeParams: {
                                nid: 54726,
                                slug: 'mobilnye-telefony',
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: 'noindex, nofollow',
                            },
                        },
                        emptyResultWithText: {
                            description: 'пустая выдача с текстом',
                            meta: {
                                id: 'm-touch-3755',
                                issue: 'MARKETFRONT-54003',
                            },
                            routeParams: {
                                nid: 54727,
                                slug: 'mobilnye-telefony',
                                text: 'test',
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: 'noindex, nofollow',
                            },
                        },
                        notEmptySearchWithGlFilterAndWithRecipe: {
                            description: 'Не пустая выдача с glfilter и поисковым рецептом',
                            meta: {
                                id: 'marketfront-5604',
                                issue: 'MARKETFRONT-72028',
                            },
                            routeParams: {
                                nid: 57390,
                                slug: 'zimnie-muzhskie-kurtki',
                                glfilter: [enumFilters.getGlFilterQueryString()],
                            },
                            compoundMock: {
                                'Cataloger.tree': makeCatalogerTree('Мужские куртки', 7812186, 57390, {categoryType: 'guru'}),
                                'report': mergeReportState([
                                    createProduct({slug: 'product'}, '1'),
                                    enumFilters.generateKadavrFilterState(),
                                    createRecipe({
                                        header: 'Мужские куртки Abibas',
                                        name: 'Мужские куртки Abibas',
                                        slug: 'muzhskie-kurtki-abibas',
                                        shortName: 'Мужские куртки Abibas',
                                        category: {
                                            id: 7812186,
                                            navigationId: 57390,
                                        },
                                        filters: [enumFilters.getRawFilterWithCheckedValues()],
                                    }, '1'),
                                    {data: {search: {total: 1}}},
                                ]),
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: '',
                            },
                        },
                        notEmptySearchWithGlFilterAndWithoutRecipe: {
                            description: 'Не пустая выдача с glfilter но без поискового рецепта',
                            meta: {
                                id: 'marketfront-5605',
                                issue: 'MARKETFRONT-72028',
                            },
                            routeParams: {
                                nid: 57390,
                                slug: 'zimnie-muzhskie-kurtki',
                                glfilter: [enumFilters.getGlFilterQueryString()],
                            },
                            compoundMock: {
                                'Cataloger.tree': makeCatalogerTree('Мужские куртки', 7812186, 57390, {categoryType: 'guru'}),
                                'report': mergeReportState([
                                    createProduct({slug: 'product'}, '1'),
                                    enumFilters.generateKadavrFilterState(),
                                    {data: {search: {total: 1}}},
                                ]),
                            },
                            testParams: {
                                robotsContent: '',
                                yandexBot: '',
                                googleBot: 'noindex, nofollow',
                            },
                        },
                    }, ({testParams, routeParams, compoundMock, meta}) => prepareSuite(MetaRobotsSuite, {
                        meta,
                        hooks: {
                            async beforeEach() {
                                if (compoundMock) {
                                    await applyCompoundState(this.browser, compoundMock);
                                } else {
                                    await this.browser.setState(
                                        'report',
                                        {data: {
                                            search: {
                                                total: 0,
                                                totalOffers: 0,
                                                totalOffersBeforeFilters: 0,
                                                filters: ['glprice', 'included-in-price'],
                                                results: [],
                                            },
                                        }}
                                    );
                                }

                                return this.browser.yaOpenPage(
                                    'touch:list', assign({_mod: 'robot'}, routeParams)
                                );
                            },
                        },
                        pageObjects: {
                            pageMeta() {
                                return this.createPageObject(PageMeta);
                            },
                        },
                        params: testParams,
                    })),
                })
            ),
        }),

        growingCashbackIncut,
        searchSnippetUnitInfo,
        EstimatedOffersSuite
    ),
});
