import assert from 'assert';
import {makeSuite, makeCase} from 'ginny';


import {clearAll as clearAllCookies} from '@self/root/src/spec/hermione/scenarios/cookies';

import {
    prepareUnpaidOrderState,
    openEntryPointPage,
    checkPaymentButtonTextAndUrl,
} from '@self/root/src/spec/hermione/scenarios/unpaidOrder';
import {getEntryPointPagePath} from '@self/root/src/spec/utils/unpaidOrder';

import OrderStatusNotifierPaymentButton from
    '@self/root/src/components/Orders/OrderStatusNotifier/OrderStatusNotifierPaymentButton/__pageObject';
import {OrderStatusNotifier} from '@self/root/src/components/Orders/OrderStatusNotifier/__pageObject';
import OrderStatus from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';


/**
 * Тесты на проверку отображения неоплаченного заказа
 *
 * @param {PageObject.OrderCard} orderCard
 * @param pageId
 */
module.exports = makeSuite('Информация о неоплаченном заказе.', {
    environment: 'kadavr',
    feature: 'Дооплата',
    params: {
        pageId: 'Идентификатор страницы',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            assert(this.params.pageId, 'Param pageId must be defined');
            assert(this.myOrder, 'PageObject.myOrder must be defined');

            this.setPageObjects({
                orderStatus: () => this.createPageObject(OrderStatus, {
                    parent: this.myOrder,
                }),
                orderStatusNotifier: () => this.createPageObject(OrderStatusNotifier, {
                    parent: this.myOrder,
                }),
                orderPaymentButton: () => this.createPageObject(OrderStatusNotifierPaymentButton, {
                    parent: this.myOrder,
                }),
            });

            const orderId = 1;
            const {pageId} = this.params;

            const entryPointPath = await getEntryPointPagePath.call(this, {
                orderId,
                pageId,
            });

            this.yaTestData = {
                orderId,
                entryPointPath,
            };

            await preparePage.call(this);
        },

        async afterEach() {
            await this.browser.yaScenario(this, clearAllCookies);
        },

        'Если заказ не оплачен,': {
            'отображается статус "Ещё не оплачен"': makeCase({
                id: 'bluemarket-3254',
                issue: 'BLUEMARKET-9970',
                async test() {
                    await this.orderStatus.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Статус заказа должен отображаться'
                        );

                    const status = 'Осталось только оплатить';

                    return this.orderStatus.getStatusText()
                        .should.eventually.be.equal(
                            status,
                            `Текст статуса заказа должен быть "${status}"`
                        );
                },
            }),
            'корректно отображается время для оплаты': makeCase({
                id: 'bluemarket-3254',
                issue: 'BLUEMARKET-9970',
                async test() {
                    await this.orderStatusNotifier.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Уведомление об оплате заказа должно отображаться'
                        );

                    await this.orderStatusNotifier.isStatusVisible()
                        .should.eventually.be.equal(
                            true,
                            'Статус оплаты заказа в уведомлении должен отображаться'
                        );

                    const status = 'Заказ не оплачен';

                    await this.orderStatusNotifier.getStatusText()
                        .should.eventually.to.be.equal(
                            status,
                            `Текст статуса заказа в уведомлении должен быть "${status}"`
                        );

                    await this.orderStatusNotifier.isRemainingTimeVisible()
                        .should.eventually.be.equal(
                            true,
                            'Время до оплаты заказа должно отображаться '
                        );

                    return this.orderStatusNotifier.getRemainingTimeText()
                        .should.eventually.match(
                            /Время для оплаты истекает через \d+ часа/,
                            'Текст времени до оплаты должен быть вида: "Время для оплаты истекает через n часа"'
                        );
                },
            }),
            'кнопка оплаты отображается и работает корректно': makeCase({
                id: 'bluemarket-3254',
                issue: 'BLUEMARKET-9970',
                async test() {
                    const {orderId, entryPointPath} = this.yaTestData;

                    return this.browser.yaScenario(
                        this,
                        checkPaymentButtonTextAndUrl,
                        {
                            orderId,
                            entryPointPath,
                            paymentButtonText: 'Оплатить заказ',
                        }
                    );
                },
            }),
        },
    },
});

async function preparePage() {
    const {orderId} = this.yaTestData;
    const {pageId} = this.params;

    await this.browser.yaScenario(
        this,
        prepareUnpaidOrderState,
        {orderId}
    );

    return this.browser.yaScenario(
        this,
        openEntryPointPage,
        {
            orderId,
            pageId,
        }
    );
}
