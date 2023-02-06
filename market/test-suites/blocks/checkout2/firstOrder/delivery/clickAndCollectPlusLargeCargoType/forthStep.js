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
import Text from '@self/root/src/uikit/components/Text/__pageObject';
import CheckoutSummary from '@self/root/src/components/CheckoutSummary/__pageObject';
import OrderTotal from '@self/root/src/components/OrderTotalV2/__pageObject';
import {
    offerMock as farmaOfferMock,
    outletMock as farmaOutletMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import {
    offerMock as largeCargoTypeOfferMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/largeCargoType';
import {LARGE_CARGO_TYPE_DELIVERY_PRICE} from './helpers';

export default makeSuite('Шаг 4.', {
    id: 'marketfront-4426',
    issue: 'MARKETFRONT-36074',
    feature: 'Шаг 4.',
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
                    hasOtherParcel: true,
                }
            );

            await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});
            this.setPageObjects({
                largeCargoTypeAddressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                    root: `${GroupedParcel.root}:nth-child(1)`,
                }),
                largeCargoTypeAddressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.largeCargoTypeAddressBlock,
                }),
                largeCargoTypeDeliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.largeCargoTypeAddressEditableCard,
                }),
                largeCargoTypeAddressCard: () => this.createPageObject(AddressCard, {
                    parent: this.largeCargoTypeDeliveryInfo,
                }),
                clickAndCollectAddressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.confirmationPage,
                    root: `${GroupedParcel.root}:nth-child(2)`,
                }),
                clickAndCollectAddressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.clickAndCollectAddressBlock,
                }),
                clickAndCollectDeliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.clickAndCollectAddressEditableCard,
                }),
                clickAndCollectOutletCard: () => this.createPageObject(Text, {
                    parent: this.clickAndCollectDeliveryInfo,
                }),
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                summary: () => this.createPageObject(CheckoutSummary, {
                    parent: this.confirmationPage,
                }),
                orderTotal: () => this.createPageObject(OrderTotal, {
                    parent: this.summary,
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
                    'Блок "Доставка" для КГТ.',
                    async () => {
                        const testAddress = 'Москва, Красная площадь, д. 1';

                        await this.largeCargoTypeAddressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.largeCargoTypeAddressCard.getText()
                            .should.eventually.to.be.equal(
                                testAddress,
                                'На карточке адреса доставки должены быть указанные пользователем данные.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Доставка" для C&C.',
                    async () => {
                        await this.clickAndCollectAddressEditableCard.getTitle()
                            .should.eventually.include(
                                'Самовывоз',
                                'Текст заголовка должен содержать "Самовывоз".'
                            );

                        await this.clickAndCollectOutletCard.getText()
                            .should.eventually.include(
                                farmaOutletMock.address.fullAddress,
                                'На карточке адреса должна отображаться информация о выбранном ПВЗ'
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
                                'На карточке способа оплаты должна отображатся кнопка "Изменить" и быть активной.'
                            );
                    }
                );
            },
        }),
        'Открыть попап "Способ оплаты".': {
            async beforeEach() {
                await this.browser.allure.runStep(
                    'Нажать кнопку "Изменить".',
                    async () => {
                        await this.paymentOptionsEditableCard.changeButtonClick();
                        await this.paymentOptionsModal.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Способ оплаты "Картой онлайн" отображается активным.',
                    async () => {
                        await this.paymentOptionsPopUpContent.isPaymentTypeYandexDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Пункт "Картой онлайн" должен быть доступен для выбора');
                    }
                );

                await this.browser.allure.runStep(
                    'Выбрать способ оплаты "Картой онлайн".',
                    async () => {
                        await this.paymentOptionsPopUpContent.setPaymentTypeYandex();
                        await this.paymentOptionsPopUpContent.submitButtonClick();

                        await this.paymentOptionsModal.waitForInvisible();
                    }
                );
            },
            'В саммари должна отображаться информация о ранее выбранных товарах.': makeCase({
                async test() {
                    const ITEMS_COUNT = 2;
                    const totalItemsPrice = {
                        value: Number(largeCargoTypeOfferMock.prices.value) + Number(farmaOfferMock.prices.value),
                        currency: 'RUR',
                    };
                    const deliveryPrice = {
                        value: LARGE_CARGO_TYPE_DELIVERY_PRICE,
                        currency: 'RUR',
                    };
                    const totalPrice = {
                        value:
                            Number(largeCargoTypeOfferMock.prices.value) +
                            Number(farmaOfferMock.prices.value) +
                            LARGE_CARGO_TYPE_DELIVERY_PRICE,
                        currency: 'RUR',
                    };

                    await this.browser.allure.runStep(
                        'В саммари должно отображаться количество товаров 2 шт.',
                        async () => {
                            await this.orderTotal.getItemsCount()
                                .should.eventually.equal(
                                    ITEMS_COUNT,
                                    'В саммари должно отображаться количество товаров "2"'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'В саммари должна отображаться корректная стоимость товаров.',
                        async () => {
                            await this.orderTotal.getItemsValue()
                                .should.eventually.equal(
                                    totalItemsPrice.value,
                                    `В саммари должна отображаться стоимость товаров "${totalItemsPrice.value}"`
                                );
                        }
                    );

                    await this.orderTotal.isDeliveryVisible()
                        .should.eventually.to.be.equal(true, 'Должен отображаться список доставки');

                    await this.browser.allure.runStep(
                        'В саммари должна отображаться общая стоимость доставки.',
                        async () => {
                            await this.orderTotal.getTotalDeliveryPriceValue()
                                .should.eventually.equal(
                                    deliveryPrice.value,
                                    'В саммари должна корректно отображаться общая стоимость доставки'
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'В саммари должнен отображаться блок "Итого".',
                        async () => {
                            await this.orderTotal.getPriceValue()
                                .should.eventually.be.equal(
                                    totalPrice.value,
                                    'В саммари должнен отображаться блок "Итого"'
                                );
                        }
                    );
                },
            }),
            'Нажать кнопку "Перейти к оплате".': makeCase({
                async test() {
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
                        'Ожидаем изменения урла на: "/my/orders/payment".',
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
                                    pathname: '/my/orders/payment',
                                }, {
                                    mode: 'match',
                                    skipProtocol: true,
                                    skipHostname: true,
                                });
                        }
                    );
                },
            }),
        },
    },
});
