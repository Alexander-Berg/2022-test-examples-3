import assert from 'assert';
import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import PaymentMethodChange from '@self/root/src/widgets/content/PaymentMethodChange/components/View/__pageObject';
import OrderPayment from '@self/root/src/widgets/parts/Payment/components/View/__pageObject';
import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';

import unpaidOrderPaymentProcess from '@self/root/src/spec/hermione/test-suites/blocks/unpaidOrder/paymentProcess';


/**
 * Тесты на оплату уже оформленного, но неоплаченного заказа с предоплатным типом оплаты
 *
 * @param {PageObject.OrderPaymentButton} orderPaymentButton
 * @param pageId
 */
module.exports = makeSuite('Дооплата.', {
    environment: 'kadavr',
    feature: 'Дооплата',
    params: {
        pageId: 'Идентификатор страницы',
        paymentButtonText: 'Текст кнопки оплаты заказа',
    },
    defaultParams: {
        isAuthWithPlugin: true,
        paymentButtonText: 'Оплатить заказ',
    },
    story: mergeSuites(
        {
            beforeEach() {
                assert(this.params.pageId, 'Param pageId must be defined');
                assert(this.orderPaymentButton, 'Param PageObject OrderPaymentButton must be defined');
            },
        },
        prepareSuite(unpaidOrderPaymentProcess, {
            suiteName: ' ', // во избежание дублирования слова "Дооплата" в заголовке
            pageObjects: {
                paymentMethodChange() {
                    return this.createPageObject(PaymentMethodChange);
                },
                orderPayment() {
                    return this.createPageObject(OrderPayment);
                },
                orderConfirmation() {
                    return this.createPageObject(OrderConfirmation);
                },
            },
        })
    ),
});
