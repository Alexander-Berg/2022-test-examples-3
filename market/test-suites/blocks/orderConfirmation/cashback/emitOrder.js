import {makeSuite, makeCase} from 'ginny';

import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import YaPlusIconPageObject from '@self/root/src/components/YaPlusIcon/__pageObject';
import {generateCashbackDetailsCollectionsMock} from '@self/root/src/resolvers/checkout/mocks/resolveOrdersCashbackDetails';
import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';

const CASHBACK_AMOUNT = 300;
const ORDER_ID = 11111;
const DEFAULT_ORDERS = [{
    orderId: ORDER_ID,
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

export default makeSuite('Заказ с начислением кэшбэка', {
    feature: 'Кэшбэк',
    id: 'bluemarket-3649',
    environment: 'kadavr',
    issue: 'MARKETFRONT-14483',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                cashbackInfo: () => this.createPageObject(CashbackInfo, {
                    parent: this.orderConfirmation,
                }),
                yaPlusIcon: () => this.createPageObject(YaPlusIconPageObject, {
                    parent: this.cashbackInfo,
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
                    orders: [{cashbackAmount: CASHBACK_AMOUNT, orderId: ORDER_ID}],
                })
            );

            // Открываем меню "Подробности".
            await this.orderConfirmation.detailsClick();
        },
        'Содержит корректный способ оплаты в меню "Подробности"': makeCase({
            async test() {
                await this.orderConfirmation.getDeliveryPaymentMethodText()
                    .should.not.eventually.include(
                        'и баллами Плюса',
                        'В способе оплаты нет упоминания баллов Плюса'
                    );
            },
        }),
        'Блок с кешбэком отображается': makeCase({
            async test() {
                await this.cashbackInfo.isVisible()
                    .should.eventually.be.equal(true, 'Кешбэк должен отображаться');
            },
        }),
        'Бейдж кэшбэка отображается': makeCase({
            async test() {
                await this.yaPlusIcon.isExisting()
                    .should.eventually.to.be.equal(true, 'Иконка Я+ должна отображаться');
            },
        }),
    },
});
