import _ from 'lodash';
import {mergeSuites, prepareSuite, makeSuite} from 'ginny';
import {
    createEntityPicture,
    createOffer,
    createProduct,
    createRecipe,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {createUser} from '@yandex-market/kadavr/mocks/PersQa/helpers';

import {makeCatalogerTree, createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
import cpaOfferKadavrMock from '@self/project/src/spec/hermione/fixtures/product/productWithCPADO';
import COOKIE_NAME from '@self/root/src/constants/cookie';

// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {shopFeedbackFormId} from '@self/platform/spec/hermione/configs/forms';

// suites
import LogoCarouselSuite from '@self/platform/spec/hermione/test-suites/blocks/LogoCarousel';
import ItemCounterCartButton from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import SnippetCell2CpcTrackSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-cell2/cpcTrack';
import SnippetCell2PreviewImageSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-cell2/preview-image';
import SnippetCard2PreviewImageSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-card2/preview-image';
import DeliveryTextSuite from '@self/platform/spec/hermione/test-suites/blocks/n-delivery/__text';
import SnippetCard2DescSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-card2/__desc';
import growingCashbackIncut from '@self/root/src/spec/hermione/test-suites/desktop.blocks/growingCashback/incutSuites';

import FilterPanelDropdownLocalOffersFirstSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-filter-panel-dropdown/__filter_local-offers-first';
import PopularRecipesSearchSuite from '@self/platform/spec/hermione/test-suites/blocks/n-popular-recipes/_mode/search';
import ProductReviewSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductReview';
import ReviewSnippetSuite from '@self/platform/spec/hermione/test-suites/blocks/ReviewSnippet';
import PopupComplainSuite from '@self/platform/spec/hermione/test-suites/blocks/PopupComplain/shopAndOffer';
import ShopInfoDrugsDisclaimerSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/drugsDisclaimer';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import RetailShopsIncutSuite from '@self/root/src/spec/hermione/test-suites/desktop.blocks/retailShopsIncut';

// page-objects
// eslint-disable-next-line max-len
import DeliveryInfo from '@self/project/src/components/Search/Snippet/Offer/common/DeliveryInfo/components/DeliveryInfoContent/__pageObject';
import SearchResults from '@self/platform/widgets/content/search/Results/__pageObject';
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import SearchSerpResults from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import SnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
import ReviewSnippet from '@self/platform/components/Search/Snippet/Review/__pageObject';
import ProductReview from '@self/platform/spec/page-objects/components/ProductReview';
import PopularRecipes
    from '@self/root/src/widgets/content/PopularRecipes/components/PopularRecipes/__pageObject/index.desktop';
import FilterPanelDropdown from '@self/platform/widgets/content/SortPanel/__pageObject';
import UIKitCheckbox from '@self/root/src/uikit/components/Checkbox/__pageObject';
import SrcScrollBox from '@self/root/src/components/ScrollBox/__pageObject';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import ComplainPopup from '@self/platform/spec/page-objects/components/ComplainPopup';
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import PopularBrandsCarousel from '@self/root/src/widgets/content/search/PopularBrandsCarousel/__pageObject';

// fixtures
import reviewsHubMock from '@self/platform/spec/hermione/fixtures/reviewsHub';
import cpcProductMock from '@self/platform/spec/hermione/fixtures/product/productWithCpcOffer';
import {offerDSBSMock} from '@self/platform/spec/hermione/fixtures/dsbs';

import {phone as phoneProductMock} from './fixtures/product';
import {phonePicture1, phonePicture2, phonePicture3} from './fixtures/pictures';
import alcoMock from './fixtures/alco';
import seo from './seo';
import cashback from './deals/cashback';
import genericBundle from './deals/genericBundle';
import {breadcrumbsExpressSuite} from './breadcrumbs/express';
import {AVAILABLE_DELIVERY_STATE, GRID_VIEW_DATA_STATE, LIST_VIEW_DATA_STATE, MINIMAL_DATA_STATE} from './constants';

const testData = [
    {
        type: 'GURU',
        nid: 54726,
        slug: 'mobilnye-telefony',
        viewtype: 'grid',
    },
    {
        type: 'GURU',
        nid: 54726,
        slug: 'mobilnye-telefony',
        viewtype: 'list',
    },
    {
        type: 'GROUP',
        nid: 54535,
        slug: 'karty-flesh-pamiati',
        viewtype: 'grid',
    },
    {
        type: 'GROUP',
        nid: 54535,
        slug: 'karty-flesh-pamiati',
        viewtype: 'list',
    },
    {
        type: 'BOOK',
        nid: 56583,
        slug: 'zarubezhnaia-proza-i-poeziia',
        viewtype: 'grid',
    },
    {
        type: 'BOOK',
        nid: 56583,
        slug: 'zarubezhnaia-proza-i-poeziia',
        viewtype: 'list',
    },
    {
        type: 'CLUSTER',
        nid: 57254,
        slug: 'zhenskie-kolgotki-i-chulki',
        viewtype: 'grid',
    },
    {
        type: 'CLUSTER',
        nid: 57254,
        slug: 'zhenskie-kolgotki-i-chulki',
        viewtype: 'list',
    },
];

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница каталога.', {
    issue: 'AUTOTESTMARKET-4093',
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    snippetList: () => this.createPageObject(
                        SnippetList,
                        {
                            root: `${SnippetList.root}:nth-child(1)`,
                        }
                    ),
                    snippetCard2: () => this.createPageObject(
                        SnippetCard,
                        {
                            parent: this.snippetList,
                            root: `${SnippetCard.root}:nth-of-type(1)`,
                        }
                    ),
                    searchSerpResults: () => this.createPageObject(SearchSerpResults),
                    snippetCell2: () => this.createPageObject(
                        SnippetCell,
                        {
                            parent: this.searchSerpResults.getNthChild(1),
                            root: SnippetCell.root,
                        }
                    ),
                    searchResults: () => this.createPageObject(SearchResults),
                });
            },
        },

        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        makeSuite('Карта.', {
            environment: 'kadavr',
            story: _.merge(
                createStories(
                    [
                        mapIconConfigs.guru,
                        mapIconConfigs.guruLight,
                    ],
                    ({mapIcon, routeParams}) => mergeSuites(
                        {
                            async beforeEach() {
                                await this.browser.setState('report', mergeState([
                                    createProduct({slug: 'product'}),
                                    MINIMAL_DATA_STATE,
                                ]));
                                return this.browser.yaOpenPage('market:list', routeParams);
                            },
                        },
                        prepareSuite(SortPanelSuite, {
                            meta: {
                                id: mapIcon.minimapTestId,
                            },
                            pageObjects: {
                                sortPanel() {
                                    return this.createPageObject(FilterPanelDropdown);
                                },
                            },
                            params: {
                                routeName: 'geo',
                                routeParams: mapIcon.linkRouteParams,
                            },
                        })
                    )
                )
            ),
        }),*/

        makeSuite('Брендокрутилка.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        this.setPageObjects({
                            popularBrandsCarousel: () => this.createPageObject(PopularBrandsCarousel),
                            scrollBox: () => this.createPageObject(SrcScrollBox, {parent: PopularBrandsCarousel.root}),
                        });
                        await this.browser.yaOpenPage('market:list', routes.brands.catalogWithBrandCarousel);
                        // Брендокрутилка на выдаче загружается лениво
                        await this.browser.yaExecAsyncClientScript('window.initAllLazyWidgets');
                    },
                },

                prepareSuite(LogoCarouselSuite, {
                    params: {
                        title: 'Популярные бренды',
                        url: {
                            pathname: 'brands--.+/[0-9]+',
                        },
                    },
                })
            ),
        }),

        makeSuite('Выдача.', {
            story: {
                'Гридовый список сниппетов.': mergeSuites(
                    prepareSuite(SnippetCell2CpcTrackSuite, {
                        params: {
                            cpc: cpcProductMock.cpc,
                        },
                        hooks: {
                            async beforeEach() {
                                await this.browser.setState(
                                    'report',
                                    mergeState([
                                        cpcProductMock.state,
                                        MINIMAL_DATA_STATE,
                                        GRID_VIEW_DATA_STATE,
                                    ])
                                );

                                return this.browser.yaOpenPage('market:list', {
                                    ...routes.list.phones,
                                    viewtype: 'grid',
                                });
                            },
                        },
                    }),

                    prepareSuite(SnippetCell2PreviewImageSuite, {
                        params: {
                            previewImageUrl: phonePicture1.thumbnails[4].url,
                            countImages: 3,
                        },
                        hooks: {
                            async beforeEach() {
                                const threePhonePictures = [phonePicture1, phonePicture2, phonePicture3];

                                const product = createProduct(phoneProductMock, phoneProductMock.id);
                                const pictures = threePhonePictures.map(pic =>
                                    createEntityPicture(pic, 'product', phoneProductMock.id, pic.url));

                                const product2 = createProduct(phoneProductMock);
                                const idProduct2 = product2.data.search.results[0].id;
                                const pictures2 = threePhonePictures.map(pic =>
                                    createEntityPicture(pic, 'product', idProduct2, pic.url));

                                const state = mergeState([
                                    product,
                                    ...pictures,
                                    product2,
                                    ...pictures2,
                                    MINIMAL_DATA_STATE,
                                    AVAILABLE_DELIVERY_STATE,
                                    GRID_VIEW_DATA_STATE,
                                ]);
                                await this.browser.setState('report', state);
                                await this.browser.yaSetCookie({name: COOKIE_NAME.HARD_CPA_ONLY_ENABLED, value: '0'});
                                this.setPageObjects({
                                    anotherCell: () => this.createPageObject(SnippetCell, {
                                        // Непонятно как выбрать нужный элемент, кроме как по tid
                                        // select возвращает неверные селекторы компонентов в SearchSerpResults
                                        parent: `${SearchSerpResults.root} [data-tid="9a9113ee"]:nth-child(2)`,
                                        root: SnippetCell.root,
                                    }),
                                });
                                return this.browser.yaOpenPage('market:list', {
                                    'local-offers-first': 0,
                                    'nid': phoneProductMock.navnodes[0].id,
                                    'slug': phoneProductMock.navnodes[0].slug,
                                    'onstock': 1,
                                    'viewtype': 'grid',
                                });
                            },
                            afterEach() {
                                return this.browser.deleteCookie(COOKIE_NAME.HARD_CPA_ONLY_ENABLED);
                            },
                        },
                    })
                ),

                'Листовая выдача.': mergeSuites(
                    prepareSuite(SnippetCard2PreviewImageSuite, {
                        params: {
                            previewImageUrl: phonePicture1.thumbnails[4].url,
                            countImages: 3,
                        },
                        hooks: {
                            async beforeEach() {
                                const threePhonePictures = [phonePicture1, phonePicture2, phonePicture3];

                                const product = createProduct(phoneProductMock, phoneProductMock.id);
                                const pictures = threePhonePictures.map(pic =>
                                    createEntityPicture(pic, 'product', phoneProductMock.id, pic.url));

                                const product2 = createProduct(phoneProductMock);
                                const idProduct2 = product2.data.search.results[0].id;
                                const pictures2 = threePhonePictures.map(pic =>
                                    createEntityPicture(pic, 'product', idProduct2, pic.url));
                                const state = mergeState([
                                    ...pictures,
                                    ...pictures2,
                                    product,
                                    product2,
                                    MINIMAL_DATA_STATE,
                                    LIST_VIEW_DATA_STATE,
                                ]);
                                await this.browser.setState('report', state);
                                await this.browser.yaSetCookie({name: COOKIE_NAME.HARD_CPA_ONLY_ENABLED, value: '0'});
                                this.setPageObjects({
                                    anotherCard: () => this.createPageObject(SnippetCard, {
                                        parent: this.searchSerpResults,
                                        root: `${SearchSerpResults.root} [data-index="1"]`,
                                    }),
                                });
                                return this.browser.yaOpenPage('market:list', {
                                    'local-offers-first': 0,
                                    'nid': phoneProductMock.navnodes[0].id,
                                    'slug': phoneProductMock.navnodes[0].slug,
                                    'onstock': 1,
                                });
                            },
                        },
                    }),

                    makeSuite('Алкогольный оффер', {
                        environment: 'kadavr',
                        story: {
                            'Информация о доставке.': mergeSuites(
                                {
                                    async beforeEach() {
                                        this.setPageObjects({
                                            delivery: () => this.createPageObject(DeliveryInfo, {
                                                parent: this.snippetCard2,
                                            }),
                                        });

                                        await this.browser.setState('report', mergeState([
                                            alcoMock.state,
                                            MINIMAL_DATA_STATE,
                                        ]));
                                        await this.browser.yaSetCookie({name: 'adult', value: '1'});
                                        await this.browser.yaOpenPage('market:list', alcoMock.route);
                                    },
                                },

                                prepareSuite(DeliveryTextSuite, {
                                    meta: {
                                        issue: 'MARKETVERSTKA-34039',
                                        id: 'marketfront-3394',
                                    },
                                    params: {
                                        expectedText: '1 магазин',
                                    },
                                })
                            ),
                        },
                    })
                ),

                'Листовой список сниппетов, CPA-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            const dataMixin = {
                                data: {
                                    search: {
                                        view: 'list',
                                        total: 1,
                                        totalOffers: 1,
                                    },
                                },
                            };

                            const state = mergeState([
                                cpaOfferKadavrMock.state,
                                dataMixin,
                                AVAILABLE_DELIVERY_STATE,
                            ]);


                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:list', {
                                nid: '123',
                                slug: 'slug',
                                onstock: 1,
                                viewtype: 'list',
                            });
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite),
                    prepareSuite(CartButtonCounterSuite),
                    prepareSuite(ItemCounterCartButton, {
                        params: {
                            offerId: cpaOfferKadavrMock.offerMock.wareId,
                            counterStep: cpaOfferKadavrMock.offerMock.bundleSettings.quantityLimit.step,
                        },
                        meta: {
                            id: 'marketfront-4195',
                        },
                        pageObjects: {
                            parent() {
                                return this.createPageObject(SnippetCard);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton);
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton);
                            },
                            cartPopup() {
                                return this.createPageObject(CartPopup);
                            },
                        },
                    })
                ),
                'Гридовый список сниппетов, CPA-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                    },
                                },
                            };

                            const state = mergeState([
                                cpaOfferKadavrMock.state,
                                dataMixin,
                                AVAILABLE_DELIVERY_STATE,
                                GRID_VIEW_DATA_STATE,
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:list', {
                                nid: '123',
                                slug: 'slug',
                                onstock: 1,
                                viewtype: 'grid',
                            });
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite),
                    prepareSuite(CartButtonCounterSuite),
                    prepareSuite(ItemCounterCartButton, {
                        params: {
                            counterStep: cpaOfferKadavrMock.offerMock.bundleSettings.quantityLimit.step,
                            offerId: cpaOfferKadavrMock.offerMock.wareId,
                        },
                        meta: {
                            id: 'marketfront-4195',
                        },
                        pageObjects: {
                            parent() {
                                return this.createPageObject(SnippetCell);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton);
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton);
                            },
                            cartPopup() {
                                return this.createPageObject(CartPopup);
                            },
                        },
                    })
                ),

                'Листовой список сниппетов, DSBS-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            const state = mergeState([
                                createOffer(offerDSBSMock, offerDSBSMock.id),
                                {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                },
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:list', {
                                nid: '123',
                                slug: 'slug',
                                onstock: 1,
                                viewtype: 'list',
                            });
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite)
                ),

                'Характеристики модели на сниппете.': _.zipObject(
                    _.map(_.filter(testData, ['viewtype', 'list']), data => `Тип: ${data.type}, ${data.viewtype}.`),
                    _.map(_.filter(testData, ['viewtype', 'list']), params => prepareSuite(SnippetCard2DescSuite, {
                        hooks: {
                            async beforeEach() {
                                const product = createProduct(phoneProductMock, phoneProductMock.id);

                                await this.browser.setState('report', mergeState([product, MINIMAL_DATA_STATE]));

                                return this.browser.yaOpenPage(
                                    'market:list',
                                    _.pick(params, ['nid', 'slug', 'viewtype'])
                                );
                            },
                        },
                        params: {
                            type: params.type,
                            count: 5,
                            paramLength: 85,
                            region: 213,
                            specs: phoneProductMock.specs,
                        },
                    }))
                ),

                'Панель сортировок.': mergeSuites(
                    {
                        beforeEach() {
                            this.setPageObjects({
                                filterPanelDropdown: () => this.createPageObject(
                                    FilterPanelDropdown,
                                    {
                                        root: `${FilterPanelDropdown.root}`,
                                    }
                                ),
                                checkbox: () => this.createPageObject(
                                    UIKitCheckbox,
                                    {
                                        parent: '[data-grabber="SearchControls"]',
                                    }
                                ),
                            });

                            const query = routes.list.grid;
                            this.params = {
                                query,
                                path: 'market:list',
                            };
                        },
                    },

                    prepareSuite(FilterPanelDropdownLocalOffersFirstSuite)
                ),
            },
        }),

        makeSuite('Гуру-категория.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const pageParams = routes.list.phones;
                        const treeParams = ['Мобильные телефоны', pageParams.nid, pageParams.hid];
                        const recipes = reviewsHubMock.createRecipes(20);

                        await this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams));
                        await this.browser.setState(
                            'report',
                            mergeState(recipes.map(recipe => createRecipe(recipe, recipe.id)))
                        );

                        this.params = {
                            ...this.params,
                            recipesNames: recipes.map(recipe => recipe.name),
                        };

                        return this.browser.yaOpenPage('market:list', pageParams);
                    },
                },

                prepareSuite(PopularRecipesSearchSuite, {
                    pageObjects: {
                        popularRecipes() {
                            return this.createPageObject(
                                PopularRecipes,
                                {
                                    root: PopularRecipes.rootModeSearch,
                                }
                            );
                        },
                    },
                    params: {
                        recipesCountInitial: 10,
                        recipesCountMax: 20,
                    },
                })
            ),
        }),

        makeSuite('Гуру-лайт категория.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const pageParams = routes.list.boards;
                        const treeParams = ['Паркетная доска', pageParams.nid, pageParams.hid];
                        const recipes = reviewsHubMock.createRecipes(20);

                        await this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams));
                        await this.browser.setState(
                            'report',
                            mergeState(recipes.map(recipe => createRecipe(recipe, recipe.id)))
                        );

                        this.params = {
                            ...this.params,
                            recipesNames: recipes.map(recipe => recipe.name),
                        };

                        return this.browser.yaOpenPage('market:list', pageParams);
                    },
                },

                prepareSuite(PopularRecipesSearchSuite, {
                    pageObjects: {
                        popularRecipes() {
                            return this.createPageObject(
                                PopularRecipes,
                                {
                                    root: PopularRecipes.rootModeSearch,
                                }
                            );
                        },
                    },
                    params: {
                        recipesCountInitial: 10,
                        recipesCountMax: 20,
                    },
                })
            ),
        }),

        makeSuite('Кластерная категория.', {
            environment: 'kadavr',
            story: mergeSuites(
                {
                    async beforeEach() {
                        const pageParams = routes.list.stockings;
                        const treeParams = ['Женские колготки и чулки', pageParams.nid, pageParams.hid];
                        const recipes = reviewsHubMock.createRecipes(20);

                        await this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams));
                        await this.browser.setState(
                            'report',
                            mergeState(recipes.map(recipe => createRecipe(recipe, recipe.id)))
                        );

                        this.params = {
                            ...this.params,
                            recipesNames: recipes.map(recipe => recipe.name),
                        };

                        return this.browser.yaOpenPage('market:list', pageParams);
                    },
                },

                prepareSuite(PopularRecipesSearchSuite, {
                    pageObjects: {
                        popularRecipes() {
                            return this.createPageObject(
                                PopularRecipes,
                                {
                                    root: PopularRecipes.rootModeSearch,
                                }
                            );
                        },
                    },
                    params: {
                        recipesCountInitial: 10,
                        recipesCountMax: 20,
                    },
                })
            ),
        }),

        makeSuite('Хаб отзывов.', {
            environment: 'kadavr',
            story: {
                'Поисковая выдача.': mergeSuites(
                    {
                        async beforeEach() {
                            this.setPageObjects({
                                snippetList: () => this.createPageObject(SnippetList),
                                reviewSnippet: () => this.createPageObject(
                                    ReviewSnippet,
                                    {
                                        parent: this.snippetList,
                                        root: `${ReviewSnippet.root}:nth-of-type(1)`,
                                    }
                                ),
                                productReview: () => this.createPageObject(
                                    ProductReview,
                                    {
                                        parent: this.reviewSnippet,
                                    }
                                ),
                                snippetCell2: () => this.createPageObject(
                                    SnippetCell,
                                    {
                                        parent: this.reviewSnippet,
                                    }
                                ),
                            });

                            await Promise.all([
                                this.browser.setState('schema', {
                                    users: [createUser(reviewsHubMock.user)],
                                }),
                                this.browser.setState(
                                    'report',
                                    mergeState([
                                        createProduct(reviewsHubMock.product, reviewsHubMock.product.id),
                                        MINIMAL_DATA_STATE,
                                        LIST_VIEW_DATA_STATE,
                                    ])
                                ),
                                this.browser.yaSetCookie({name: COOKIE_NAME.HARD_CPA_ONLY_ENABLED, value: '0'}),
                            ]);

                            return this.browser.yaOpenPage('market:list', routes.list.reviews);
                        },
                    },

                    prepareSuite(ProductReviewSuite, {
                        params: {
                            expectedRating: 5,
                        },
                    }),
                    prepareSuite(ReviewSnippetSuite, {
                        params: {
                            id: reviewsHubMock.product.id,
                            slug: reviewsHubMock.product.slug,
                        },
                    })
                ),

                'Панель сортировок.': mergeSuites(
                    {
                        async beforeEach() {
                            await this.browser.setState('report', mergeState([
                                createProduct({slug: 'product'}, 1),
                                MINIMAL_DATA_STATE,
                            ]));

                            this.setPageObjects({
                                checkbox: () => this.createPageObject(
                                    UIKitCheckbox,
                                    {
                                        parent: '[data-grabber="SearchControls"]',
                                    }
                                ),
                            });
                        },
                    },

                    prepareSuite(FilterPanelDropdownLocalOffersFirstSuite, {
                        params: {
                            query: routes.list.reviews,
                            path: 'market:list',
                        },
                    })
                ),

                'Популярные рецепты.': mergeSuites(
                    {
                        async beforeEach() {
                            const recipes = reviewsHubMock.createRecipes(20);
                            await this.browser.setState(
                                'report',
                                mergeState(recipes.map(recipe => createRecipe(recipe, recipe.id)))
                            );

                            this.params = {
                                ...this.params,
                                recipesNames: recipes.map(recipe => recipe.name),
                            };

                            return this.browser.yaOpenPage('market:list', routes.list.reviews);
                        },
                    },

                    prepareSuite(PopularRecipesSearchSuite, {
                        pageObjects: {
                            popularRecipes() {
                                return this.createPageObject(
                                    PopularRecipes,
                                    {
                                        root: PopularRecipes.rootModeSearch,
                                    }
                                );
                            },
                        },
                        params: {
                            recipesCountInitial: 10,
                            recipesCountMax: 20,
                        },
                    })
                ),
            },
        }),

        prepareSuite(PopupComplainSuite, {
            meta: {
                environment: 'kadavr',
            },
            hooks: {
                async beforeEach() {
                    const offer = createOffer({
                        cpc: 'MEISANOFFER',
                        shop: {
                            id: 1,
                            name: 'shop',
                            slug: 'shop',
                        },
                        urls: {
                            encrypted: '/redir/test',
                            decrypted: '/redir/test',
                            geo: '/redir/test',
                            offercard: '/redir/test',
                        },
                    });
                    const pageParams = routes.catalog.feedbackPopup;
                    const treeParams = ['Настольные', pageParams.nid, pageParams.hid, {viewType: 'list'}];

                    await Promise.all([
                        this.browser.setState('report', mergeState([offer, MINIMAL_DATA_STATE])),
                        this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams)),
                        this.browser.setState('Forms.data.collections.forms', {
                            [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                        }),
                    ]);

                    await this.browser.yaOpenPage('market:list', pageParams);
                    await this.browser.moveToObject(SnippetCard.root);
                },
            },
            pageObjects: {
                popup() {
                    return this.createPageObject(ComplainButton, {
                        parent: this.snippetCard2,
                    });
                },
                popupForm() {
                    return this.createPageObject(ComplainPopup);
                },
            },
        }),

        prepareSuite(ShopInfoDrugsDisclaimerSuite, {
            meta: {
                id: 'marketfront-3871',
                issue: 'MARKETFRONT-7776',
            },
            pageObjects: {
                shopsInfo() {
                    return this.createPageObject(LegalInfo);
                },
            },
            hooks: {
                async beforeEach() {
                    const offerId = 'uQizLmsYjkLixn5SRhgitQ';
                    const offer = createOffer({
                        shop: {
                            id: 1,
                            name: 'shop',
                            slug: 'shop',
                            outletsCount: 1,
                        },
                        urls: {
                            encrypted: '/redir/encrypted',
                            decrypted: '/redir/decrypted',
                            offercard: '/redir/offercard',
                            geo: '/redir/geo',
                        },
                    }, offerId);
                    await this.browser.setState('report', mergeState([offer, MINIMAL_DATA_STATE]));
                    return this.browser.yaOpenPage('market:offer', {offerId});
                },
            },
        }),

        seo,
        cashback,
        genericBundle,
        breadcrumbsExpressSuite,
        prepareSuite(growingCashbackIncut, {
            params: {
                viewType: 'list',
            },
        }),

        prepareSuite(RetailShopsIncutSuite)
    ),
});
