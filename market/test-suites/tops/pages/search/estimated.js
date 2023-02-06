import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import {upperFirst} from 'ambar';
import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {
    mergeState,
    createOffer,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {randomString} from '@self/root/src/helpers/string';

// suites
import WithDeliveryTextSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/withDeliveryTextSuite';

// page-objects
import SearchResults from '@self/platform/spec/page-objects/SearchResults';
import SearchSnippet from '@self/platform/spec/page-objects/containers/SearchSnippet';
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';

// mocks
import {createOfferDeliveryOption} from '@self/platform/spec/hermione/test-suites/tops/pages/search/fixtures/delivery';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {getEstimatedDate} from '@self/platform/spec/hermione/test-suites/tops/pages/product/fixtures/getEstimatedDay';

const UNIQUE_DAYS = 30;
const ESTIMATED_DAYS = 60;
const BEFORE_SNIPPET_ELEMS = 2;
const UNIQUE_COURIER = 1;
const ESTIMATED_COURIER = 2;
const UNIQUE_PICKUP = 3;
const ESTIMATED_PICKUP = 4;

export default makeSuite('Оферы с неточной датой доставки', {
    environment: 'kadavr',
    story: createStories({
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
    }, ({route, routeParams}) => mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    uniqueSnippet: () => this.createPageObject(SearchSnippet, {
                        parent: `${SearchResults.root} li:nth-child(${BEFORE_SNIPPET_ELEMS + UNIQUE_COURIER})`,
                    }),
                    estimatedSnippet: () => this.createPageObject(SearchSnippet, {
                        parent: `${SearchResults.root} li:nth-child(${BEFORE_SNIPPET_ELEMS + ESTIMATED_COURIER})`,
                    }),
                    uniquePickupSnippet: () => this.createPageObject(SearchSnippet, {
                        parent: `${SearchResults.root} li:nth-child(${BEFORE_SNIPPET_ELEMS + UNIQUE_PICKUP})`,
                    }),
                    estimatedPickupSnippet: () => this.createPageObject(SearchSnippet, {
                        parent: `${SearchResults.root} li:nth-child(${BEFORE_SNIPPET_ELEMS + ESTIMATED_PICKUP})`,
                    }),
                });

                const offersCount = 4;
                const states = [{
                    data: {
                        search: {
                            total: offersCount,
                            totalOffers: offersCount,
                        },
                    },
                }];

                for (let i = 1; i <= offersCount; i++) {
                    const isEstimated = i === ESTIMATED_COURIER || i === ESTIMATED_PICKUP;
                    const isPickup = i === UNIQUE_PICKUP || i === ESTIMATED_PICKUP;
                    const options = [
                        createOfferDeliveryOption({
                            dayFrom: isEstimated ? ESTIMATED_DAYS : UNIQUE_DAYS,
                            dayTo: isEstimated ? ESTIMATED_DAYS : UNIQUE_DAYS,
                            isEstimated,
                        }),
                    ];

                    states.push(createOffer({
                        cpc: `${randomString()}_${i}`,
                        urls: {
                            encrypted: '/redir/',
                        },
                        delivery: {
                            isAvailable: !isPickup,
                            isDownloadable: false,
                            inStock: isPickup,
                            hasPickup: isPickup,
                            options: isPickup ? null : options,
                            pickupOptions: isPickup ? options : null,
                        },
                        isUniqueOffer: !isEstimated,
                    }));
                }

                await this.browser.setState('report', mergeState(states));
                await this.browser.yaOpenPage(route, routeParams).yaClosePopup(this.regionPopup);
            },
        },
        prepareSuite(WithDeliveryTextSuite, {
            meta: {
                id: 'marketfront-5957',
                issue: 'MARKETFRONT-86020',
            },
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery, {
                        parent: this.estimatedSnippet,
                    });
                },
            },
            params: {
                deliveryText: `${upperFirst(getEstimatedDate(ESTIMATED_DAYS))}, от продавца`,
            },
        }),
        prepareSuite(WithDeliveryTextSuite, {
            suiteName: 'У товара под заказ',
            meta: {
                id: 'marketfront-5958',
                issue: 'MARKETFRONT-86020',
            },
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery, {
                        parent: this.uniqueSnippet,
                    });
                },
            },
            params: {
                deliveryText: `${upperFirst(getEstimatedDate(UNIQUE_DAYS))}, от продавца`,
            },
        }),
        prepareSuite(WithDeliveryTextSuite, {
            suiteName: 'Доставка самовывозом.',
            meta: {
                id: 'marketfront-6098',
                issue: 'MARKETFRONT-86020',
            },
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery, {
                        parent: this.estimatedPickupSnippet,
                    });
                },
            },
            params: {
                deliveryText: `Самовывоз ${getEstimatedDate(ESTIMATED_DAYS)}`,
            },
        }),
        prepareSuite(WithDeliveryTextSuite, {
            suiteName: 'Доставка самовывозом. У товара под заказ',
            meta: {
                id: 'marketfront-6099',
                issue: 'MARKETFRONT-86020',
            },
            pageObjects: {
                deliveryInfo() {
                    return this.createPageObject(SearchSnippetDelivery, {
                        parent: this.uniquePickupSnippet,
                    });
                },
            },
            params: {
                deliveryText: `Самовывоз ${getEstimatedDate(UNIQUE_DAYS)}`,
            },
        })
    )),
});
