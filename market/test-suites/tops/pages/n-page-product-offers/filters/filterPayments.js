import {prepareSuite, makeSuite} from 'ginny';
import {
    createFilter,
    createFilterValue,
    createOfferForProduct,
    createProduct,
    mergeState,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {paymentTypesFilter, paymentTypesFilterValues} from '@self/platform/spec/hermione/fixtures/filters/paymentTypes';
import FiltersInteractionWithPaymentTypesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withPaymentTypes';

import productMock from './mocks/product.mock';

export default makeSuite('Фильтр способов оплаты.', {
    environment: 'kadavr',
    story: prepareSuite(FiltersInteractionWithPaymentTypesSuite, {
        meta: {
            id: 'marketfront-3376',
            issue: 'MARKETVERSTKA-33917',
        },
        hooks: {
            beforeEach() {
                const mockedProduct = createProduct(productMock, productMock.id);
                const offerCardOnline = createOfferForProduct({
                    payments: {
                        deliveryCard: false,
                        deliveryCash: false,
                        prepaymentCard: true,
                        prepaymentOther: false,
                    },
                }, productMock.id, '42');
                const offerCardCourier = createOfferForProduct({
                    payments: {
                        deliveryCard: true,
                        deliveryCash: false,
                        prepaymentCard: false,
                        prepaymentOther: false,
                    },
                }, productMock.id, '43');
                const offerCashCourier = createOfferForProduct({
                    payments: {
                        deliveryCard: false,
                        deliveryCash: true,
                        prepaymentCard: false,
                        prepaymentOther: false,
                    },
                }, productMock.id, '44');

                const filterValues = paymentTypesFilterValues.map(filter => (
                    createFilterValue(filter, 'payments', filter.id)
                ));
                const dataFixture = {
                    data: {
                        search: {
                            total: 3,
                            totalOffers: 3,
                            totalOffersBeforeFilters: 3,
                            filters: ['payments'],
                            results: [
                                {
                                    id: '42',
                                    schema: 'offer',
                                },
                                {
                                    id: '43',
                                    schema: 'offer',
                                },
                                {
                                    id: '44',
                                    schema: 'offer',
                                },
                            ],
                        },
                    },
                };

                const reportState = mergeState([
                    mockedProduct,
                    offerCardOnline,
                    offerCashCourier,
                    offerCardCourier,
                    dataFixture,
                    createFilter(paymentTypesFilter, 'payments'),
                    ...filterValues,
                ]);

                return this.browser.setState('report', reportState)
                    .then(() => this.browser.yaOpenPage('market:product-offers', {
                        productId: productMock.id,
                        slug: productMock.slug,
                    }));
            },
        },
    }),
});
