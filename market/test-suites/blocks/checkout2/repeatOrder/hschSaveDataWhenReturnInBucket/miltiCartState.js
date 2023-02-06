import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    fillAllDeliveryAddressFields,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import {SelectPopover} from '@self/root/src/components/Select/__pageObject';
import {DeliveryIntervals} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryIntervals/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
import Subtitle from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/Subtitle/__pageObject/';
/**
 * @ifLose заменить на старые импорты из .../components/DeliveryIntervals/__pageObject
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @start
 */
import {
    TimeSelect,
    // eslint-disable-next-line max-len
} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/TimeIntervalSelector/__pageObject';
/**
 * @expFlag all_checkout_new_intervals [обратный эксперимент]
 * @ticket MARKETFRONT-58113
 * @end
 */

import {ADDRESSES, CONTACTS} from '../../constants';

const SIMPLE_PRODUCT_INDEX = 0;
const DSBS_PRODUCT_INDEX = 1;
const CC_PRODUCT_INDEX = 2;
const DIGITAL_PRODUCT_INDEX = 3;

export default makeSuite('ХСЧ: сохранение данных при оформлении мультизаказа.', {
    id: 'marketfront-4896',
    issue: 'MARKETFRONT-50680',
    feature: 'ХСЧ: сохранение данных при оформлении мультизаказа.',
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
                placemarkMap: () => this.createPageObject(PlacemarkMap, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                popupBase: () => this.createPageObject(PopupBase),
                editPopup: () => this.createPageObject(EditPopup),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                timeSelect: () => this.createPageObject(TimeSelect),
                recipientList: () => this.createPageObject(RecipientList),
                recepientForm: () => this.createPageObject(RecipientForm),
                recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                    parent: this.recipientForm,
                }),
                paymentOptionsModal: () => this.createPageObject(Modal),
                paymentOptionsPopUpContent: () => this.createPageObject(PaymentOptionsList),
                deliveryIntervals: () => this.createPageObject(DeliveryIntervals),
                intervalSelectPopover: () => this.createPageObject(SelectPopover),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                editableCardSubtitle: () => this.createPageObject(Subtitle),
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
                buildCheckouterBucket({
                    cartIndex: 1,
                    items: [{
                        skuMock: dsbs.skuPhoneMock,
                        offerMock: dsbs.offerPhoneMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryDeliveryMock,
                        deliveryPartnerType: 'SHOP',
                    }],
                }),
                buildCheckouterBucket({
                    cartIndex: 2,
                    items: [{
                        skuMock: farma.skuMock,
                        offerMock: farma.offerMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryPickupMock,
                        paymentOptions: [
                            paymentOptions.cashOnDelivery,
                        ],
                        outlets: [
                            {id: x5outletMock.id, regionId: 0},
                            {id: farma.outletMock.id, regionId: 0},
                        ],
                    }],
                    outlets: [
                        x5outletMock,
                        farma.outletMock,
                    ],
                }),
                buildCheckouterBucket({
                    cartIndex: 3,
                    items: [{
                        skuMock: digital.skuMock,
                        offerMock: digital.offerMock,
                        count: 1,
                    }],
                    isDigital: true,
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
        },
        'Сохранение данных получателя при оформлении мультизаказа.': makeCase({
            async test() {
                await this.allure.runStep(
                    'В блоке информации о доставке ОТ', async () => {
                        await this.allure.runStep(
                            'В блоке доставки ОТ нажать на кнопку "Изменить"', async () => {
                                await this.addressBlocks.clickChangeButtonByIndex(SIMPLE_PRODUCT_INDEX);
                            }
                        );

                        await this.allure.runStep(
                            'Выбрать способ доставки "Курьером" и нажать на кнопку "Добавить новый адрес".', async () => {
                                await this.editPopup.waitForVisibleRoot();
                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery();
                                await this.editPopup.deliveryChooseButtonClick();

                                await this.deliveryEditorCheckoutWizard.waitForVisible();
                            }
                        );
                    }
                );

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

                await this.confirmationPage.waitForVisible();
                await this.deliveryInfo.waitForVisible();

                await this.allure.runStep(
                    `В блоке информации о доставки отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, async () =>
                        this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX)
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                            )
                );

                const lastSimpleDateIntervalText = await this.addressBlocks.getTextDataIntervalByIndex(SIMPLE_PRODUCT_INDEX);
                const lastSimpleTimeIntervalText = await this.addressBlocks.getTextTimeIntervalByIndex(SIMPLE_PRODUCT_INDEX);

                await this.allure.runStep(
                    'В блоке информации о доставке DSBS', async () => {
                        await this.allure.runStep(
                            'В блоке доставки DSBS нажать на кнопку "Изменить"', async () => {
                                await this.deliveryInfo.waitForVisible();
                                await this.addressBlocks.clickChangeButtonByIndex(DSBS_PRODUCT_INDEX);
                            }
                        );

                        await this.allure.runStep(
                            'Выбрать способ доставки "Курьером" и нажать на кнопку "Добавить новый адрес".', async () => {
                                await this.editPopup.waitForVisibleRoot();
                                await this.popupDeliveryTypes.setDeliveryTypeDelivery();
                                await this.editPopup.addButtonClick();

                                await this.deliveryEditorCheckoutWizard.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.yaScenario(
                    this,
                    fillAllDeliveryAddressFields,
                    {address: ADDRESSES.HSCH_ADDRESS_FOR_DSBS}
                );

                await this.allure.runStep(
                    'Нажать кнопку "Выбрать".', async () => {
                        await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                        await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка "Выбрать" должна быть активна.');

                        return this.deliveryEditorCheckoutWizard.submitButtonClick();
                    }
                );

                await this.confirmationPage.waitForVisible();
                await this.deliveryInfo.waitForVisible();

                await this.allure.runStep(
                    `В блоке информации о доставки DSBS отображается адрес "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`, async () =>
                        this.addressBlocks.getTextByCardIndex(DSBS_PRODUCT_INDEX)
                            .should.eventually.to.be.equal(
                                ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                `Текст в поле адрес должен быть "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`
                            )
                );

                await this.allure.runStep(
                    'Кликаем по кнопке селекта даты доставки для DSBS.', () =>
                        this.addressBlocks.clickDataIntervalByIndex(DSBS_PRODUCT_INDEX)
                );
                const lastDSBSDateIntervalText = await this.intervalSelectPopover.getLastOptionText();
                await this.allure.runStep(
                    'Для DSBS выбираем последнюю доступную дату доставки.', () =>
                        this.intervalSelectPopover.clickOptionByText(lastDSBSDateIntervalText)
                );
                await this.allure.runStep(
                    `Текст кнопки выбора даты доставки для DSBS "${lastDSBSDateIntervalText}".`, () =>
                        this.addressBlocks.getTextDataIntervalByIndex(DSBS_PRODUCT_INDEX)
                            .should.eventually.to.be.include(
                                lastDSBSDateIntervalText,
                                `Текст кнопки выбора даты доставки должен быть "${lastDSBSDateIntervalText}".`
                            )
                );

                await this.deliveryIntervals.waitForRootIsVisible();

                const lastDSBSTimeIntervalText = await this.timeSelect.getText();
                await this.allure.runStep(
                    'Кликаем по разным интервалам в списке для применения ХСЧ', async () => {
                        // по умолчанию выбирается первый попавшийся интервал, поэтому пытаемся кликнуть по второму
                        await this.timeSelect.clickByIndex(1);
                        await this.timeSelect.clickByText(lastDSBSTimeIntervalText);
                    }
                );
                await this.allure.runStep(
                    `Текст кнопки выбора времени доставки для DSBS "${lastDSBSTimeIntervalText}".`, () =>
                        this.addressBlocks.getTextTimeIntervalByIndex(DSBS_PRODUCT_INDEX)
                            .should.eventually.to.be.include(
                                lastDSBSTimeIntervalText,
                                `Текст кнопки выбора времени доставки должен быть "${lastDSBSTimeIntervalText}".`
                            )
                );

                await this.allure.runStep(
                    'В блоке информации о доставке C&C', async () => {
                        await this.allure.runStep(
                            'В блоке доставки C&C нажать на кнопку "Изменить"', async () => {
                                await this.addressBlocks.clickChangeButtonByIndex(CC_PRODUCT_INDEX);
                            }
                        );

                        await this.allure.runStep(
                            'Нажать на кнопку "Выбрать пункт выдачи".', async () => {
                                await this.editPopup.waitForVisibleRoot();
                                await this.editPopup.deliveryChooseButtonClick();

                                await this.deliveryEditorCheckoutWizard.waitForVisible();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Выбрать доступный ПВЗ и нажать на кнопку "Выбрать".', async () => {
                        await this.placemarkMap.waitForVisible(2000);
                        await this.placemarkMap.waitForReady(4000);

                        await this.placemarkMap.clickOnOutlet([
                            farma.outletMock.gpsCoord.longitude,
                            farma.outletMock.gpsCoord.latitude,
                        ]);

                        await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(3000);
                        await this.deliveryEditorCheckoutWizard.submitButtonClick();
                    }
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

                await this.allure.runStep(
                    'Ждем вызова userLastState',
                    () => this.browser.yaWaitKadavrLogByBackendMethod(
                        'PersAddress', 'updateUserLastState',
                        () => this.allure.runStep(
                            'Выбрать способ оплаты "Наличными при получении".', async () => {
                                await this.paymentOptionsPopUpContent.setPaymentTypeCashOnDelivery();
                                await this.paymentOptionsPopUpContent.submitButtonClick();

                                await this.paymentOptionsBlock.getText()
                                    .should.eventually.include(
                                        'Наличными при получении',
                                        'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                                    );
                            }
                        )
                    )
                );

                await this.allure.runStep(
                    'Перезагружаем страницу.', () =>
                        this.browser.refresh()
                );

                await this.allure.runStep(
                    'Открывается главный экран чекаута', async () => {
                        await this.confirmationPage.waitForVisible(5000);

                        await this.addressBlocks.getAddressTitleByCardIndex(SIMPLE_PRODUCT_INDEX)
                            .should.eventually.include(
                                `Доставка курьером ${lastSimpleDateIntervalText
                                    .replace('Дата доставки\n', '')
                                    .replace('с ', '')
                                    .replace(' по ', ' \u2013 ')}`,
                                'На карточке доставки ОТ должена отображаться доставка курьером и дата доставки.'
                            );

                        await this.addressBlocks.getTextTimeIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                            .should.eventually.to.be.include(
                                lastSimpleTimeIntervalText,
                                `На карточке доставки ОТ должен отображаться интервал доставки "${lastSimpleTimeIntervalText}".`
                            );

                        await this.allure.runStep(
                            `В блоке информации о доставки отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, async () =>
                                this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.to.be.equal(
                                        ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                                    )
                        );

                        await this.addressBlocks.getAddressTitleByCardIndex(DSBS_PRODUCT_INDEX)
                            .should.eventually.include(
                                `Доставка курьером ${lastDSBSDateIntervalText
                                    .replace('Дата доставки\n', '')
                                    .replace('с ', '')
                                    .replace(' по ', ' \u2013 ')}`,
                                'На карточке доставки DSBS должена отображаться доставка курьером и дата доставки.'
                            );

                        await this.addressBlocks.getTextTimeIntervalByIndex(DSBS_PRODUCT_INDEX)
                            .should.eventually.to.be.include(
                                lastDSBSTimeIntervalText,
                                `На карточке доставки DSBS должен отображаться интервал доставки "${lastDSBSTimeIntervalText}".`
                            );

                        await this.allure.runStep(
                            `В блоке информации о доставки DSBS отображается адрес "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`, async () =>
                                this.addressBlocks.getTextByCardIndex(DSBS_PRODUCT_INDEX)
                                    .should.eventually.to.be.equal(
                                        ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`
                                    )
                        );

                        await this.addressBlocks.getAddressTitleByCardIndex(CC_PRODUCT_INDEX)
                            .should.eventually.include(
                                `Самовывоз ${lastDSBSDateIntervalText
                                    .replace('Дата доставки\n', '')
                                    .replace('с ', '')
                                    .replace(' по ', ' \u2013 ')}`,
                                'На карточке доставки C&C должена отображаться доставка самовывозом и дата доставки.'
                            );

                        await this.allure.runStep(
                            'В блоке информации о доставки C&C отображается выбранный ранее ПВЗ.', async () => {
                                const filterIntermediateTitles = 'Доставка';
                                const outletInfo = ['Магазин Retest Full 1\n'] +
                                    ['Москва, Сходненская, д. 11, стр. 1\n'] +
                                    ['Ежедневно\n'] +
                                    ['10:00 – 22:00'];

                                await this.addressBlocks.getInfoTitleByCardIndex(CC_PRODUCT_INDEX, filterIntermediateTitles)
                                    .should.eventually.include(
                                        outletInfo,
                                        `Текст в поле адрес должен быть "${outletInfo}".`
                                    );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки Цифрового товара отображается выбранная ранее почта.', async () => {
                                await this.addressBlocks.getAddressTitleByCardIndex(DIGITAL_PRODUCT_INDEX)
                                    .should.eventually.to.be.equal(
                                        'Получение по электронной почте',
                                        'Текст в заголовке должен быть "Получение по электронной почте".'
                                    );

                                await this.editableCardSubtitle.getSubtitleText()
                                    .should.eventually.to.be.equal(
                                        CONTACTS.HSCH_CONTACT.fullDeliveryInfo,
                                        `Текст в подзаголовке должен быть "${CONTACTS.HSCH_CONTACT.fullDeliveryInfo}".`
                                    );
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
