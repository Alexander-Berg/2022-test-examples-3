import {makeSuite, prepareSuite} from 'ginny';
import {mergeState, createOffer, createFilter, createFilterValue, createFilterValueGroup} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import FiltersPaymentsSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/payments';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import {paymentTypesFilter, paymentTypesFilterValues} from '@self/platform/spec/hermione/test-suites/tops/pages/fixtures/filters';

import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';

export default makeSuite('Фильтр «Способы оплаты»', {
    environment: 'kadavr',
    story: createStories([
        {
            description: 'Оферная выдача',
            routeParams: routes.catalog.list,
            meta: {
                id: 'm-touch-2854',
                issue: 'MOBMARKET-12541',
            },
        },
        {
            description: 'Модельная выдача',
            routeParams: routes.catalog.phones,
            meta: {
                id: 'm-touch-2855',
                issue: 'MOBMARKET-12541',
            },
        },
    ], ({meta, routeParams}) => prepareSuite(FiltersPaymentsSuite, {
        meta,
        hooks: {
            async beforeEach() {
                const offersCount = 1;
                const offer = createOffer();
                const paymentsFilter = createFilter(paymentTypesFilter, 'payments');

                const paymentsFilterValues = paymentTypesFilterValues.map(filter => (
                    createFilterValue(filter, 'payments', filter.id)
                ));

                const paymentsFilterValuesGroup = paymentTypesFilterValues.map(filter => (
                    createFilterValueGroup(filter, 'payments', filter.id)
                ));

                const treeParams = ['Женские колготки и чулки', 857707, 55313];
                await this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams));

                const totalMixin = {
                    data: {
                        search: {
                            total: offersCount,
                            totalOffers: offersCount,
                        },
                        sorts: [{text: 'по популярности'}],
                    },
                };

                const reportState = mergeState([
                    offer,
                    totalMixin,
                    paymentsFilter,
                    ...paymentsFilterValues,
                    ...paymentsFilterValuesGroup,
                ]);

                return this.browser
                    .setState('report', reportState)
                    .yaOpenPage('touch:list', routeParams);
            },
        },
        pageObjects: {
            searchOptions() {
                return this.createPageObject(SearchOptions);
            },
        },
        params: {
            filterName: 'Способы оплаты',
            filterId: 'payments',
        },
    })),
});
