import {
    makeSuite,
    makeCase,
} from 'ginny';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {
    offerMock as farmaOfferMock,
    skuMock as farmaSkuMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import {
    deliveryDeliveryMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// scenarious
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
    ACTUALIZATION_TIMEOUT,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject/index.touch';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CartItemsDetails
    from '@self/root/src/components/Checkout/CartItemsDetails/__pageObject/index.touch';
import OrderItemsList
    from '@self/root/src/components/OrderItemsList/__pageObject';

import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {ADDRESSES, CONTACTS} from '../constants';

const address = ADDRESSES.MOSCOW_ADDRESS;
const unavailableAddress = ADDRESSES.VOLGOGRAD_ADDRESS;
const unavailableMassage = 'Недоступен для этих товаров';
const unavailableCardText = `${unavailableAddress.address}\n${unavailableMassage}`;

export default makeSuite('Presets 2.0. Сообщение о невозможности доставки посылок в пресет.', {
    id: 'm-touch-3763',
    issue: 'MARKETFRONT-48979',
    feature: 'Presets 2.0. Сообщение о невозможности доставки посылок в пресет.',
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
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                cartItemDetails: () => this.createPageObject(CartItemsDetails),
                orderItemsList: () => this.createPageObject(OrderItemsList, {
                    perent: this.cartItemsDetails,
                }),
            });
            const carts = [
                buildCheckouterBucket({
                    cartIndex: 0,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryDeliveryMock,
                    }],
                }),
                buildCheckouterBucket({
                    cartIndex: 1,
                    items: [{
                        skuMock: farmaSkuMock,
                        offerMock: farmaOfferMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryDeliveryMock,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: address,
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: unavailableAddress,
                    contact: CONTACTS.DEFAULT_CONTACT,
                    carts: carts.map(cart => ({
                        label: cart.label,
                        deliveryAvailable: false,
                    })),
                    deliveryType: DELIVERY_TYPES.DELIVERY,
                }
            );

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
        'Отображаем сообщение о невозможности доставки посылок в пресет.': makeCase({
            async test() {
                await this.allure.runStep(
                    'Открыть страницу чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Выбрать пункт выдачи" в блоке заказа.',
                    async () => {
                        await this.deliveryInfo.click();
                        await this.popupDeliveryTypes.waitForVisible();
                    }
                );
                await this.browser.allure.runStep(
                    'Присутствуют чипсы способов доставки "Самовывоз", "Курьер".',
                    async () => {
                        await this.browser.allure.runStep(
                            'Выбран способ доставки "Курьер".',
                            async () => {
                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен быть выбран способ доставки "Курьер".'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Присутствует чипс "Самовывоз".',
                            async () => {
                                await this.popupDeliveryTypes.deliveryTypePickupIsExisting()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен отображаться способ доставки "Самовывоз".'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    `Присутствует пресет с адресом "${unavailableAddress.address}" и сообщением "${unavailableMassage}".`,
                    async () => {
                        await this.addressList.getCardIndexByAddress(unavailableCardText)
                            .should.eventually.not.to.be.equal(
                                -1,
                                `Должен присутствовать пресет с текстом "${unavailableCardText}".`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'На недоступном пресете нажать на информационный значек рядом с надписью "Не подходит для этих товаров"',
                    async () => {
                        await this.addressList.clickOnFirstUnavailableSubtitle();
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем появления попапа "Нельзя доставить на этот адрес".',
                    async () => {
                        await this.cartItemDetails.waitForVisibleRoot();

                        await this.browser.allure.runStep(
                            'Отображается информация о товарах, которые невозможно доставить на этот адрес.',
                            async () => {
                                await this.orderItemsList.isOrdersListVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен отображаться список недоступных товаров.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на крестик.',
                    async () => {
                        await this.cartItemDetails.clickCrossButton();
                        await this.cartItemDetails.waitForInvisibleRoot();

                        await this.browser.allure.runStep(
                            'Переход в модалку выбора способов доставки',
                            async () => {
                                await this.popupDeliveryTypes.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    `Нажать на пресет с адресом "${unavailableAddress.address}".`,
                    async () => {
                        await this.addressList.clickAddressListItemByAddress(unavailableCardText);

                        await this.browser.allure.runStep(
                            `Активным отображается отображается адрес "${address.address}".`,
                            async () => {
                                await this.addressList.isItemWithAddressChecked(address.address)
                                    .then(({value}) => value)
                                    .should.eventually.to.be.equal(
                                        'true',
                                        `Активным должен быть адрес "${address.address}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    `Нажать на пресет с адресом "${address.address}".`,
                    async () => {
                        await this.addressList.clickAddressListItemByAddress(address.address);

                        await this.editPopup.waitForChooseButtonEnabled();
                        await this.editPopup.isChooseButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Привезти сюда" должна быть активна.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Привезти сюда".',
                    async () => {
                        await this.editPopup.chooseButtonClick();

                        await this.browser.allure.runStep(
                            `В блоке доставки отображается адрес "${address.address}".`,
                            async () => {
                                if (await this.preloader.waitForVisible(1000)) {
                                    await this.preloader.waitForHidden(ACTUALIZATION_TIMEOUT);
                                }

                                await this.browser.allure.runStep(
                                    `В блоке доставки отображается адрес "${address.address}".`,
                                    async () => {
                                        await this.addressCard.getText()
                                            .should.eventually.to.be.equal(
                                                address.address,
                                                `Должен отображаться адрес "${address.address}".`
                                            );
                                    }
                                );
                            }
                        );
                    }
                );
            },
        }),
    },
});
