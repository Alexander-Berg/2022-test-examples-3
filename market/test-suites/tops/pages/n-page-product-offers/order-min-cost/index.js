import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';

import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
import OrderMinCostSuite from '@self/platform/spec/hermione/test-suites/blocks/n-snippet-card/order-min-cost';
import SnippetCard from '@self/platform/spec/page-objects/snippet-card';

import offerFixture from '../fixtures/offer';
import productOptionsFixture from '../fixtures/productOptions';
import dataFixture from '../fixtures/data';

export default makeSuite('Минимальная сумма заказа.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                const params = {
                    productId: 1,
                    slug: 'product',
                };
                const mockedProduct = createProduct(productOptionsFixture, params.productId);
                const offer = createOfferForProduct(offerFixture, params.productId, '3');

                const reportState = mergeReportState([
                    mockedProduct,
                    offer,
                    dataFixture,
                ]);
                return this.browser.setState('report', reportState)
                    .then(() => this.browser.yaOpenPage('market:product-offers', params));
            },
        },
        prepareSuite(OrderMinCostSuite,
            {
                pageObjects: {
                    snippetCard() {
                        return this.createPageObject(SnippetCard);
                    },
                },
            })
    ),
});
