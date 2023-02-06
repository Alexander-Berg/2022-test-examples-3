import {makeSuite, makeCase} from 'ginny';

// scenario
import {prepareThankPage} from '@self/root/src/spec/hermione/scenarios/thank';
// constants
import {outlet1 as outletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import {PAYMENT_STATUS} from '@self/root/src/entities/payment/statuses';
import {PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {PAYMENT_METHODS} from '@self/root/src/entities/payment/constants';

const CASHBACK_AMOUNT = 300;
const DEFAULT_AMOUNT = 1;
const CASHBACK_SPEND_TEXT = `Оплата плюсом ${CASHBACK_AMOUNT}`;
const DEFAULT_SPEND_TEXT = `Оплата картой ${DEFAULT_AMOUNT}`;
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
    payment: {
        paymentType: PAYMENT_TYPE.PREPAID,
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

export default makeSuite('Заказ со списанием кэшбэка', {
    feature: 'Кэшбэк',
    id: 'bluemarket-3655',
    environment: 'kadavr',
    issue: 'MARKETFRONT-14483',
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            await this.browser.yaScenario(this, prepareThankPage, {
                region: 213,
                orders: DEFAULT_ORDERS,
                paymentOptions: {
                    paymentType: PAYMENT_TYPE.PREPAID,
                    paymentMethod: PAYMENT_METHODS.YANDEX,
                    status: PAYMENT_STATUS.CLEARED,
                },
            });
        },
        'Блок с кэшбэком отображается': makeCase({
            async test() {
                await this.orderConfirmation.orderPaymentStatus.isExisting()
                    .should.eventually.to.be.equal(
                        true,
                        'Блок со способами оплаты должен отображаться'
                    );
            },
        }),
        'Содержит текст с правильными способами оплаты': makeCase({
            async test() {
                await this.orderConfirmation.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        CASHBACK_SPEND_TEXT,
                        `Текст должен сожержать ${CASHBACK_SPEND_TEXT}`
                    );

                await this.orderConfirmation.getOrderPaymentStatusText()
                    .should.eventually.to.be.include(
                        DEFAULT_SPEND_TEXT,
                        `Текст должен содержать ${DEFAULT_SPEND_TEXT}`
                    );
            },
        }),
        'Содержит корректный способ оплаты в меню "Подробности"': makeCase({
            async test() {
                await this.orderConfirmation.detailsClick();

                await this.orderConfirmation.getDeliveryPaymentMethodText()
                    .should.eventually.include(
                        'и баллами Плюса',
                        'Способ оплаты должен содержать "и баллами Плюса"'
                    );
            },
        }),
    },
});
