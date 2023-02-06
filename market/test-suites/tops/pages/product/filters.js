import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    mergeState,
    createFilter,
    createFilterValue,
    createOfferForProduct,
} from '@yandex-market/kadavr/mocks/Report/helpers';

// suites
import FiltersPaymentsSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffers/filters/payments';
import FiltersSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffers/filters';
import FiltersCpaSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductOffers/filters/cpa';
// page-objects
import ProductOffers from '@self/platform/spec/page-objects/widgets/parts/ProductOffers';
import CpaFilterTumbler from '@self/platform/components/FilterTumbler/__pageObject/cpaFilter';
import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';
// fixtures
import {
    productWithAveragePrice,
    phoneWithAveragePriceProductRoute,
} from '@self/platform/spec/hermione/fixtures/product';
import {
    paymentTypesFilter,
    paymentTypesFilterValues,
} from '@self/platform/spec/hermione/test-suites/tops/pages/fixtures/filters';

import {testShop, offerUrls} from './kadavrMocks';

const FILTER_ID = 'payments';

export default makeSuite('Взаимодействие с фильтрами', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: {
        'Блок с фильтрами': mergeSuites(
            {
                beforeEach() {
                    const paymentsFilter = createFilter(paymentTypesFilter, FILTER_ID);
                    const paymentsFilterValues = paymentTypesFilterValues.map(filter => (
                        createFilterValue(filter, FILTER_ID, filter.id)));


                    const offerRegular = createOfferForProduct(
                        {
                            urls: offerUrls,
                            shop: testShop,
                            orderMinCost: {
                                value: 5500,
                                currency: 'RUR',
                            },
                        },
                        phoneWithAveragePriceProductRoute.productId,
                        2
                    );
                    const dataMixin = {
                        data: {
                            search: {
                                total: 2,
                                totalOffers: 2,
                                totalOffersBeforeFilters: 2,
                                results: [
                                    {
                                        id: '2',
                                        schema: 'offer',
                                    },
                                ],
                            },
                        },
                    };

                    const state = mergeState([
                        productWithAveragePrice,
                        offerRegular,
                        dataMixin,
                        paymentsFilter,
                        ...paymentsFilterValues,
                    ]);

                    return this.browser.setState('report', state)
                        .then(() => this.browser.yaOpenPage('touch:product-offers', phoneWithAveragePriceProductRoute));
                },
            },
            prepareSuite(FiltersPaymentsSuite, {
                pageObjects: {
                    productOffers() {
                        return this.createPageObject(ProductOffers);
                    },
                    filterCompound() {
                        return this.createPageObject(FilterCompound, {
                            root: `[data-autotest-id="${FILTER_ID}"]`,
                        });
                    },
                },
                params: {
                    filterName: 'Способы оплаты',
                },
            }),
            prepareSuite(FiltersSuite, {
                pageObjects: {
                    productOffers() {
                        return this.createPageObject(ProductOffers);
                    },
                    filters() {
                        return this.createPageObject(Filters);
                    },
                },
            })
        ),

        'Блок с фильтрами.': prepareSuite(FiltersCpaSuite, {
            params: {},
            pageObjects: {
                productOffers() {
                    return this.createPageObject(ProductOffers);
                },
                filter() {
                    return this.createPageObject(CpaFilterTumbler);
                },
            },
        }),
    },
});
