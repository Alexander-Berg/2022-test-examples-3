import _ from 'lodash';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {
    mergeState,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {createSurveyFormMock} from '@self/project/src/spec/hermione/helpers/metakadavr';
// configs
import reactFiltersConfig from '@self/platform/spec/hermione/configs/react-filters';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {shopFeedbackFormId} from '@self/platform/spec/hermione/configs/forms';
// suites
import SnippetListViewtypeSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-list/viewtype';
import FilterLocalOffersFirstSuite
    from '@self/platform/spec/hermione/test-suites/blocks/n-filter-panel-dropdown/__filter_local-offers-first';
import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
import ClarifyCategoryAbsenceSuite from '@self/platform/spec/hermione/test-suites/blocks/ClarifyCategory/absence';
import PopupComplainSuite from '@self/platform/spec/hermione/test-suites/blocks/PopupComplain/shopAndOffer';
import CartButtonSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton';
import CartButtonExpressSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/express';
import CartButtonCounterSuite from '@self/platform/spec/hermione/test-suites/blocks/CartButton/counter';
import ItemCounterCartButtonSuite from '@self/project/src/spec/hermione/test-suites/blocks/ItemCounterCartButton';
import SearchPageDegradationSuite from '@self/platform/spec/hermione/test-suites/tops/pages/n-page-search/degradation';
import PurchaseListSuite from '@self/root/src/spec/hermione/test-suites/blocks/PurchaseList';

// page-objects
import SnippetList from '@self/root/src/widgets/content/search/Serp/components/Page/__pageObject';
import SearchResults from '@self/platform/widgets/content/search/Results/__pageObject';
import SnippetCard from '@self/project/src/components/Search/Snippet/Card/__pageObject';
import SnippetCell from '@self/project/src/components/Search/Snippet/Cell/__pageObject';
import FilterPanelDropdown from '@self/platform/widgets/content/SortPanel/__pageObject';
import ClarifyCategory from '@self/root/src/widgets/content/search/Clarify/components/View/__pageObject';
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
import ComplainButton from '@self/platform/spec/page-objects/components/ComplainButton';
import ComplainPopup from '@self/platform/spec/page-objects/components/ComplainPopup';
import CartButton from '@self/project/src/components/CartButton/__pageObject';
import CounterCartButton from '@self/project/src/components/CounterCartButton/__pageObject';
import CartPopup from '@self/project/src/widgets/content/upsale/CartUpsalePopup/components/Full/Popup/__pageObject/index.desktop';
import ScrollBoxComponent from '@self/root/src/components/ScrollBox/__pageObject';
import LocalOffersFirst from '@self/root/src/widgets/content/search/LocalOffersFirst/__pageObject';

// fixtures
import offerFarma from '@self/root/src/spec/hermione/kadavr-mock/report/offer/farma';
import {offerCPAMock, offerMock, offerExpressMock} from '@self/platform/spec/hermione/fixtures/priceFilter/offer';
import {reportState} from './fixtures';
import searchResultWithIntentsFixture from './fixtures/searchResultWithIntents';

// import
import Delivery from './delivery';
import automaticallyCalculatedDelivery from './automticallyCalculatedDelivery';
import placementTypes from './placementTypes';
import unitInfoSuite from './unitInfo';

const VIEWTYPE_COOKIE = 'viewtype';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница поиска.', {
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
                    snippetCell2: () => this.createPageObject(
                        SnippetCell,
                        {
                            parent: this.snippetList,
                            root: `${SnippetCell.root}:nth-of-type(1)`,
                        }
                    ),
                });
            },
        },
        makeSuite('Выдача с express-офферами', {
            environment: 'kadavr',
            story: {
                'Листовой список сниппетов, express-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            const offer = createOffer(offerExpressMock, offerExpressMock.wareId);
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                        view: 'list',
                                    },
                                },
                            };

                            const state = mergeState([
                                offer,
                                dataMixin,
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:search', routes.search.list);
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonExpressSuite)
                ),
            },
        }),
        makeSuite('Выдача с CPA-офферами', {
            environment: 'kadavr',
            story: {
                'Листовой список сниппетов, CPA-оффер': mergeSuites(
                    {
                        async beforeEach() {
                            const offer = createOffer(offerCPAMock, offerMock.wareId);
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                        view: 'list',
                                    },
                                },
                            };

                            const state = mergeState([
                                offer,
                                dataMixin,
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:search', routes.search.list);
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite),
                    prepareSuite(CartButtonCounterSuite),
                    prepareSuite(ItemCounterCartButtonSuite, {
                        params: {
                            counterStep: offerMock.bundleSettings.quantityLimit.step,
                            offerId: offerMock.wareId,
                        },
                        meta: {
                            id: 'marketfront-4195',
                        },
                        pageObjects: {
                            parent() {
                                return this.createPageObject(SnippetCard);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: SnippetCard.root,
                                });
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: SnippetCard.root,
                                });
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
                            const offer = createOffer(offerCPAMock, offerMock.wareId);
                            const dataMixin = {
                                data: {
                                    search: {
                                        total: 1,
                                        totalOffers: 1,
                                        view: 'grid',
                                    },
                                },
                            };

                            const state = mergeState([
                                offer,
                                dataMixin,
                            ]);

                            await this.browser.setState('Carter.items', []);
                            await this.browser.setState('report', state);

                            return this.browser.yaOpenPage('market:search', routes.search.grid);
                        },
                        afterEach() {
                            return this.browser.deleteCookie('viewtype');
                        },
                    },
                    prepareSuite(CartButtonSuite),
                    prepareSuite(CartButtonCounterSuite),
                    prepareSuite(ItemCounterCartButtonSuite, {
                        params: {
                            counterStep: offerMock.bundleSettings.quantityLimit.step,
                            offerId: offerMock.wareId,
                        },
                        meta: {
                            id: 'marketfront-4195',
                        },
                        pageObjects: {
                            parent() {
                                return this.createPageObject(SnippetCell);
                            },
                            cartButton() {
                                return this.createPageObject(CartButton, {
                                    parent: SnippetCell.root,
                                });
                            },
                            counterCartButton() {
                                return this.createPageObject(CounterCartButton, {
                                    parent: SnippetCell.root,
                                });
                            },
                            cartPopup() {
                                return this.createPageObject(CartPopup);
                            },
                        },
                    })
                ),
            },
        }),
        // Кадавризированные тесты для выдачи
        makeSuite('Выдача категорий', {
            environment: 'kadavr',
            story: {
                'Категорийное дерево': mergeSuites(
                    {
                        beforeEach() {
                            this.browser.setState('report', searchResultWithIntentsFixture.state);
                            return this.browser.yaOpenPage(
                                'market:search',
                                routes.search.withClarifyCategory
                            );
                        },
                    },
                    prepareSuite(ScrollBoxSuite, {
                        pageObjects: {
                            scrollBox() {
                                return this.createPageObject(ScrollBoxComponent, {
                                    parent: ClarifyCategory.root,
                                });
                            },
                        },
                    })
                ),
            },
        }),

        makeSuite('Выдача', {
            environment: 'kadavr',
            story: {
                'Листовая выдача.': mergeSuites(
                    {
                        async beforeEach() {
                            await this.browser.deleteCookie(VIEWTYPE_COOKIE);
                            this.browser.setState('report', mergeState([

                                reportState,
                                {
                                    data: {
                                        search: {
                                            view: 'list',
                                        },
                                    },
                                }]));
                            return this.browser.yaOpenPage('market:search', routes.search.list);
                        },

                        async afterEach() {
                            await this.browser.deleteCookie(VIEWTYPE_COOKIE);
                        },
                    },
                    prepareSuite(SnippetListViewtypeSuite, {
                        params: {
                            viewtype: 'list',
                        },
                        meta: {
                            environment: 'kadavr',
                        },
                    }),
                    prepareSuite(PurchaseListSuite, {
                        params: {
                            expectedPrice: 'от 107 ₽',
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

                                await this.browser.setState('report', state);

                                return this.browser.yaOpenPage('market:search', routes.search.medicinePurchaseList);
                            },
                        },
                        pageObjects: {
                            snippetCard2() {
                                return this.createPageObject(SnippetCard, {parent: SnippetList.root});
                            },
                        },
                    })
                ),

                'Панель сортировок.': mergeSuites(
                    {
                        async beforeEach() {
                            await this.browser.setState('report', mergeState([
                                createOffer(offerExpressMock, offerExpressMock.wareId),
                                {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                },
                            ]));

                            this.setPageObjects({
                                filterPanelDropdown: () => this.createPageObject(
                                    FilterPanelDropdown,
                                    {
                                        root: `${FilterPanelDropdown.root}`,
                                    }
                                ),
                                checkbox: () => this.createPageObject(
                                    LocalOffersFirst
                                ),
                            });

                            const query = routes.search.grid;
                            this.params = {
                                query,
                                path: 'market:search',
                            };
                        },
                    },

                    prepareSuite(FilterLocalOffersFirstSuite)
                ),

                'Категорийное дерево.': mergeSuites(
                    {
                        beforeEach() {
                            return this.browser.yaOpenPage('market:search', routes.search.withClarifyCategory);
                        },
                    },

                    prepareSuite(ScrollBoxSuite, {
                        pageObjects: {
                            scrollBox() {
                                return this.createPageObject(ScrollBox, {
                                    parent: ClarifyCategory.root,
                                });
                            },
                        },
                    }),

                    _.merge(
                        createStories(
                            [
                                reactFiltersConfig.SEARCH.SHOP_ITEMS,
                            ],
                            ({queryParams}) => prepareSuite(ClarifyCategoryAbsenceSuite, {
                                hooks: {
                                    async beforeEach() {
                                        const shop = {
                                            'entity': 'shop',
                                            'id': queryParams.fesh,
                                            'name': 'Custom SHOP',
                                            'status': 'actual',
                                            'oldStatus': 'actual',
                                            'shopName': 'Custom SHOP',
                                            'slug': 'custom',
                                        };

                                        await this.browser.setState('report', createOffer({
                                            shop,
                                            urls: {
                                                encrypted: '/redir/encrypted',
                                                decrypted: '/redir/decrypted',
                                                offercard: '/redir/offercard',
                                                geo: '/redir/geo',
                                            },
                                        }));

                                        return this.browser.yaOpenPage('market:search', queryParams);
                                    },
                                },
                                pageObjects: {
                                    clarifyCategory() {
                                        return this.createPageObject(ClarifyCategory);
                                    },
                                },
                            })
                        ),

                        createStories(
                            [
                                reactFiltersConfig.SEARCH.MULTISEARCH,
                            ],
                            ({queryParams}) => prepareSuite(ClarifyCategoryAbsenceSuite, {
                                hooks: {
                                    async beforeEach() {
                                        const fesh = 1000000000;
                                        const shop = {
                                            'entity': 'shop',
                                            'id': fesh,
                                            'name': 'Custom SHOP',
                                            'status': 'actual',
                                            'oldStatus': 'actual',
                                            'shopName': 'Custom SHOP',
                                            'slug': 'custom',
                                        };

                                        await this.browser.setState('report', createOffer({
                                            shop,
                                            urls: {
                                                encrypted: '/redir/encrypted',
                                                decrypted: '/redir/decrypted',
                                                offercard: '/redir/offercard',
                                                geo: '/redir/geo',
                                            },
                                        }));
                                        return this.browser.yaOpenPage('market:multisearch', {...queryParams, fesh});
                                    },
                                },
                                pageObjects: {
                                    clarifyCategory() {
                                        return this.createPageObject(ClarifyCategory);
                                    },
                                },
                            })
                        )
                    )
                ),
            },
        }),

        /*
        * Точка входа удалена. Задача на возврат тестов https://st.yandex-team.ru/MARKETFRONT-71298
        makeSuite('Карта.', {
            environment: 'kadavr',
            story: prepareSuite(SortPanelSuite, {
                pageObjects: {
                    sortPanel() {
                        return this.createPageObject(FilterPanelDropdown);
                    },
                },
                params: {
                    routeName: 'geo',
                    routeParams: routes.search.cats,
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('report', createProduct({slug: 'product'}));
                        return this.browser.yaOpenPage('market:search', routes.search.cats);
                    },
                },
            }),
        }),*/

        Delivery,

        prepareSuite(PopupComplainSuite, {
            meta: {
                environment: 'kadavr',
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
            hooks: {
                async beforeEach() {
                    const offer = createOffer({
                        cpc: 'MEISABEAUTIFULOFFER',
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

                    const dataMixin = {
                        data: {
                            search: {
                                total: 1,
                                view: 'list',
                            },
                        },
                    };

                    await Promise.all([
                        this.browser.setState('report', mergeState([offer, dataMixin])),
                        this.browser.setState('Forms.data.collections.forms', {
                            [shopFeedbackFormId]: createSurveyFormMock(shopFeedbackFormId),
                        }),
                    ]);
                    await this.browser.yaOpenPage('market:search', routes.search.default);
                    await this.browser.moveToObject(SnippetCard.root);
                },
            },
        }),

        automaticallyCalculatedDelivery,

        unitInfoSuite,

        placementTypes,
        prepareSuite(SearchPageDegradationSuite, {
            pageObjects: {
                searchResults() {
                    return this.createPageObject(SearchResults);
                },
            },
            hooks: {
                async beforeEach() {
                    const offer = createOffer(offerCPAMock, offerMock.wareId);
                    const dataMixin = {
                        data: {
                            search: {
                                total: 1,
                                totalOffers: 1,
                                view: 'list',
                            },
                        },
                    };

                    const state = mergeState([
                        offer,
                        dataMixin,
                    ]);

                    await this.browser.setState('Carter.items', []);
                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('market:search', routes.search.grid);
                },
            },
        })
    ),
});
