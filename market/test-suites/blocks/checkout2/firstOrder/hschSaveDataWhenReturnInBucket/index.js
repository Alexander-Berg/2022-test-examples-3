import {
    makeSuite,
    makeCase,
} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import * as dsbs from '@self/root/src/spec/hermione/kadavr-mock/report/dsbs';
import * as digital from '@self/root/src/spec/hermione/kadavr-mock/report/digital';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {
    deliveryDeliveryMock,
    deliveryPickupMock,
    paymentOptions,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {fillAddressForm, fillDeliveryType} from '@self/platform/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {prepareCheckoutPage, waitPreloader} from '@self/root/src/spec/hermione/scenarios/checkout';

import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch.js';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
/**
 * @ifLose заменить на старые импорты из .../components/DeliveryIntervals/__pageObject
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
import {SelectPopover} from '@self/root/src/components/Select/__pageObject';
import PaymentOptionsList
    from '@self/root/src/components/PaymentOptionsList/__pageObject';
import RecipientPopupContainer from
    '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import CartCheckoutButton from '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import PlacemarkMap from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/PlacemarkMap/__pageObject';
import Subtitle from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/Subtitle/__pageObject';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ADDRESSES, CONTACTS} from '../constants';

const SIMPLE_PRODUCT_INDEX = 0;
const CC_PRODUCT_INDEX = 1;
const DSBS_ADDRESS_INDEX = 1;
const DSBS_PRODUCT_INDEX = 2;
const DIGITAL_PRODUCT_INDEX = 3;
const filterIntermediateTitles = 'Доставка';

export default makeSuite('Сохранение данных получателя при оформлении мультизаказа и возврате на страницу корзины.', {
    id: 'm-touch-3644',
    issue: 'MARKETFRONT-50731',
    feature: 'Сохранение данных получателя при оформлении мультизаказа и возврате на страницу корзины.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions),
                recipientPopupContainer: () => this.createPageObject(RecipientPopupContainer),
                fullAddressForm: () => this.createPageObject(FullAddressForm),
                citySuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.fullAddressForm,
                }),
                streetSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: FullAddressForm.street,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.addressBlock,
                }),
                contactCard: () => this.createPageObject(ContactCard, {
                    parent: this.confirmationPage,
                }),
                dateSelect: () => this.createPageObject(DateSelect, {
                    parent: this.addressBlock,
                }),
                timeSelect: () => this.createPageObject(TimeSelect, {
                    parent: this.addressBlock,
                }),
                selectPopover: () => this.createPageObject(SelectPopover),
                paymentOptionsPopupContent: () => this.createPageObject(PaymentOptionsList),
                cartCheckoutButton: () => this.createPageObject(CartCheckoutButton),
                placemarkMap: () => this.createPageObject(PlacemarkMap),
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
                    deliveryOptions: [
                        deliveryDeliveryMock,
                        {
                            ...deliveryDeliveryMock,
                            id: 'mockedId1',
                            hash: 'mockedHash1',
                            dates: {
                                fromDate: '05-03-2024',
                                toDate: '06-03-2024',
                            },
                            deliveryOptionId: 1003938,
                        },
                    ],
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

            await this.browser.setState('persAddress.lastState', {
                paymentType: null,
                paymentMethod: null,
                contactId: null,
                parcelsInfo: null,
            });

            await this.browser.yaScenario(
                this,
                prepareCheckoutPage,
                {
                    items: testState.checkoutItems,
                    reportSkus: testState.reportSkus,
                    checkout2: true,
                }
            );
        },
        'Сохранение данных получателя при оформлении мультизаказа.': makeCase({
            async test() {
                await this.allure.runStep(
                    'Выбираем тип доставки "Курьером" и адрес ОТ.', async () => {
                        await this.browser.yaScenario(
                            this,
                            fillDeliveryType,
                            {type: 'DELIVERY'}
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();

                        await this.browser.yaScenario(
                            this,
                            fillAddressForm,
                            ADDRESSES.MOSCOW_HSCH_ADDRESS
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();
                    }
                );

                await this.allure.runStep(
                    'Выбираем тип доставки "Самовывозом" и ПВЗ C&C.', async () => {
                        await this.browser.yaScenario(
                            this,
                            fillDeliveryType,
                            {type: 'PICKUP'}
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();

                        await this.allure.runStep(
                            'Для C&C выбрать доступный ПВЗ.', async () => {
                                await this.placemarkMap.waitForVisible(2000);
                                await this.placemarkMap.waitForReady(4000);

                                await this.placemarkMap.clickOnOutlet([
                                    farma.outletMock.gpsCoord.latitude,
                                    farma.outletMock.gpsCoord.longitude,
                                ]);
                            }
                        );

                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () => {
                                await this.deliveryEditor.chooseButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Выбираем тип доставки "Курьером" и адрес DSBS.', async () => {
                        await this.browser.yaScenario(
                            this,
                            fillDeliveryType,
                            {type: 'DELIVERY'}
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();

                        await this.browser.yaScenario(
                            this,
                            fillAddressForm,
                            ADDRESSES.HSCH_ADDRESS_FOR_DSBS
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();
                    }
                );

                await this.allure.runStep(
                    'Заполняем данные пользователя.', async () => {
                        await this.recipientForm.setRecipientData(CONTACTS.HSCH_CONTACT, 0);
                        await this.deliveryEditor.submitButtonClick();
                    }
                );

                await this.allure.runStep(
                    'Отображается главный экран чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.browser.yaScenario(this, waitPreloader);

                        await this.allure.runStep(
                            'В блоке информации о доставке ОТ', async () => {
                                await this.browser.waitUntil(
                                    async () => {
                                        const request = await this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX);
                                        return Boolean(request);
                                    },
                                    5000, 'Карточка с адресом ОТ должна быть не пуста.', 1000
                                );

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Доставка курьером"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Доставка курьером',
                                                'Текст заголовка должен содержать "Доставка курьером".'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должны отображаться указанные пользователем данные адреса', async () => {
                                        await this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.to.be.equal(
                                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'На экране для C&C отображается выбранный ПВЗ', async () => {
                                const outletInfo = ['Магазин Retest Full 1\n'] +
                                    ['Москва, Сходненская, д. 11, стр. 1\n'] +
                                    ['Ежедневно\n'] +
                                    ['10:00 – 22:00'];

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Самовывоз"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(CC_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Самовывоз',
                                                'На карточке доставки C&C должена отображаться доставка самовывозом.'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должен отображаться выбранный ПВЗ', async () => {
                                        await this.addressBlocks.getInfoTitleByCardIndex(CC_PRODUCT_INDEX, filterIntermediateTitles)
                                            .should.eventually.include(
                                                outletInfo,
                                                `Текст в поле адрес должен быть "${outletInfo}".`
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставке DSBS', async () => {
                                await this.browser.waitUntil(
                                    async () => {
                                        const request = await this.addressBlocks.getTextByCardIndex(DSBS_ADDRESS_INDEX);
                                        return Boolean(request);
                                    },
                                    5000, 'Карточка с адресом DSBS должна быть не пуста.', 1000
                                );

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Доставка курьером"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(DSBS_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Доставка курьером',
                                                'Текст заголовка должен содержать "Доставка курьером".'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должны отображаться указанные пользователем данные адреса', async () => {
                                        await this.addressBlocks.getTextByCardIndex(DSBS_ADDRESS_INDEX)
                                            .should.eventually.to.be.equal(
                                                ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки Цифрового товара отображается выбранная ранее почта.', async () => {
                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Получение по электронной почте"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(DIGITAL_PRODUCT_INDEX)
                                            .should.eventually.to.be.equal(
                                                'Получение по электронной почте',
                                                'Текст в заголовке должен быть "Получение по электронной почте".'
                                            );
                                    }
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
                    'Открываем список с датами доставки для ОТ', async () => {
                        await this.allure.runStep(
                            'Кликаем по кнопке селекта даты доставки для ОТ.', () =>
                                this.addressBlocks.clickDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                        );

                        await this.allure.runStep(
                            'Выбираем последнюю дату доставки', async () => {
                                await this.selectPopover.waitForListIsVisible();
                                await this.selectPopover.clickOptionByText('с 23 февраля по 8 марта, 250 ₽');
                            }
                        );

                        await this.allure.runStep(
                            'На экране отображается выбранная дата доставки', async () => {
                                await this.addressBlocks.getTextDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.to.be.include(
                                        'с 23 февраля по 8 марта',
                                        'должна отображаться выбранная дата доставки'
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Открываем список с интервалами доставки для ОТ', async () => {
                        await this.allure.runStep(
                            'Выбираем последний интервал времени доставки', async () => {
                                await this.timeSelect.clickByText('16:00-20:00');
                            }
                        );

                        await this.allure.runStep(
                            'На экране отображается выбранное время доставки', async () => {
                                await this.addressBlocks.getTextTimeIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                    .should.eventually.to.be.include(
                                        '16:00-20:00',
                                        'должна отображаться выбранное время доставки'
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В блоке способа оплаты.', async () => {
                        await this.allure.runStep(
                            'Нажать кнопку изменения способа оплаты.', async () => {
                                await this.paymentOptionsEditableCard.changeButtonClick();
                            }
                        );

                        await this.allure.runStep(
                            'Выбрать способ оплаты "Наличными при получении".', async () => {
                                await this.paymentOptionsModal.waitForVisible();
                                await this.paymentOptionsPopupContent.setPaymentTypeCashOnDelivery();
                            }
                        );

                        await this.allure.runStep(
                            'Нажать кнопку подтверждения.', async () => {
                                await this.paymentOptionsPopupContent.submitButtonClick();
                                await this.paymentOptionsModal.waitForNonexisting();
                            }
                        );

                        await this.allure.runStep(
                            'В описании блока отображается.', async () => {
                                await this.browser.yaScenario(this, waitPreloader);
                                await this.paymentOptionsBlock.getText()
                                    .should.eventually.include(
                                        'Наличными при получении',
                                        'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                                    );
                            }
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
                    'После перезагрузки отображаются измененные данные', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.browser.yaScenario(this, waitPreloader);

                        await this.allure.runStep(
                            'В блоке информации о доставке ОТ', async () => {
                                await this.browser.waitUntil(
                                    async () => {
                                        const request = await this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX);
                                        return Boolean(request);
                                    },
                                    5000, 'Карточка с адресом ОТ должна быть не пуста.', 1000
                                );

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Доставка курьером"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Доставка курьером',
                                                'Текст заголовка должен содержать "Доставка курьером".'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должны отображаться указанные пользователем данные адреса', async () => {
                                        await this.addressBlocks.getTextByCardIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.to.be.equal(
                                                ADDRESSES.MOSCOW_HSCH_ADDRESS.fullDeliveryInfo,
                                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На экране отображается дефолтная дата доставки', async () => {
                                        await this.addressBlocks.getTextDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.to.be.include(
                                                'с 23 февраля по 8 марта',
                                                'должна отображаться дефолтная дата доставки'
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'На экране для C&C отображается выбранный ПВЗ', async () => {
                                const outletInfo = ['Магазин Retest Full 1\n'] +
                                    ['Москва, Сходненская, д. 11, стр. 1\n'] +
                                    ['Ежедневно\n'] +
                                    ['10:00 – 22:00'];

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Самовывоз"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(CC_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Самовывоз',
                                                'На карточке доставки C&C должена отображаться доставка самовывозом.'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должен отображаться выбранный ПВЗ', async () => {
                                        await this.addressBlocks.getInfoTitleByCardIndex(CC_PRODUCT_INDEX, filterIntermediateTitles)
                                            .should.eventually.include(
                                                outletInfo,
                                                `Текст в поле адрес должен быть "${outletInfo}".`
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставке DSBS', async () => {
                                await this.browser.waitUntil(
                                    async () => {
                                        const request = await this.addressBlocks.getTextByCardIndex(DSBS_ADDRESS_INDEX);
                                        return Boolean(request);
                                    },
                                    5000, 'Карточка с адресом DSBS должна быть не пуста.', 1000
                                );

                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Доставка курьером"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(DSBS_PRODUCT_INDEX)
                                            .should.eventually.include(
                                                'Доставка курьером',
                                                'Текст заголовка должен содержать "Доставка курьером".'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На карточке адреса доставки должны отображаться указанные пользователем данные адреса', async () => {
                                        await this.addressBlocks.getTextByCardIndex(DSBS_ADDRESS_INDEX)
                                            .should.eventually.to.be.equal(
                                                ADDRESSES.HSCH_ADDRESS_FOR_DSBS.fullDeliveryInfo,
                                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На экране отображается выбранная дата доставки', async () => {
                                        await this.addressBlocks.getTextDataIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.to.be.include(
                                                'с 23 февраля по 8 марта',
                                                'должна отображаться выбранная дата доставки'
                                            );
                                    }
                                );

                                await this.allure.runStep(
                                    'На экране отображается выбранное время доставки', async () => {
                                        await this.addressBlocks.getTextTimeIntervalByIndex(SIMPLE_PRODUCT_INDEX)
                                            .should.eventually.to.be.include(
                                                '16:00-20:00',
                                                'должно отображаться выбранное время доставки'
                                            );
                                    }
                                );
                            }
                        );

                        await this.allure.runStep(
                            'В блоке информации о доставки Цифрового товара отображается выбранная ранее почта.', async () => {
                                await this.allure.runStep(
                                    'Текст заголовка должен содержать "Получение по электронной почте"', async () => {
                                        await this.addressBlocks.getAddressTitleByCardIndex(DIGITAL_PRODUCT_INDEX)
                                            .should.eventually.to.be.equal(
                                                'Получение по электронной почте',
                                                'Текст в заголовке должен быть "Получение по электронной почте".'
                                            );
                                    }
                                );

                                await this.editableCardSubtitle.getSubtitleText()
                                    .should.eventually.to.be.equal(
                                        CONTACTS.HSCH_CONTACT.fullDeliveryInfo,
                                        `Текст в подзаголовке должен быть "${CONTACTS.HSCH_CONTACT.fullDeliveryInfo}".`
                                    );
                            }
                        );

                        await this.contactCard.getContactText()
                            .should.eventually.to.be.equal(
                                CONTACTS.HSCH_CONTACT.recipientFullInfo,
                                'На карточке получателя должны отображаться данные указанные пользователем'
                            );

                        await this.allure.runStep(
                            'В описании блока отображается.', async () => {
                                await this.paymentOptionsBlock.getText()
                                    .should.eventually.include(
                                        'Наличными при получении',
                                        'На карточке способа оплаты должен отображаться способ оплаты "Наличными при получении".'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
