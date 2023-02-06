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
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CartItemsDetails from '@self/root/src/components/Checkout/CartItemsDetails/__pageObject';
import OrderItemsList from '@self/root/src/components/OrderItemsList/__pageObject';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Пользователь с пресетами.', {
    id: 'marketfront-4630',
    issue: 'MARKETFRONT-45595',
    feature: 'Пользователь с пресетами.',
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
                popupBase: () => this.createPageObject(PopupBase, {
                    root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                }),
                editPopup: () => this.createPageObject(EditPopup),
                cartItemsDetails: () => this.createPageObject(CartItemsDetails),
                orderItemsList: () => this.createPageObject(OrderItemsList, {
                    perent: this.cartItemsDetails,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
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
        'Перейти к главной странице чекаута.': {
            async beforeEach() {
                await this.confirmationPage.waitForVisible();
                await this.deliveryInfo.waitForVisible();
            },
            'Переход к попапу со списком пресетов по нажатию на кнопку "Изменить" в блоке заказа.': makeCase({
                async test() {
                    const expectedAddressesCount = 3;
                    const expectedSubtitlesCount = 2;

                    await this.addressEditableCard.isChangeButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                        );

                    await this.addressEditableCard.changeButtonClick();

                    await this.editPopup.waitForVisibleRoot();
                    await this.deliveryTypes.waitForVisible();

                    await this.allure.runStep(
                        'Выбран способ доставки Курьером.', async () =>
                            this.deliveryTypes.isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."')
                    );

                    await this.allure.runStep(
                        'Отображаются пресеты адресов для курьерской доставки.', async () =>
                            this.addressList.getAddressTitlesCount()
                                .should.eventually.to.be.equal(
                                    expectedAddressesCount,
                                    `Количество адресов должно быть больше ${expectedAddressesCount}.`
                                )
                    );

                    await this.allure.runStep(
                        'Для каждого пресета видна информация о возможности/невозможности доставки посылок в пресет.', async () =>
                            this.addressList.getAddressSubtitlesCount()
                                .should.eventually.to.be.equal(
                                    expectedSubtitlesCount,
                                    'Каждый адрес должен иметь информацию о возможности доставки.'
                                )
                    );
                },
            }),
            'Нажать на текст с информацией о невозможности доставки посылок в пресет.': {
                async beforeEach() {
                    await this.addressEditableCard.isChangeButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                        );

                    await this.addressEditableCard.changeButtonClick();

                    await this.editPopup.waitForVisibleRoot();
                    await this.deliveryTypes.waitForVisible();

                    await this.addressList.clickOnFirstUnavailableSubtitle();
                    await this.popupBase.waitForVisible();
                },
                'Откроется окно с информацией о товарах, которые невозможно доставить.': makeCase({
                    async test() {
                        const title = 'Нельзя доставить на этот адрес';

                        await this.cartItemsDetails.waitForTitleVisible();
                        await this.cartItemsDetails.getTitleText()
                            .should.eventually.to.be.equal(title, `Заголовок должен быть ${title}.`);

                        await this.allure.runStep(
                            'Вверху формы есть стрелка возврата на предыдущее окно.', async () =>
                                this.cartItemsDetails.isBackArrowVisible()
                                    .should.eventually.to.be.equal(true, 'Стрелка должна быть видна.')
                        );

                        await this.allure.runStep(
                            'Вверху формы есть крестик.', async () =>
                                this.popupBase.isCrossVisible()
                                    .should.eventually.to.be.equal(true, 'Крестик должен быть виден.')
                        );

                        await this.allure.runStep(
                            'Присутствует список товаров.', async () =>
                                this.orderItemsList.isOrdersListVisible()
                                    .should.eventually.to.be.equal(true, 'Список товаров должен быть виден.')
                        );

                        await this.allure.runStep(
                            'Есть изображения товара.', async () =>
                                this.orderItemsList.isEveryImageExisting()
                                    .should.eventually.to.be.equal(true, 'Картинка товара должена быть видна для каждой позиции.')
                        );

                        await this.allure.runStep(
                            'Есть названия товаров.', async () =>
                                this.orderItemsList.isEveryTitlesExisting()
                                    .should.eventually.to.be.equal(true, 'Название товара должено быть видно для каждой позиции.')
                        );

                        await this.allure.runStep(
                            'Есть количество штук каждой позиции.', async () =>
                                this.orderItemsList.isEveryCountExisting()
                                    .should.eventually.to.be.equal(true, 'Количество товара должено быть видно для каждой позиции.')
                        );

                        await this.allure.runStep(
                            'Снизу формы есть кнопка "Хорошо".', async () =>
                                this.cartItemsDetails.isGoodButtonVisible()
                                    .should.eventually.to.be.equal(true, 'Кнопка "Хорошо" должна быть видна.')
                        );
                    },
                }),
                'Возврат к списку пресетов по нажатию на стрелку "назад" вверху формы.': makeCase({
                    async test() {
                        await this.cartItemsDetails.waitForBackArrowButtonVisible();
                        await this.cartItemsDetails.clickOnBackArrowButton();

                        await this.editPopup.waitForVisibleRoot();
                    },
                }),
                'Возврат к списку пресетов по нажатию на кнопку "Хорошо".': makeCase({
                    async test() {
                        await this.cartItemsDetails.waitForGoodButtonVisible();
                        await this.cartItemsDetails.clickOnGoodButton();

                        await this.editPopup.waitForVisibleRoot();
                    },
                }),
                'Возврат к списку пресетов по нажатию на крестик в углу формы.': makeCase({
                    async test() {
                        await this.popupBase.clickOnCrossButton();

                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    },
                }),
            },
        },
    },
});
