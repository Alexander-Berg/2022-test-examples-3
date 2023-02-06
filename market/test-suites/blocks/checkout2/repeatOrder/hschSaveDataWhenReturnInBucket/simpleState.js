import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    goToTypeNewAddressRepeatOrder,
    fillAllDeliveryAddressFields,
    waitCallingUserLastState,
    switchToSpecifiedDeliveryForm,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import {SelectPopover} from '@self/root/src/components/Select/__pageObject';
import {DeliveryIntervals} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryIntervals/__pageObject';
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @start
 */
import {
    DateSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DateIntervalSelector/__pageObject';

import {
    TimeSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/TimeIntervalSelector/__pageObject';
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @end
 */
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('ХСЧ: сохранение данных при оформлении обычного товара.', {
    id: 'marketfront-4894',
    issue: 'MARKETFRONT-50680',
    feature: 'ХСЧ: сохранение данных при оформлении обычного товара.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                popupBase: () => this.createPageObject(PopupBase),
                editPopup: () => this.createPageObject(EditPopup),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                recipientList: () => this.createPageObject(RecipientList),
                recepientForm: () => this.createPageObject(RecipientForm),
                recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                    parent: this.recipientForm,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal),
                paymentOptionsPopUpContent: () => this.createPageObject(PaymentOptionsList),
                /**
                 * @ifLose удалить использования и заменить на соответствующие обращения в deliveryIntervals
                 * @expFlag all_checkout_new_intervals [обратный эксперимент]
                 * @ticket MARKETFRONT-58113
                 * @start
                 */
                dateSelect: () => this.createPageObject(DateSelect),
                timeSelect: () => this.createPageObject(TimeSelect),
                /**
                 * @expFlag all_checkout_new_intervals [обратный эксперимент]
                 * @ticket MARKETFRONT-58113
                 * @end
                 */
                deliveryIntervals: () => this.createPageObject(DeliveryIntervals),
                intervalSelectPopover: () => this.createPageObject(SelectPopover),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
            });

            const carts = [
                buildCheckouterBucket({
                    cartIndex: 0,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );

            await this.browser.yaScenario(
                this,
                goToTypeNewAddressRepeatOrder
            );
        },
        'Сохранение данных получателя при оформлении обычного товара.': makeCase({
            async test() {
                const titleText = 'Как доставить заказ?';

                await this.allure.runStep(
                    'На экране "Как доставить заказ?"', async () => {
                        await this.deliveryTypes.waitForVisible();
                        await this.allure.runStep(
                            `Текст заголовка "${titleText}"`, () =>
                                this.deliveryEditorCheckoutWizard.getTitleText()
                                    .should.eventually.to.be.equal(
                                        titleText,
                                        `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                                    )
                        );

                        await this.allure.runStep(
                            'Выбрать способ доставки "Курьером".', () =>
                                this.deliveryTypes.setDeliveryTypeDelivery()
                        );

                        await this.deliveryEditorCheckoutWizard.waitForVisible();

                        await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);

                        await this.browser.yaScenario(
                            this,
                            fillAllDeliveryAddressFields,
                            {address: ADDRESSES.MOSCOW_HSCH_ADDRESS}
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Выбрать" должна быть активна.');

                                return this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );
                    }
                );

                await this.confirmationPage.waitForVisible();

                await this.allure.runStep(
                    'В блоке информации о доставке.', async () => {
                        await this.deliveryInfo.waitForVisible();
                        await this.allure.runStep(
                            `В блоке информации о доставки отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, () =>
                                this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                                    )
                        );
                    }
                );

                await this.deliveryIntervals.waitForRootIsVisible();
                await this.allure.runStep(
                    'Кликаем по кнопке селекта даты доставки.', () =>
                        this.dateSelect.click()
                );

                const lastDateIntervalText = await this.intervalSelectPopover.getLastOptionText();

                await this.allure.runStep(
                    'Выбираем последнюю доступную дату доставки.', () =>
                        this.intervalSelectPopover.clickOnLastOption()
                );

                await this.allure.runStep(
                    `Текст кнопки выбора даты доставки "${lastDateIntervalText}".`, () =>
                        this.dateSelect.getText()
                            .should.eventually.to.be.include(
                                lastDateIntervalText,
                                `Текст кнопки выбора даты доставки должен быть "${lastDateIntervalText}".`
                            )
                );

                await this.allure.runStep(
                    'Кликаем по разным интервалам в списке для применения ХСЧ', async () => {
                        await this.timeSelect.clickOnLastOption();
                    }
                );

                const lastTimeIntervalText = await this.timeSelect.getText();

                await this.allure.runStep(
                    `Текст кнопки выбора времени доставки "${lastTimeIntervalText}".`, () =>
                        this.timeSelect.getText()
                            .should.eventually.to.be.include(
                                lastTimeIntervalText,
                                `Текст кнопки выбора времени доставки должен быть "${lastTimeIntervalText}".`
                            )
                );

                await this.allure.runStep(
                    'В блоке "Получатель" указать данные получателя.', async () => {
                        await this.allure.runStep(
                            'В блоке "Получатель" нажать на кнопку "Выбрать Получателя".', async () => {
                                await this.recipientEditableCard.changeButtonClick();
                                await this.editPopup.waitForVisibleRoot();
                            }
                        );

                        await this.allure.runStep(
                            'В форме данных получателя указать следующие данные.', async () => {
                                await this.recipientFormFields.setNameInputValue(CONTACTS.HSCH_CONTACT.nameAndFamily);
                                await this.recipientFormFields.setPhoneInputValue(CONTACTS.HSCH_CONTACT.phone);
                                await this.recipientFormFields.setEmailInputValue(CONTACTS.HSCH_CONTACT.email);
                            }
                        );

                        await this.allure.runStep(
                            'Нажать на кнопку "Сохранить".', async () => {
                                await this.recepientForm.saveButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В попапе "Получатели" выбраны данные ранее указанного получателя.', async () => {
                        const recipientFullInfo = CONTACTS.HSCH_CONTACT.recipientFullInfo;
                        await this.recipientList.getActiveItemText()
                            .should.eventually.to.be.equal(recipientFullInfo, `Текст активного элемента должен быть "${recipientFullInfo}".`);
                        await this.recipientList.chooseRecipientButtonClick();
                    }
                );

                await this.allure.runStep(
                    'В блоке "Получатель" отображаются данные получателя из выбранные ранее.', async () => {
                        await this.recipientBlock.getContactText()
                            .should.eventually.to.be.equal(
                                CONTACTS.HSCH_CONTACT.recipientFullInfo,
                                'На карточке получателя должны быть указанные пользователем данные'
                            );
                    }
                );

                await this.allure.runStep(
                    'В блоке опций оплаты нажать на кнопку "Изменить".', async () => {
                        await this.paymentOptionsEditableCard.changeButtonClick();
                        await this.paymentOptionsModal.waitForVisible();
                    }
                );

                const changePaymentMethod = () => this.allure.runStep(
                    'Выбрать способ оплаты "Наличными при получении".', async () => {
                        await this.paymentOptionsPopUpContent.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsPopUpContent.submitButtonClick();

                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                            );
                    });

                await this.browser.yaScenario(
                    this,
                    waitCallingUserLastState,
                    changePaymentMethod
                );

                await this.allure.runStep(
                    'Перезагружаем страницу', async () => {
                        await this.browser.refresh();
                        await this.confirmationPage.waitForVisible(5000);
                    }
                );

                await this.allure.runStep(
                    'Проверяем ранее выбранные данные.', async () => {
                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'курьером',
                                'На карточке адреса должен отображаться способ доставки "Курьером".'
                            );

                        await this.allure.runStep(
                            `В блоке информации о доставки отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, () =>
                                this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                                    )
                        );

                        await this.allure.runStep(
                            `Текст кнопки выбора даты доставки "${lastDateIntervalText}".`, () =>
                                this.dateSelect.getText()
                                    .should.eventually.to.be.include(
                                        lastDateIntervalText,
                                        `Текст кнопки выбора даты доставки должен быть "${lastDateIntervalText}".`
                                    )
                        );

                        await this.allure.runStep(
                            `Текст кнопки выбора времени доставки "${lastTimeIntervalText}".`, () =>
                                this.timeSelect.getText()
                                    .should.eventually.to.be.include(
                                        lastTimeIntervalText,
                                        `Текст кнопки выбора времени доставки должен быть "${lastTimeIntervalText}".`
                                    )
                        );

                        await this.allure.runStep(
                            'В блоке "Получатель" отображаются данные получателя из выбранные ранее.', async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        CONTACTS.HSCH_CONTACT.recipientFullInfo,
                                        'На карточке получателя должны быть указанные пользователем данные'
                                    );
                            }
                        );

                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                            );
                    }
                );
            },
        }),
    },
});
