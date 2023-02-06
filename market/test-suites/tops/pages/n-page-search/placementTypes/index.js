import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import schema from 'js-schema';

// suites
import DeliveryFromWarehouse from '@self/platform/spec/hermione/test-suites/blocks/DeliveryFromWarehouse';

// page-objects
import DeliveryFromWarehousePO from '@self/project/src/components/DeliveryFromWarehouse/__pageObject';
import SearchOfferSnippetCard from '@self/project/src/components/Search/Snippet/Offer/Card/__pageObject';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import nodeConfig from '@self/platform/configs/development/node';

import MetricaReactClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/reactClick';

import {
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
} from './fixtures/placementTypes';


export default makeSuite('Типы размещения офферов.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Офферная выдача. 1P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithFirstPartyOfferState);

                        return this.browser.yaOpenPage(
                            'market:search',
                            routes.search.default
                        );
                    },
                },
                prepareSuite(DeliveryFromWarehouse, {
                    meta: {
                        id: 'marketfront-4153',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedElementText: 'со склада Яндекса',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
                        },
                    },
                })
            ),
        }),
        makeSuite('Офферная выдача. 3P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithThirdPartyOfferState);

                        return this.browser.yaOpenPage(
                            'market:search',
                            routes.search.default
                        );
                    },
                },
                prepareSuite(DeliveryFromWarehouse, {
                    meta: {
                        id: 'marketfront-4158',
                        issue: 'MARKETFRONT-17082',
                    },
                    params: {
                        expectedElementText: 'со склада Яндекса',
                    },
                    pageObjects: {
                        deliveryFromWarehouse() {
                            return this.createPageObject(DeliveryFromWarehousePO);
                        },
                    },
                })
            ),
        }),
        makeSuite('Метрика. Наличие параметра 1Р при добавлении товара в корзину.', {
            story: prepareSuite(MetricaReactClickSuite, {
                meta: {
                    id: 'marketfront-4154',
                    issue: 'MARKETFRONT-17082',
                },
                params: {
                    expectedGoalName: 'search-page_search-results_search-results-paged_search-partition_snippet-list_snippet-card_click',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: schema({
                        placementType: '1P',
                    }),
                },
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            snippet: () => this.createPageObject(SearchOfferSnippetCard),
                        });

                        await this.browser.setState('report', productWithFirstPartyOfferState);

                        return this.browser.yaOpenPage(
                            'market:search',
                            routes.search.default
                        );
                    },
                },
            }),
        }),
        makeSuite('Метрика. Наличие параметра 3Р при добавлении товара в корзину.', {
            story: prepareSuite(MetricaReactClickSuite, {
                meta: {
                    id: 'marketfront-4155',
                    issue: 'MARKETFRONT-17082',
                },
                params: {
                    expectedGoalName: 'search-page_search-results_search-results-paged_search-partition_snippet-list_snippet-card_click',
                    counterId: nodeConfig.yaMetrika.market.id,
                    payloadSchema: schema({
                        placementType: '3P',
                    }),
                },
                hooks: {
                    async beforeEach() {
                        await this.setPageObjects({
                            snippet: () => this.createPageObject(SearchOfferSnippetCard),
                        });

                        await this.browser.setState('report', productWithThirdPartyOfferState);

                        return this.browser.yaOpenPage(
                            'market:search',
                            routes.search.default
                        );
                    },
                },
            }),
        })
    ),
});
