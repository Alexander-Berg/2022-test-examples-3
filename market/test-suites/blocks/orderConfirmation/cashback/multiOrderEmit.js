import {makeSuite, makeCase} from 'ginny';

import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {generateCashbackDetailsCollectionsMock} from '@self/root/src/resolvers/checkout/mocks/resolveOrdersCashbackDetails';

const CASHBACK_AMOUNT = 300;
const FIRST_ORDER_ID = 11111;
const SECOND_ORDER_ID = 222222;
const DEFAULT_ORDERS = [{
    orderId: FIRST_ORDER_ID,
    items: [{
        skuId: 11,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: 111,
    deliveryType: 'POST',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
    },
    cashbackEmitInfo: {
        totalAmount: CASHBACK_AMOUNT,
        status: 'INIT',
    },
}, {
    orderId: SECOND_ORDER_ID,
    items: [{
        skuId: 11,
        count: 1,
        buyerPrice: 5,
    }],
    recipient: 111,
    deliveryType: 'POST',
    outletId: outletMock.id,
    currency: 'RUR',
    buyerCurrency: 'RUR',
    delivery: {
        buyerPrice: 100,
        dates: {
            fromDate: '10-10-2000',
            toDate: '15-10-2000',
            fromTime: '13:00',
            toTime: '19:00',
        },
    },
    cashbackEmitInfo: {
        totalAmount: CASHBACK_AMOUNT,
        status: 'INIT',
    },
}];

export default makeSuite('Мультизаказ с начислением кэшбэка', {
    feature: 'Кэшбэк',
    id: 'bluemarket-3727',
    environment: 'kadavr',
    issue: 'MARKETFRONT-14483',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                firstCashbackInfo: () => this.createPageObject(CashbackInfo, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.firstDelivery} ${CashbackInfo.root}`,
                }),
                secondCashbackInfo: () => this.createPageObject(CashbackInfo, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery} ${CashbackInfo.root}`,
                }),
            });

            await this.browser.yaScenario(
                this,
                'thank.prepareThankPage',
                {
                    region: 213,
                    orders: DEFAULT_ORDERS,
                },
                generateCashbackDetailsCollectionsMock({
                    totalAmount: CASHBACK_AMOUNT,
                    orders: DEFAULT_ORDERS.map(({orderId, cashbackEmitInfo}) => ({
                        orderId,
                        cashbackAmount: cashbackEmitInfo.totalAmount,
                    })),
                })
            );
        },
        'Блок с кэшбэком 1-го заказа отображается': makeCase({
            async test() {
                await this.orderConfirmation.firstDetailsClick();
                await this.firstCashbackInfo.isVisible()
                    .should.eventually.be.equal(true, 'Кэшбэк должен отображаться');
            },
        }),
        'Блок с кэшбэком 2-го заказа отображается': makeCase({
            async test() {
                await this.orderConfirmation.secondDetailsClick();
                await this.secondCashbackInfo.isVisible()
                    .should.eventually.be.equal(true, 'Кэшбэк должен отображаться');
            },
        }),
    },
});
