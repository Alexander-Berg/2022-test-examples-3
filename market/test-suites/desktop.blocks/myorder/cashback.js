import {path} from 'ambar';
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import OrderSection from '@self/root/src/components/Orders/MyOrderSection/__pageObject';
import OrderDetailsRow from '@self/root/src/components/OrderDetailsRow/__pageObject';
import CashbackInfo from '@self/root/src/components/CashbackInfos/CashbackInfo/__pageObject';
import {prepareOrder} from '@self/root/src/spec/hermione/scenarios/checkoutResource';
import checkoutItemIds from '@self/root/src/spec/hermione/configs/checkout/items';
import {PAYMENT_AGENT} from '@self/root/src/entities/paymentPartition';
import {ORDER_CASHBACK_EMIT_STATUS} from '@self/root/src/entities/orderCashback';

const {
    makeCase,
    makeSuite,
    mergeSuites,
} = require('ginny');

const DEFAULT_ORDER = {
    items: [{
        skuId: checkoutItemIds.asus.skuId,
    }],
    deliveryType: 'DELIVERY',
};

/**
 * Тесты на кешбек заказа.
 */
module.exports = makeSuite('Кешбек в заказе.', {
    feature: 'Кешбек',
    environment: 'kadavr',
    issue: 'MARKETFRONT-23992',
    story: mergeSuites(
        makeSuite('Начисление кешбека.', {
            params: {
                loyaltyCashbackEmitAmount: 'Кол-во начисленных за заказ баллов кешбека от лоялти',
                loyaltyCashbackEmitStatus: 'Статус начисления баллов за заказ от лоялти',
                checkouterCashbackEmitAmount: 'Кол-во начисленных за заказ баллов кешбека от чекаутера',
                checkouterCashbackEmitStatus: 'Статус начисления баллов за заказ от чекаутера',
            },
            story: {
                beforeEach() {
                    this.setPageObjects({
                        orderCashbackSection: () => this.createPageObject(OrderSection, {
                            root: `${OrderSection.root}[data-auto="order-section-cashback"]`,
                        }),
                        cashbackDetailsRow: () => this.createPageObject(OrderDetailsRow, {
                            parent: this.orderCashbackSection,
                        }),
                        cashbackInfo: () => this.createPageObject(CashbackInfo, {
                            parent: this.cashbackDetailsRow,
                        }),
                    });

                    return prepareOrderPage.call(
                        this,
                        DEFAULT_ORDER,
                        {
                            amount: this.params.loyaltyCashbackEmitAmount,
                            status: this.params.loyaltyCashbackEmitStatus,
                        },
                        {
                            emitAmount: this.params.checkouterCashbackEmitAmount,
                            emitStatus: this.params.checkouterCashbackEmitStatus,
                        }
                    );
                },

                'За заказ с еще не начисленными баллами отображается корректно': makeCase({
                    id: 'bluemarket-3644',
                    defaultParams: {
                        loyaltyCashbackEmitAmount: 100,
                        loyaltyCashbackEmitStatus: ORDER_CASHBACK_EMIT_STATUS.PENDING,
                        checkouterCashbackEmitAmount: 100,
                        checkouterCashbackEmitStatus: ORDER_CASHBACK_EMIT_STATUS.PENDING,
                    },
                    test() {
                        return checkCashbackEmit.call(this, 'Вернется баллами:');
                    },
                }),

                'За заказ с уже начисленными баллами отображается корректно': makeCase({
                    id: 'bluemarket-3645',
                    defaultParams: {
                        loyaltyCashbackEmitAmount: 100,
                        loyaltyCashbackEmitStatus: ORDER_CASHBACK_EMIT_STATUS.SUCCESS,
                        checkouterCashbackEmitAmount: 100,
                        checkouterCashbackEmitStatus: ORDER_CASHBACK_EMIT_STATUS.SUCCESS,
                    },
                    async test() {
                        return checkCashbackEmit.call(this, 'Начислено:');
                    },
                }),
            },
        }),
        makeSuite('Списание кешбека', {
            params: {
                cashbackSpendAmount: 'Кол-во начисленных за заказ баллов кешбека',
            },
            story: {
                beforeEach() {
                    this.setPageObjects({
                        orderDetailsPrice: () =>
                            this.createPageObject(OrderSection, {
                                parent: this.myOrder,
                                root: `${OrderSection.root}[data-auto="order-details-price"]`,
                            }),
                        cashbackPaymentRow: () => this.createPageObject(OrderDetailsRow, {
                            parent: this.orderDetailsPrice,
                            root: `${OrderDetailsRow.root}[data-auto="order-payment-cashback"]`,
                        }),
                    });

                    return prepareOrderPage.call(this, {
                        ...DEFAULT_ORDER,
                        payment: {
                            partitions: [
                                {
                                    paymentAgent: PAYMENT_AGENT.DEFAULT,
                                    totalAmount: 100,
                                },
                                {
                                    paymentAgent: PAYMENT_AGENT.YANDEX_CASHBACK,
                                    totalAmount: this.params.cashbackSpendAmount,
                                },
                            ],
                        },
                    });
                },
                'за заказ в любом статусе отображается корректно': makeCase({
                    id: 'bluemarket-3654',
                    defaultParams: {
                        cashbackSpendAmount: 150,
                    },
                    async test() {
                        await this.cashbackPaymentRow.isVisible()
                            .should.eventually.to.be.equal(true, 'Строка об оплате кешбеком должна отображаться');

                        await this.cashbackPaymentRow.getTitle()
                            .should.eventually.to.be.equal(
                                'Баллами Плюса',
                                'Заголовок строки об оплате кешбеком должен быть "Баллами Плюса"'
                            );

                        return this.cashbackPaymentRow.getContent()
                            .should.eventually.to.be.equal(
                                `${this.params.cashbackSpendAmount} ₽`,
                                `Контент строки об оплате кешбеком должен быть "${this.params.cashbackSpendAmount} ₽"`
                            );
                    },
                }),
            },
        })
    ),
});

async function checkCashbackEmit(expectedRowTitle) {
    await this.orderCashbackSection.waitForVisible();

    await this.orderCashbackSection.getTitle()
        .should.eventually.to.be.equal(
            'Яндекс.Плюс',
            'Загловок секции заказа должен быть "Яндекс.Плюс"');

    await this.cashbackDetailsRow.getTitle()
        .should.eventually.to.be.equal(
            expectedRowTitle,
            `Заголовок строки о кешбеке должен быть "${expectedRowTitle}"`
        );

    const emitAmount = this.params.loyaltyCashbackEmitAmount + this.params.checkouterCashbackEmitAmount;

    return this.cashbackInfo.getCashbackText()
        .should.eventually.to.be.equal(
            `${emitAmount}`,
            `Количество баллов кешбека должно быть ${emitAmount}`
        );
}

async function prepareOrderPage(order, loyaltyOrderCashback, checkouterOrderCashback) {
    const response = await this.browser.yaScenario(
        this,
        prepareOrder,
        {
            region: this.params.region,
            status: 'DELIVERY',
            orders: [order],
            paymentType: 'PREPAID',
            paymentMethod: 'CASH_ON_DELIVERY',
        }
    );

    const orderId = path(['orders', 0, 'id'], response);

    await this.browser.setState('Loyalty.collections.ordersCashback', [{
        orderId,
        ...loyaltyOrderCashback,
    }]);

    await this.browser.setState('Checkouter.collections.ordersCashbackInfo', [{
        orderId,
        ...checkouterOrderCashback,
    }]);

    if (this.params.pageId === PAGE_IDS_COMMON.ORDER) {
        return this.browser.yaOpenPage(this.params.pageId, {orderId});
    }

    return this.browser.yaOpenPage(this.params.pageId);
}
