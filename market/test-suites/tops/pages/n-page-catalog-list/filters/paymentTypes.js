import {makeSuite, prepareSuite} from 'ginny';
import {createFilter, createFilterValue, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {createStories} from '@self/platform/spec/hermione/helpers/createStories';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {paymentTypesFilter, paymentTypesFilterValues} from '@self/platform/spec/hermione/fixtures/filters/paymentTypes';
import FiltersPaymentsSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/payments';
import FilterList from '@self/platform/spec/page-objects/FilterList';
import SearchFiltersAside from '@self/platform/spec/page-objects/SearchFiltersAside';

const stories = [
    {
        description: 'в гуру-лайт выдаче',
        queryParams: routes.list.boards,
        meta: {
            id: 'marketfront-3377',
            issue: 'MARKETVERSTKA-33918',
        },
    },
    {
        description: 'в гуру выдаче',
        queryParams: routes.list.phones,
        meta: {
            id: 'marketfront-3378',
            issue: 'MARKETVERSTKA-33919',
        },
    },
];

export default makeSuite('Фильтр «способы оплаты».', {
    environment: 'kadavr',
    story: createStories(stories, ({queryParams, meta}) => prepareSuite(FiltersPaymentsSuite, {
        meta,
        hooks: {
            async beforeEach() {
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

                const state = mergeState([
                    offer,
                    createFilter(paymentTypesFilter, 'payments'),
                ].concat(
                    paymentTypesFilterValues.map(filter => (
                        createFilterValue(filter, 'payments', filter.id)
                    ))
                ));

                await this.browser.setState('report', state);

                return this.browser.yaOpenPage('market:list', queryParams);
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
    })),
});
