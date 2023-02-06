import {makeCase, makeSuite} from 'ginny';

import InnerPayment
    from '@self/root/src/components/InnerPayment/__pageObject';
// eslint-disable-next-line max-len
import Confirm from '@self/root/src/components/InnerPayment/components/Confirm/__pageObject';

import OrderConfirmation from '@self/root/src/spec/page-objects/OrderConfirmation';

import validCard from '@self/root/src/spec/hermione/configs/checkout/cards/valid';

module.exports = makeSuite('Сценарий отмены', {
    story: {
        async beforeEach() {
            this.setPageObjects({
                innerPayment: () => this.createPageObject(InnerPayment),
                orderConfirmation: () => this.createPageObject(OrderConfirmation),
                confirmPopup: () => this.createPageObject(Confirm),
            });

            await this.browser.allure.runStep(
                'В блоке "Способ оплаты" выбираем "Новой картой"',
                async () => {
                    await this.paymentOptionsEditableCard.changeButtonClick();
                    await this.editPopup.waitForVisibleRoot();
                    return this.browser.allure.runStep(
                        'Установить способ оплаты "Оплата картой онлайн"',
                        async () => {
                            await this.paymentOptions.setPaymentTypeYandex();
                            return this.paymentOptions.submitButtonClick();
                        }
                    );
                }
            );
            await this.browser.allure.runStep('Клик по кнопке "Перейти к оплате"',
                async () => {
                    await this.orderButton.click();
                    await this.browser.allure.runStep('Ожидаем появления траста',
                        () => this.innerPayment.waitForVisibleTrust()
                    );
                }
            );
            await this.browser.allure.runStep('Нажимаем "назад"', () => this.browser.back());
        },
        'Возвращает к оплате': makeCase({
            id: 'marketfront-4837',
            async test() {
                await this.confirmPopup.waitForVisible();
                await this.confirmPopup.clickBackToPaymentButton();
                await this.browser.allure.runStep('Ожидаем появления траста',
                    async () => {
                        await this.innerPayment.waitForVisibleTrust();
                        await this.innerPayment.switchToContentFrame();
                    });
                await this.browser.allure.runStep('Вводим данные карты',
                    async () => {
                        await this.innerPayment.setCardNumber(validCard.cardNumber.join(''));
                        await this.innerPayment.setCardDate(validCard.month, validCard.year);
                        await this.innerPayment.setCardCVV(validCard.cvc);
                    });
                await this.browser.allure.runStep('Нажать на кнопку "Оплатить"',
                    () => this.innerPayment.clickOnSubmitPaymentButton()
                );
                await this.browser.allure.runStep('Перешли на страницу оплаченного заказа',
                    async () => {
                        await this.browser.yaWaitForChangeUrl(null, 60000);
                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/my/orders/confirmation',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });

                        const header = await this.orderConfirmation.getTitle();
                        await this.expect(header).to.equal(
                            'Заказ оформлен!',
                            'Заголовок должен быть "Заказ оформлен!"'
                        );
                    }
                );
            },
        }),
        'Отменяет оплату': makeCase({
            id: 'marketfront-5219',
            async test() {
                await this.confirmPopup.waitForVisible();
                await this.browser.allure.runStep('Перешли на страницу неоплаченного заказа',
                    async () => {
                        await this.browser.yaWaitForChangeUrl(
                            () => this.confirmPopup.clickCancelPaymentButton(),
                            60000
                        );
                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/my/orders/confirmation',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });

                        const header = await this.orderConfirmation.getTitle();
                        await this.expect(header).to.equal(
                            'Заказ не оплачен',
                            'Заголовок должен быть "Заказ не оплачен"'
                        );
                    }
                );
            },
        }),
    },
});
