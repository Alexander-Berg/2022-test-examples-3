import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder, addPresetForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import * as televizor from '@self/root/src/spec/hermione/kadavr-mock/report/televizor';
import * as farma from '@self/root/src/spec/hermione/kadavr-mock/report/farma';
import {
    deliveryDeliveryMock,
} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Выбор недоступного пресета.', {
    id: 'marketfront-4630',
    issue: 'MARKETFRONT-45595',
    feature: 'Выбор недоступного пресета.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: false,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                popupBase: () => this.createPageObject(PopupBase),
                editPopup: () => this.createPageObject(EditPopup),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                street: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
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
                        skuMock: televizor.skuMock,
                        offerMock: televizor.offerMock,
                        count: 1,
                    }],
                    deliveryOptions: [{
                        ...deliveryDeliveryMock,
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
                        ...deliveryDeliveryMock,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.MOSCOW_ADDRESS,
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );
            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.SPB_ADDRESS,
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
                addPresetForRepeatOrder,
                {
                    address: ADDRESSES.VOLGOGRAD_ADDRESS,
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
                    'Перейти в чекаут.', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'Переход к попапу со списком пресетов по нажатию на кнопку "Изменить" в блоке заказа.', async () => {
                        await this.addressEditableCard.isChangeButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                            );

                        await this.addressEditableCard.changeButtonClick();
                        await this.editPopup.waitForVisibleRoot();
                    }
                );

                await this.allure.runStep(
                    'Выбрать пресет, куда нельзя доставить хотя бы 1 посылку.', async () => {
                        const address = ADDRESSES.SPB_ADDRESS.address;
                        await this.addressList.clickItemByAddress(address);

                        await this.allure.runStep(
                            'Пресет недоступен для выбора, активным остался предыдущий доступный пресет.',
                            async () => {
                                await this.addressList.getActiveItemText()
                                    .should.eventually.to.be.equal(
                                        ADDRESSES.MOSCOW_ADDRESS.address,
                                        'Предыдущий доступный пресет должен остаться активным.'
                                    );
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Выбран пресет, куда можно доставить все посылки.', async () => {
                        await this.addressList.getActiveItemText()
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_ADDRESS.address,
                                'Должен быть выбран пресет куда можно доставить все посылки.'
                            );
                        await this.allure.runStep(
                            'Проверка доступности кнопки "Выбрать".', async () => {
                                await this.editPopup.waitForEnabledChooseButton();
                                await this.editPopup.isChooseButtonDisabled()
                                    .should.eventually.to.be.equal(false, 'Кнопка "Выбрать" должна быть доступна.');
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Возврат к главной странице чекаута по нажатию кнопку "Выбрать".', async () => {
                        await this.editPopup.chooseButtonClick();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'В блоке информации о заказе указан адрес из выбранного пресета.', async () => {
                        const address = ADDRESSES.MOSCOW_ADDRESS.address;
                        return this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                address,
                                `Текст в поле адрес должен быть "${address}".`
                            );
                    }
                );
            },
        }),
    },
});
