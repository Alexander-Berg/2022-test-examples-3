import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';
import {setReportState} from '@self/root/src/spec/hermione/scenarios/kadavr';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';

import emitCashbackOrder from './emitOrder';
import spendOrderCashback from './spendOrderCashback';
import multiOrderCashbackEmit from './multiOrderEmit';
import multiOrderCashbackSpend from './multiOrderSpend';

export default makeSuite('Кэшбэк', {
    feature: 'Кэшбэк',
    environment: 'kadavr',
    issue: 'MARKETFRONT-14483',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    orderConfirmation: () => this.createPageObject(OrderConfirmation, {
                        parent: this.confirmationPage,
                    }),
                });

                const defaultState = mergeState([
                    {data: {results: [outletMock], search: {results: []}}},
                ]);
                await this.browser.yaScenario(this, setReportState, {state: defaultState});
            },
        },
        prepareSuite(emitCashbackOrder),
        prepareSuite(spendOrderCashback),
        prepareSuite(multiOrderCashbackEmit),
        prepareSuite(multiOrderCashbackSpend)
    ),
});
