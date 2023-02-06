import {makeCase, makeSuite} from 'ginny';

import GroupedParcel from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import CheckoutRecipient from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject/index.js';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';

import EditableCard from '@self/root/src/components/EditableCard/__pageObject/index.desktop.js';
import Modal from '@self/root/src/components/PopupBase/__pageObject';

import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';


import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';
import {ACTUALIZATION_TIMEOUT} from '@self/root/src/spec/hermione/scenarios/checkout';

import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';


export default makeSuite('Оформление первого заказа. Шаг 3.', {
    id: 'marketfront-4425',
    issue: 'MARKETFRONT-45602',
    feature: 'Оформление первого заказа. Шаг 3',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            const formName = 'user-postpaid';

            await this.browser.yaScenario(
                this,
                goToConfirmationPage,
                {
                    userFormData,
                    formName,
                }
            );

            await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});

            this.setPageObjects({
                addressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                }),
                addressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.addressBlock,
                }),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.addressEditableCard,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                    parent: this.confirmationPage,
                }),
                paymentOptionsEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.paymentOptionsBlock,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal),
                paymentOptionsPopUpContent: () => this.createPageObject(PaymentOptionsList),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                    parent: this.confirmationPage,
                }),
            });
        },
        'Отображаются ранее заполненные данные.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Доставка".',
                    async () => {
                        const testAddress = 'Москва, Красная площадь, д. 1';

                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                testAddress,
                                'На карточке адреса доставки должены быть указанные пользователем данные.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Получатель".',
                    async () => {
                        const {recipient} = userFormData;

                        await this.recipientBlock.getContactText()
                            .should.eventually.to.be.equal(
                                `${recipient.name}\n${recipient.email}, ${recipient.phone}`,
                                'На карточке получателя должны быть указанные пользователем данные'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Способ оплаты".',
                    async () => {
                        await this.paymentOptionsEditableCard.isChangeButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'На карточке способа оплаты должна отображаться кнопка "Изменить" и быть активной.'
                            );
                    }
                );
            },
        }),
        'Выбрать способоплаты "Наличными при получении" и нажать кнопку "Подтвердить заказ".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Открыть попап "Способ оплаты".',
                    async () => {
                        await this.browser.allure.runStep(
                            'Нажать кнопку "Изменить".',
                            async () => {
                                await this.paymentOptionsEditableCard.changeButtonClick();
                                await this.paymentOptionsModal.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Способ оплаты "Наличными при получении" отображается активным.',
                            async () => {
                                await this.paymentOptionsPopUpContent.isPaymentTypeCashOnDeliveryDisabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Пункт "Наличными при получении" должен отображаться активным.');
                            }
                        );

                        await this.browser.allure.runStep(
                            'Выбрать способ оплаты "Наличными при получении".',
                            async () => {
                                await this.paymentOptionsPopUpContent.setPaymentTypeCashOnDelivery();
                                await this.paymentOptionsPopUpContent.submitButtonClick();

                                await this.paymentOptionsModal.waitForInvisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" отображается способ оплаты "Наличными при получении".',
                    async () => {
                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображатся способ оплаты "Наличными при получении".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Подтвердить заказ".',
                    async () => {
                        await this.browser.allure.runStep(
                            'Кнопка "Подтвердить заказ" отображается активной.',
                            async () => {
                                await this.checkoutOrderButton.waitForEnabledButton();
                                await this.checkoutOrderButton.isButtonDisabled()
                                    .should.eventually.to.be.equal(
                                        false,
                                        'Кнопка "Подтвердить заказ" дожна быть активна'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем изменения урла на: "my/orders/confirmation".',
                            async () => {
                                await this.browser.yaWaitForChangeUrl(
                                    async () => {
                                        await this.checkoutOrderButton.click();
                                    },
                                    ACTUALIZATION_TIMEOUT
                                );

                                await this.browser.getUrl()
                                    .should.eventually.to.be.link({
                                        query: {
                                            orderId: /\d+/,
                                        },
                                        pathname: 'my/orders/confirmation',
                                    }, {
                                        mode: 'match',
                                        skipProtocol: true,
                                        skipHostname: true,
                                    });
                            }
                        );
                    }
                );
            },
        }),
    },
});
