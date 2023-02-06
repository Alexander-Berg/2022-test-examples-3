import {makeSuite, prepareSuite} from 'ginny';
import {createFilter, createFilterValue, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import FiltersPaymentsSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/payments';
import FilterList from '@self/platform/spec/page-objects/FilterList';
import SearchFiltersAside from '@self/platform/spec/page-objects/SearchFiltersAside';
import {paymentTypesFilter, paymentTypesFilterValues} from '@self/platform/spec/hermione/fixtures/filters/paymentTypes';

export default makeSuite('Фильтр «способы оплаты» в фильтрах.', {
    environment: 'kadavr',
    story: prepareSuite(FiltersPaymentsSuite, {
        meta: {
            id: 'marketfront-3379',
            issue: 'MARKETVERSTKA-33920',
        },
        hooks: {
            beforeEach() {
                const offer = createOffer({
                    payments: {
                        deliveryCard: true,
                        deliveryCash: true,
                        prepaymentCard: true,
                        prepaymentOther: false,
                    },
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                });
                const filterValues = paymentTypesFilterValues.map(filter => (
                    createFilterValue(filter, 'payments', filter.id)
                ));

                const state = mergeState([
                    offer,
                    createFilter(paymentTypesFilter, 'payments'),
                    ...filterValues,
                ]);

                return this.browser.setState('report', state)
                    .then(() => this.browser.yaOpenPage('market:search', routes.search.cats));
            },
        },
        pageObjects: {
            filterList() {
                return this.createPageObject(
                    FilterList,
                    {
                        parent: SearchFiltersAside.root,
                        root: `${SearchFiltersAside.root} [data-autotest-id="payments"]`,
                    }
                );
            },
        },
    }),
});
