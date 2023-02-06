import {makeSuite, prepareSuite} from 'ginny';
import {createFilter, createFilterValue, createOffer, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {paymentTypesFilter, paymentTypesFilterValues} from '@self/platform/spec/hermione/fixtures/filters/paymentTypes';

import FiltersInteractionWithPaymentTypesSuite from
    '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withPaymentTypes';

export default makeSuite('Фильтр «способы оплаты» взаимодействие с фильтром.', {
    environment: 'kadavr',
    story: prepareSuite(FiltersInteractionWithPaymentTypesSuite, {
        meta: {
            id: 'marketfront-3380',
            issue: 'MARKETVERSTKA-33921',
        },
        hooks: {
            beforeEach() {
                const offerCardOnline = createOffer({
                    payments: {
                        deliveryCard: false,
                        deliveryCash: false,
                        prepaymentCard: true,
                        prepaymentOther: false,
                    },
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                }, '42');
                const offerCardCourier = createOffer({
                    payments: {
                        deliveryCard: true,
                        deliveryCash: false,
                        prepaymentCard: false,
                        prepaymentOther: false,
                    },
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                }, '43');
                const offerCashCourier = createOffer({
                    payments: {
                        deliveryCard: false,
                        deliveryCash: true,
                        prepaymentCard: false,
                        prepaymentOther: false,
                    },
                    urls: {
                        encrypted: '/redir/encrypted',
                        decrypted: '/redir/decrypted',
                        offercard: '/redir/offercard',
                        geo: '/redir/geo',
                    },
                }, '44');
                const filterValues = paymentTypesFilterValues.map(filter => (
                    createFilterValue(filter, 'payments', filter.id)
                ));

                const reportState = mergeState([
                    offerCardOnline,
                    offerCashCourier,
                    offerCardCourier,
                    createFilter(paymentTypesFilter, 'payments'),
                    ...filterValues,
                ]);

                return this.browser.setState('report', reportState)
                    .then(() => this.browser.yaOpenPage('market:search', routes.search.cats));
            },
        },
    }),
});
