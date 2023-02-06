import {makeCase, makeSuite} from 'ginny';

import InnerPayment
    from '@self/root/src/components/InnerPayment/__pageObject';

import CARD from '@self/root/src/spec/hermione/configs/checkout/cards/valid';

module.exports = makeSuite('Новой картой', {
    story: {
        async beforeEach() {
            this.setPageObjects({
                innerPayment: () => this.createPageObject(InnerPayment),
            });
        },
        'Проходит успешно с действительной картой': makeCase({
            async test() {
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
                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" способ оплаты отображается "Новой картой"',
                    () => this.paymentOptionsBlock.getText()
                        .should.eventually.to.be.include(
                            'Новой картой',
                            'Должен быть выбран способ оплаты картой'
                        )
                );
                await this.browser.allure.runStep('Клик по кнопке "Перейти к оплате"',
                    async () => {
                        await this.orderButton.click();
                        await this.browser.allure.runStep('Ожидаем появления траста',
                            async () => {
                                await this.innerPayment.waitForVisibleTrust();
                                await this.innerPayment.switchToContentFrame();
                            });
                        await this.browser.allure.runStep('Вводим данные карты',
                            async () => {
                                await this.innerPayment.setCardNumber(CARD.cardNumber.join(''));
                                await this.innerPayment.setCardDate(CARD.month, CARD.year);
                                await this.innerPayment.setCardCVV(CARD.cvc);
                            });
                        await this.browser.allure.runStep('Нажать на кнопку "Оплатить"',
                            async () => {
                                await this.innerPayment.clickOnSubmitPaymentButton();
                            });
                    });

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
    },
});
