import {
    makeSuite,
    makeCase,
} from 'ginny';
import assert from 'assert';

import OrderPayment from '@self/root/src/widgets/parts/Payment/components/View/__pageObject';

const paymentStatusTimeout = OrderPayment.paymentStatusTimeout;

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Предоплатный неоплаченный заказ успешно оформлен.', {
    environment: 'kadavr',
    id: 'bluemarket-3335',
    issue: 'BLUEMARKET-10196',
    feature: 'Спасибо за заказ',
    story: {
        async beforeEach() {
            assert(this.orderConfirmation, 'PageObject.OrderConfirmation must be defined');

            await this.orderConfirmation.waitForCheckoutThankyouIsVisible(paymentStatusTimeout);
        },

        'Заголовок страницы отображается корректно': makeCase({
            async test() {
                await this.orderConfirmation.isOrdersTitleVisible()
                    .should.eventually.be.equal(
                        true,
                        'Заголовок страницы должен отображаться'
                    );

                const title = 'Заказ не оплачен';

                return this.orderConfirmation.getOrdersTitle()
                    .should.eventually.be.equal(
                        title,
                        `Заголовок страницы должен быть "${title}"`
                    );
            },
        }),

        'Информация про срок оплаты отображается корректно': makeCase({
            async test() {
                await this.orderConfirmation.isOrdersInfoTextVisible()
                    .should.eventually.be.equal(
                        true,
                        'Информационный текст должен быть виден'
                    );

                const subtitle = 'Оплатите заказ в течение 30 минут,\n' +
                    'иначе его придётся отменить';

                return this.orderConfirmation.getOrdersInfoText()
                    .should.eventually.be.equal(
                        subtitle,
                        `Информационный текст должен быть "${subtitle}"`
                    );
            },
        }),

        'Кнопка "Оплатить" отображается и работает корректно': makeCase({
            async test() {
                await this.orderConfirmation.isPaymentLinkVisible()
                    .should.eventually.be.equal(
                        true,
                        'Кнопка оплаты заказа должна быть видна'
                    );

                const paymentButtonText = 'Оплатить';

                await this.orderConfirmation.getPaymentLinkText()
                    .should.eventually.be.equal(
                        paymentButtonText,
                        `Текст кнопки оплаты заказа должен быть "${paymentButtonText}"`
                    );

                const currentUrl = await this.browser.yaWaitForChangeUrl(() =>
                    this.orderConfirmation.clickPaymentLink()
                );

                return this.expect(currentUrl).be.link({
                    pathname: '/my/orders',
                }, {
                    skipProtocol: true,
                    skipHostname: true,
                });
            },
        }),

        'Текст "Не оплачено" присутствует в блоке состава заказа': makeCase({
            async test() {
                await this.orderConfirmation.isOrderPaymentStatusVisible()
                    .should.eventually.be.equal(
                        true,
                        'Статус заказа в блоке состава заказа должен быть виден'
                    );

                const paymentText = 'Не оплачено';

                await this.orderConfirmation.getOrderPaymentStatusText()
                    .should.eventually.be.equal(
                        paymentText,
                        `Текст статуса заказа в блоке состава заказа должен быть "${paymentText}"`
                    );
            },
        }),
    },
});
