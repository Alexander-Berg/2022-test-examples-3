import {makeCase, makeSuite} from 'ginny';

import {prepareCheckoutPage, fillRecipientForm, fillAllDeliveryAddressFields} from '@self/root/src/spec/hermione/scenarios/checkout';

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
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import {SelectPopover} from '@self/root/src/components/Select/__pageObject';
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
import EditableCard from '@self/root/src/components/EditableCard/__pageObject';
import Modal from '@self/root/src/components/PopupBase/__pageObject';
import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';
import PlacemarkMap
    from '@self/root/src/widgets/content/checkout/common/CheckoutVectorPlacemarkMap/components/VectorPlacemarkMap/__pageObject';
/* eslint-disable max-len */
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import {DeliveryIntervals} from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryIntervals/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';
import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import Subtitle from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/Subtitle/__pageObject/';
import GroupedParcels from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import GroupedParcel from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/GroupedParcel/__pageObject';
import CheckoutRecipient from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject';
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';
/* eslint-enable max-len */
import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {ADDRESSES, CONTACTS} from '../../../constants';

const SIMPLE_PRODUCT_INDEX = 0;
const CC_PRODUCT_INDEX = 1;
const DSBS_PRODUCT_INDEX = 2;
const DSBS_INTERVAL_INDEX = 1;
const DIGITAL_PRODUCT_INDEX = 3;
const filterIntermediateTitles = 'Доставка';

export default makeSuite('Cохранение данных при оформлении мультизаказа.', {
    id: 'marketfront-4895',
    issue: 'MARKETFRONT-50677',
    feature: 'Cохранение данных при оформлении мультизаказа.',
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
                addressBlocks: () => this.createPageObject(GroupedParcels, {
                    parent: this.confirmationPage,
                }),
                addressBlock: () => this.createPageObject(GroupedParcel, {
                    parent: this.addressBlocks,
                }),
                addressEditableCard: () => this.createPageObject(EditableCard, {
                    parent: this.addressBlock,
                }),
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                recipientEditableCard: () => this.createPageObject(EditableCard, {
                    root: `${CheckoutRecipient.root}${EditableCard.root}`,
                    parent: this.confirmationPage,
                }),
                recipientForm: () => this.createPageObject(RecipientFormFields, {
                    parent: this.recipientWizard,
                }),
                deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                placemarkMap: () => this.createPageObject(PlacemarkMap, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.addressEditableCard,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                street: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
                recipientList: () => this.createPageObject(RecipientList),
                recepientForm: () => this.createPageObject(RecipientForm),
                recipientFormFields: () => this.createPageObject(RecipientFormFields, {
                    parent: this.recipientForm,
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
                deliveryIntervals: () => this.createPageObject(DeliveryIntervals),
                timeSelect: () => this.createPageObject(TimeSelect),
                intervalSelectPopover: () => this.createPageObject(SelectPopover),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                editableCardSubtitle: () => this.createPageObject(Subtitle),
                deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
            });

            await this.browser.setState('persAddress.lastState', {
                paymentType: null,
                paymentMethod: null,
                contactId: null,
                parcelsInfo: null,
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
                    cartIndex: 2,
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
                    cartIndex: 3,
                    items: [{
                        skuMock: digital.skuMock,
                        offerMock: digital.offerMock,
                        count: 1,
                    }],
                    isDigital: true,
                }),
            ];

            const testState = await this.browser.yaScenario(
                this,
                prepareMultiCartState,
                carts
            );

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    region: this.params.region,
                    checkout2: true,
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                }
            );
        },
        'Сохранение данных получателя.': makeCase({
            async test() {
                await this.allure.runStep(
                    'На экране выбора адреса для ОТ(обычный товар)', async () => {
                        await this.allure.runStep(
                            'Выбрать способ доставки "Курьером".', async () => {
                                await this.deliveryTypes.waitForVisible();
                                await this.deliveryTypes.setDeliveryTypeDelivery();
                            }
                        );

                        await this.browser.yaScenario(
                            this,
                            fillAllDeliveryAddressFields,
                            {address: ADDRESSES.MOSCOW_HSCH_ADDRESS}
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Продолжить".', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');

                                return this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Для C&C выбрать доступный ПВЗ.', async () => {
                        await this.placemarkMap.waitForVisible(2000);
                        await this.placemarkMap.waitForReady(4000);

                        await this.placemarkMap.clickOnOutlet([
                            farma.outletMock.gpsCoord.longitude,
                            farma.outletMock.gpsCoord.latitude,
                        ]);

                        await this.allure.runStep(
                            'Нажать кнопку "Продолжить".', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');

                                return this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'На экране выбора адреса для DSBS.', async () => {
                        await this.allure.runStep(
                            'Выбрать способ доставки "Курьером".', async () => {
                                await this.deliveryTypes.waitForVisible();
                                await this.deliveryTypes.setDeliveryTypeDelivery();
                            }
                        );

                        await this.browser.yaScenario(
                            this,
                            fillAllDeliveryAddressFields,
                            {address: ADDRESSES.HSCH_ADDRESS_FOR_DSBS}
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Продолжить".', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');

                                return this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В форме экрана "Получатель" указать данные получателя.', async () => {
                        await this.recipientWizard.waitForVisible();

                        await this.allure.runStep(
                            'Заполняем форму "Получатель" данными.', async () =>
                                this.browser.yaScenario(
                                    this,
                                    fillRecipientForm,
                                    {
                                        formName: 'user-postpaid',
                                        formData: CONTACTS.HSCH_CONTACT,
                                        recipientForm: this.recipientForm,
                                    }
                                )
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Продолжить".', async () => {
                                await this.recipientWizard.waitForEnabledSubmitButton(3000);
                                await this.recipientWizard.submitButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Открывается главный экран чекаута.', async () => {
                        await this.confirmationPage.waitForVisible();

                        await this.allure.runStep(
                            'В заголовке для ОТ отображается доставка "Курьером"', async () =>
                                this.addressBlocks.getAddressTitleByCardIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.include(
                                        'Доставка курьером',
                                        'Текст в заголовке должен содержать "Доставка курьером".'
                                    )
                        );

                        await this.allure.runStep(
                            `В блоке информации о доставки ОТ отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, async () =>
                                this.addressBlocks.getInfoTitleByCardIndex(SIMPLE_PRODUCT_INDEX, filterIntermediateTitles)
                                    .should.eventually.to.be.include(
                                        ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                                    )
                        );

                        await this.allure.runStep(
                            'В заголовке для C&C отображается доставка "Самовывоз"', async () =>
                                this.addressBlocks.getAddressTitleByCardIndex(CC_PRODUCT_INDEX)
                                    .should.eventually.include(
                                        'Самовывоз',
                                        'Текст в заголовке должен содержать "Сымовывоз".'
                                    )
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки C&C отображается выбранный ранее ПВЗ.', async () => {
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
                            'В заголовке для DSBS отображается доставка "Курьером"', async () =>
                                this.addressBlocks.getAddressTitleByCardIndex(DSBS_PRODUCT_INDEX)
                                    .should.eventually.include(
                                        'Доставка курьером',
                                        'Текст в заголовке должен содержать "Доставка курьером".'
                                    )
                        );

                        await this.allure.runStep(
                            `В блоке информации о доставки DSBS отображается адрес "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`, async () =>
                                this.addressBlocks.getInfoTitleByCardIndex(DSBS_PRODUCT_INDEX, filterIntermediateTitles)
                                    .should.eventually.to.be.include(
                                        ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                        `Текст в поле адрес должен быть "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`
                                    )
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
                    }
                );

                await this.allure.runStep(
                    'Для ОТ открыть список с датой доставки.', () =>
                        this.addressBlocks.clickDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                );

                const lastSimpleDateIntervalText = await this.intervalSelectPopover.getLastOptionText();
                const lastDSBSDateIntervalText = await this.addressBlocks.getTextDataIntervalByIndex(DSBS_INTERVAL_INDEX);

                await this.allure.runStep(
                    'Выбираем последнюю доступную дату доставки.', async () => {
                        await this.intervalSelectPopover.clickOptionByText(lastSimpleDateIntervalText);
                        await this.allure.runStep(
                            `Текст кнопки выбора даты доставки "${lastSimpleDateIntervalText}".`, () =>
                                this.addressBlocks.getTextDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.to.be.include(
                                        lastSimpleDateIntervalText,
                                        `Текст кнопки выбора даты доставки должен быть "${lastSimpleDateIntervalText}".`
                                    )
                        );
                    }
                );

                await this.deliveryIntervals.waitForRootIsVisible();

                const lastSimpleTimeIntervalText = await this.timeSelect.getText();
                await this.allure.runStep(
                    'Кликаем по разным интервалам в списке для применения ХСЧ', async () => {
                        // по умолчанию выбирается первый попавшийся интервал, поэтому пытаемся кликнуть по второму
                        await this.timeSelect.clickByIndex(1);
                        await this.timeSelect.clickByText(lastSimpleTimeIntervalText);
                        await this.allure.runStep(
                            `Выбрана кнопка времени доставки "${lastSimpleTimeIntervalText}".`, () =>
                                this.addressBlocks.getTextTimeIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.to.be.equal(
                                        lastSimpleTimeIntervalText,
                                        `Текст кнопки выбора времени доставки должен быть "${lastSimpleTimeIntervalText}".`
                                    )
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
                    'Выбрать способ оплаты "Наличными при получении".', async () => {
                        await this.paymentOptionsPopUpContent.setPaymentTypeCashOnDelivery();
                        await this.paymentOptionsPopUpContent.submitButtonClick();
                        await this.preloader.waitForHidden(5000);

                        await this.paymentOptionsBlock.getText()
                            .should.eventually.include(
                                'Наличными при получении',
                                'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                            );
                    }
                );

                await this.allure.runStep(
                    'Вернуться на страницу корзины.', () =>
                        this.browser.yaOpenPage(PAGE_IDS_COMMON.CART)
                );

                await this.allure.runStep(
                    'На странице корзины нажать на кнопку "Перейти к оформлению"', async () => {
                        await this.cartCheckoutButton.waitForButtonEnabled();
                        await this.cartCheckoutButton.goToCheckout();
                    }
                );

                await this.allure.runStep(
                    'Открывается главный экран чекаута', async () => {
                        await this.confirmationPage.waitForVisible(5000);

                        await this.allure.runStep(
                            'В блоке информации о доставки ОТ отображается выбранный ранее интервал, адрес и доставка.',
                            async () => {
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
                                        `На карточке доставки OT должен отображаться интервал доставки "${lastSimpleTimeIntervalText}".`
                                    );

                                await this.allure.runStep(
                                    `В блоке информации о доставки ОТ отображается адрес "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`, async () =>
                                        this.addressBlocks.getInfoTitleByCardIndex(SIMPLE_PRODUCT_INDEX, filterIntermediateTitles)
                                            .should.eventually.to.be.include(
                                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                                `Текст в поле адрес должен быть "${ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo}".`
                                            )
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки C&C отображается выбранный ранее ПВЗ.', async () => {
                                const outletInfo = ['Магазин Retest Full 1\n'] +
                                    ['Москва, Сходненская, д. 11, стр. 1\n'] +
                                    ['Ежедневно\n'] +
                                    ['10:00 – 22:00'];

                                await this.addressBlocks.getAddressTitleByCardIndex(CC_PRODUCT_INDEX)
                                    .should.eventually.include(
                                        `Самовывоз ${lastDSBSDateIntervalText
                                            .replace('Дата доставки\n', '')
                                            .replace('с ', '')
                                            .replace(' по ', ' \u2013 ')}`,
                                        'На карточке доставки C&C должена отображаться доставка самовывозом и дата доставки.'
                                    );

                                await this.addressBlocks.getInfoTitleByCardIndex(CC_PRODUCT_INDEX, filterIntermediateTitles)
                                    .should.eventually.include(
                                        outletInfo,
                                        `Текст в поле адрес должен быть "${outletInfo}".`
                                    );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки DSBS отображается выбранный ранее адрес и доставка.', async () => {
                                await this.addressBlocks.getAddressTitleByCardIndex(DSBS_PRODUCT_INDEX)
                                    .should.eventually.include(
                                        `Доставка курьером ${lastDSBSDateIntervalText
                                            .replace('Дата доставки\n', '')
                                            .replace('с ', '')
                                            .replace(' по ', ' \u2013 ')}`,
                                        'На карточке доставки DSBS должена отображаться доставка курьером и дата доставки.'
                                    );

                                await this.allure.runStep(
                                    `В блоке информации о доставки DSBS отображается адрес "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`, async () =>
                                        this.addressBlocks.getInfoTitleByCardIndex(DSBS_PRODUCT_INDEX, filterIntermediateTitles)
                                            .should.eventually.to.be.include(
                                                ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                                `Текст в поле адрес должен быть "${ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo}".`
                                            )
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
