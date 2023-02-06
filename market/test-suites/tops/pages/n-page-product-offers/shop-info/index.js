import {prepareSuite, mergeSuites, makeSuite} from 'ginny';
import {createProduct, createOfferForProduct} from '@yandex-market/kadavr/mocks/Report/helpers/searchResult';

import mergeReportState from '@yandex-market/kadavr/mocks/Report/helpers/mergeState';
// suites
import ProductOffersSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/product-offers';
import ProductOffersNotInStockSuite from
    '@self/platform/spec/hermione/test-suites/blocks/n-w-shop-info/product-offers__not-in-stock';
// page-objects
import LegalInfo from '@self/platform/spec/page-objects/components/LegalInfo';

import offerFixture from '../fixtures/offer';
import productOptionsFixture from '../fixtures/productOptions';
import dataFixture from '../fixtures/data';

export default makeSuite('Информация о продавце.', {
    environment: 'kadavr',
    story: mergeSuites(
        makeSuite('Модель в продаже.', {
            feature: 'Модель в продаже.',
            issue: 'MARKETVERSTKA-26681',
            story: mergeSuites(
                prepareSuite(ProductOffersSuite, {
                    pageObjects: {
                        shopsInfo() {
                            return this.createPageObject(LegalInfo);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const params = {productId: 1, slug: 'model'};
                            const productState = createProduct(productOptionsFixture, params.productId);
                            const productOfferState = createOfferForProduct(offerFixture, params.productId, '3');

                            const reportState = mergeReportState([
                                productState,
                                productOfferState,
                                dataFixture,
                            ]);
                            return this.browser.setState('report', reportState)
                                .then(() => this.browser.yaOpenPage('market:product-offers', params));
                        },
                    },
                    params: {
                        shopIds: [offerFixture.shop.id],
                    },
                })
            ),
        }),

        makeSuite('Модель не в продаже.', {
            feature: 'Модель не в продаже.',
            issue: 'MARKETVERSTKA-26681',
            story: mergeSuites(
                prepareSuite(ProductOffersNotInStockSuite, {
                    pageObjects: {
                        shopsInfo() {
                            return this.createPageObject(LegalInfo);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            const params = {
                                productId: 1,
                                slug: 'product',
                            };
                            const productState = createProduct(productOptionsFixture, params.productId);

                            return this.browser.setState('report', productState)
                                .then(() => this.browser.yaOpenPage('market:product-offers', params));
                        },
                    },
                })
            ),
        })
    ),
});
