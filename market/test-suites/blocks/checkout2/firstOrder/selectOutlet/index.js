import {
    makeSuite,
    makeCase,
} from 'ginny';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';
import {
    deliveryPickupMock,
    deliveryOptionsMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {
    sdek,
    boxberry,
    pickpoint1,
    x5outlet,
} from '@self/root/src/spec/hermione/kadavr-mock/report/outlets';
import x5outletMock from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {fillDeliveryType} from '@self/platform/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {
    ACTUALIZATION_TIMEOUT,
    prepareCheckoutPage,
    waitPreloader,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch.js';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import ContactCard from '@self/root/src/components/Checkout/ContactCard/__pageObject';
// eslint-disable-next-line max-len
import OutletInfo from '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/OutletInfo/__pageObject/index.js';
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
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
import CartCheckoutButton from
    '@self/root/src/widgets/content/cart/CartCheckoutControl/components/CartCheckoutButton/__pageObject';
import PlacemarkMap from
    // eslint-disable-next-line max-len
    '@self/root/src/widgets/content/checkout/common/CheckoutTouchSimpleDeliveryEditor/components/PlacemarkMap/__pageObject';
import Subtitle from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/Subtitle/__pageObject';
import CheckoutOrderButton
    from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';

import {CONTACTS} from '../constants';


export default makeSuite('Оформление заказа.', {
    id: 'marketfront-5723',
    issue: 'marketfront-5723',
    params: {
        region: 'Регион',
    },
    defaultParams: {
        region: region['Москва'],
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
                outletInfo: () => this.createPageObject(OutletInfo),
                recipientForm: () => this.createPageObject(RecipientForm),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton),
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
                        ...deliveryOptionsMock,
                        {
                            ...deliveryPickupMock,
                            outlets: [
                                {id: x5outlet.id, regionId: 0},
                                {id: sdek.id, regionId: 0},
                                {id: boxberry.id, regionId: 0},
                                {id: pickpoint1.id, regionId: 0},
                            ],
                        },
                    ],
                    outlets: [
                        x5outlet,
                        sdek,
                        boxberry,
                        pickpoint1,
                    ],
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
        'В пункт самовывоза.': makeCase({
            async test() {
                await this.allure.runStep(
                    'Выбираем тип доставки "Самовывозом".', async () => {
                        await this.browser.yaScenario(
                            this,
                            fillDeliveryType,
                            {type: 'PICKUP'}
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();
                    }
                );

                await this.allure.runStep(
                    'На карте выбираем доступный ПВЗ', async () => {
                        await this.placemarkMap.waitForVisible(2000);
                        await this.placemarkMap.waitForReady(4000);

                        await this.placemarkMap.clickOnOutlet([
                            x5outletMock.gpsCoord.latitude,
                            x5outletMock.gpsCoord.longitude,
                        ]);

                        await this.outletInfo.waitForVisible();
                        await this.outletInfo.waitForVisibleCloseButton();

                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () => {
                                await this.deliveryEditor.chooseButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep('Экран с получателем отображается', async () => {
                    await this.recipientForm.waitForVisible();
                });

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
                    }
                );

                await this.allure.runStep(
                    'В блоке доставки отображается информация о выбранном ПВЗ', async () => {
                        const [workingTime] = x5outletMock.workingTime;
                        const {hoursFrom, hoursTo} = workingTime;

                        const outletInfo = [`${x5outletMock.name}\n`] +
                            [`${x5outletMock.address.fullAddress}\n`] +
                            ['Ежедневно\n'] +
                            [`${hoursFrom} – ${hoursTo}`];

                        await this.allure.runStep(
                            'На карточке адреса доставки должен отображаться выбранный ПВЗ', async () => {
                                await this.addressBlocks.getInfoTitleByCardIndex(0, 'Доставка')
                                    .should.eventually.include(
                                        outletInfo,
                                        `Текст в поле адрес должен быть "${outletInfo}".`
                                    );
                            }
                        );
                    }
                );

                await this.contactCard.getContactText()
                    .should.eventually.to.be.equal(
                        CONTACTS.HSCH_CONTACT.recipientFullInfo,
                        'На карточке получателя должны отображаться данные указанные пользователем'
                    );

                await this.allure.runStep('Переходим к оплате', async () => {
                    await this.checkoutOrderButton.waitForEnabledButton();

                    await this.browser.yaWaitForChangeUrl(
                        () => this.checkoutOrderButton.click(),
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
                });
            },
        }),
    },
});
