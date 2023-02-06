import {makeSuite, makeCase} from 'ginny';

// pageObjects
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';
// scenario
import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
// constants
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_TYPE, PAYMENT_METHOD} from '@self/root/src/entities/payment';
import {PAYMENT_STATUS} from '@self/root/src/entities/payment/statuses';
import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';

const CASHBACK_AMOUNT = 300;
const DEFAULT_AMOUNT = 1;
const CASHBACK_SPEND_TEXT = `Оплата плюсом ${CASHBACK_AMOUNT}`;
const DEFAULT_SPEND_TEXT = `Оплата картой ${DEFAULT_AMOUNT}`;
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
    payment: {
        paymentType: 'PREPAID',
        partitions: [{
            paymentAgent: 'YANDEX_CASHBACK',
            totalAmount: CASHBACK_AMOUNT,
        }, {
            paymentAgent: 'DEFAULT',
            totalAmount: DEFAULT_AMOUNT,
        }],
        status: PAYMENT_STATUS.CLEARED,
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
    payment: {
        paymentType: 'PREPAID',
        partitions: [{
            paymentAgent: 'YANDEX_CASHBACK',
            totalAmount: CASHBACK_AMOUNT,
        }, {
            paymentAgent: 'DEFAULT',
            totalAmount: DEFAULT_AMOUNT,
        }],
        status: PAYMENT_STATUS.CLEARED,
    },
}];

export default makeSuite('Мультизаказ со списанием кэшбэка', {
    feature: 'Кэшбэк',
    id: 'bluemarket-3728',
    environment: 'kadavr',
    issue: 'MARKETFRONT-14483',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                firstOrderDetails: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.firstDelivery}`,
                }),
                secondOrderDetails: () => this.createPageObject(OrderConfirmation, {
                    parent: this.confirmationPage,
                    root: `${OrderConfirmation.secondDelivery}`,
                }),
            });
            await this.browser.yaScenario(this, prepareThankPage, {
                region: 213,
                orders: DEFAULT_ORDERS,
                paymentOptions: {
                    paymentType: PAYMENT_TYPE.PREPAID,
                    paymentMethod: PAYMENT_METHOD.YANDEX,
                    status: ORDER_STATUS.PENDING,
                },
            });
        },
        'Блок с кэшбэком 1-го заказа отображается': makeCase({
            async test() {
                await this.firstOrderDetails.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Блок со способами оплаты должен отображаться'
                    );
            },
        }),
        'Содержит текст с правильными способами оплаты для 1-го заказа': makeCase({
            async test() {
                await this.firstOrderDetails.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        CASHBACK_SPEND_TEXT,
                        `Текст должен сожержать ${CASHBACK_SPEND_TEXT}`
                    );

                await this.firstOrderDetails.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        DEFAULT_SPEND_TEXT,
                        `Текст должен содержать ${DEFAULT_SPEND_TEXT}`
                    );
            },
        }),
        'Блок с кэшбэком 2-го заказа отображается': makeCase({
            async test() {
                await this.secondOrderDetails.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Блок со способами оплаты должен отображаться'
                    );
            },
        }),
        'Содержит текст с правильными способами оплаты 2-го заказа': makeCase({
            async test() {
                await this.secondOrderDetails.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        CASHBACK_SPEND_TEXT,
                        `Текст должен сожержать ${CASHBACK_SPEND_TEXT}`
                    );

                await this.secondOrderDetails.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        DEFAULT_SPEND_TEXT,
                        `Текст должен содержать ${DEFAULT_SPEND_TEXT}`
                    );
            },
        }),
    },
});
