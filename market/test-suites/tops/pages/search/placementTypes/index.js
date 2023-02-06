import dayjs from 'dayjs';
import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import schema from 'js-schema';

import {PAGE_IDS_TOUCH} from '@self/root/src/constants/pageIds';
import {NBSP} from '@self/platform/constants/string';

// suites
import SearchOfferPlacementTypesSuite from '@self/platform/spec/hermione/test-suites/blocks/SearchOffer/placementTypes';
import MetricaClickSuite from '@self/platform/spec/hermione/test-suites/blocks/Metrica/click';

// page-objects
import SearchSnippetDelivery from '@self/platform/spec/page-objects/containers/SearchSnippet/Delivery';
import SearchSnippetCartButton from '@self/platform/spec/page-objects/containers/SearchSnippet/CartButton';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import nodeConfig from '@self/platform/configs/development/node';

import {
    productWithFirstPartyOfferState,
    productWithThirdPartyOfferState,
} from './fixtures/placementTypes';

const EXPECTED = {
    // текст отсюда market/platform.touch/containers/SearchSnippet/connectors/helpers/getDeliveryFormattedText.js:75
    DELIVERY_TEXT: `${dayjs().add(4, 'day').format(`D${NBSP}MMMM`)}, от Яндекса`,
    GOAL_NAME: 'search-page_search-results_offer-snippet_cart-button_cart-add-offer-to-cart',
};

export default makeSuite('Типы размещения офферов.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Офферная выдача. 1P оффер.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await this.browser.setState('report', productWithFirstPartyOfferState);
                        return this.browser.yaOpenPage(
                            PAGE_IDS_TOUCH.YANDEX_MARKET_SEARCH,
                            routes.search.default
                        );
                    },
                },
                prepareSuite(SearchOfferPlacementTypesSuite, {
                    meta: {
                        id: 'marketfront-4153',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedElementText: EXPECTED.DELIVERY_TEXT,
                    },
                    pageObjects: {
                        snippetDelivery() {
                            return this.createPageObject(SearchSnippetDelivery);
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
                            PAGE_IDS_TOUCH.YANDEX_MARKET_SEARCH,
                            routes.search.default
                        );
                    },
                },
                prepareSuite(SearchOfferPlacementTypesSuite, {
                    meta: {
                        id: 'marketfront-4158',
                        issue: 'MARKETFRONT-18602',
                    },
                    params: {
                        expectedElementText: EXPECTED.DELIVERY_TEXT,
                    },
                    pageObjects: {
                        snippetDelivery() {
                            return this.createPageObject(SearchSnippetDelivery);
                        },
                    },
                })
            ),
        }),
        makeSuite('Метрика. Наличие параметра 1Р при добавлении товара в корзину.', {
            story: prepareSuite(MetricaClickSuite, {meta: {
                id: 'marketfront-4154',
                issue: 'MARKETFRONT-18602',
            },
            params: {
                expectedGoalName: EXPECTED.GOAL_NAME,
                counterId: nodeConfig.yaMetrika.market.id,
                payloadSchema: schema({
                    placementType: '1P',
                }),
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithFirstPartyOfferState);
                    await this.browser.yaOpenPage(
                        PAGE_IDS_TOUCH.YANDEX_MARKET_SEARCH,
                        routes.search.default
                    );

                    this.params.selector = SearchSnippetCartButton.root;
                },
            },
            }),
        }),
        makeSuite('Метрика. Наличие параметра 3Р при добавлении товара в корзину.', {
            story: prepareSuite(MetricaClickSuite, {meta: {
                id: 'marketfront-4154',
                issue: 'MARKETFRONT-18602',
            },
            params: {
                expectedGoalName: EXPECTED.GOAL_NAME,
                counterId: nodeConfig.yaMetrika.market.id,
                payloadSchema: schema({
                    placementType: '3P',
                }),
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState('report', productWithThirdPartyOfferState);
                    await this.browser.yaOpenPage(
                        PAGE_IDS_TOUCH.YANDEX_MARKET_SEARCH,
                        routes.search.default
                    );

                    this.params.selector = SearchSnippetCartButton.root;
                },
            },
            }),
        })
    ),
});
