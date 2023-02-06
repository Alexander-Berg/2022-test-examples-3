import {makeCase, makeSuite} from 'ginny';

import CARD from '@self/root/src/spec/hermione/configs/checkout/cards/valid';
import {CARD_ID} from './constants';

module.exports = makeSuite('Существующая карта', {
    id: 'marketfront-4835',
    issue: 'MARKETFRONT-60509',
    story: {
        'Карта отображается в попапе выбора способов оплаты': makeCase({
            async test() {
                await this.browser.allure.runStep('Открываем попап выбора способов оплаты', async () => {
                    await this.paymentOptionsEditableCard.changeButtonClick();
                    await this.editPopup.waitForVisibleRoot();
                });
                await this.browser.allure.runStep(
                    'Проверяем отображение способа оплаты существующей картой',
                    () => this.paymentOptions.paymentTypeBankCard(CARD_ID).isVisible()
                );
                await this.browser.allure.runStep(
                    'Выбираем оплату существующей картой',
                    async () => {
                        await this.paymentOptions.setPaymentBankCard(CARD_ID);
                        return this.paymentOptions.submitButtonClick();
                    }
                );
                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" способ оплаты отображается выбранная карта',
                    () => this.paymentOptionsBlock.getText()
                        .should.eventually.to.be.match(
                            new RegExp(`••••\\s+${CARD.cardNumber[3]}`),
                            `Должна отображаться карта "•••• ${CARD.cardNumber[3]}"`
                        )
                );
            },
        }),
        'Карта выбирается по дефолту': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" способ оплаты отображается выбранная карта',
                    () => this.paymentOptionsBlock.getText()
                        .should.eventually.to.be.match(
                            new RegExp(`••••\\s+${CARD.cardNumber[3]}`),
                            `Должна отображаться карта "•••• ${CARD.cardNumber[3]}"`
                        )
                );
            },
        }),
        'Оплата существующей картой проходит успешно': makeCase({
            async test() {
                await this.browser.allure.runStep('Открываем попап выбора способов оплаты', async () => {
                    await this.paymentOptionsEditableCard.changeButtonClick();
                    await this.editPopup.waitForVisibleRoot();
                });
                await this.browser.allure.runStep(
                    'Проверяем отображение способа оплаты существующей картой',
                    () => this.paymentOptions.paymentTypeBankCard(CARD_ID).isVisible()
                );
                await this.browser.allure.runStep(
                    'Выбираем оплату существующей картой',
                    async () => {
                        await this.paymentOptions.setPaymentBankCard(CARD_ID);
                        return this.paymentOptions.submitButtonClick();
                    }
                );
                await this.browser.allure.runStep(
                    'Нажимаем на кнопку "Оплатить"',
                    () => this.orderButton.click()
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
    },
});
