import assert from 'assert';
import {makeSuite, makeCase} from 'ginny';


import {clearAll as clearAllCookies} from '@self/root/src/spec/hermione/scenarios/cookies';

import {
    prepareUnpaidOrderState,
    openEntryPointPage,
    checkPaymentButtonTextAndUrl,
} from '@self/root/src/spec/hermione/scenarios/unpaidOrder';
import {getEntryPointPagePath} from '@self/root/src/spec/utils/unpaidOrder';

import {ActionButton} from '@self/root/src/components/OrderActions/Actions/ActionButton/__pageObject';
import {OrderStatus} from '@self/root/src/components/OrderHeader/OrderStatus/__pageObject';


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
            assert(this.orderCard, 'PageObject.orderCard must be defined');

            this.setPageObjects({
                orderStatus: () => this.createPageObject(OrderStatus),
                orderPaymentButton: () => this.createPageObject(ActionButton, {
                    parent: this.orderCard,
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
            'отображается корректный статус заказа': makeCase({
                id: 'bluemarket-3255',
                issue: 'BLUEMARKET-9970',
                async test() {
                    await this.orderStatus.isVisible()
                        .should.eventually.be.equal(
                            true,
                            'Статус заказа должен отображаться'
                        );

                    return this.orderStatus.getText()
                        .should.eventually.be.equal(
                            'Осталось только оплатить',
                            'Текст статуса заказа должен быть: "Осталось только оплатить"'
                        );
                },
            }),
            'кнопка оплаты отображается и работает корректно': makeCase({
                id: 'bluemarket-3255',
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
