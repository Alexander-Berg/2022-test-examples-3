import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import {ADDRESSES} from '../../constants';

export default makeSuite('Алерт о невалидности адреса.', {
    id: 'marketfront-4969',
    issue: 'MARKETFRONT-51887',
    feature: 'Алерт о невалидности адреса.',
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
                deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                deliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                popupBase: () => this.createPageObject(PopupBase),
                editPopup: () => this.createPageObject(EditPopup),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                street: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                    parent: this.confirmationPage,
                }),
            });

            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.setState(`persAddress.address.${ADDRESSES.ADDRESS_WITH_INVALID_FIELD.id}`, ADDRESSES.ADDRESS_WITH_INVALID_FIELD);

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
        'Открыть главную страницу чекаута.': {
            async beforeEach() {
                await this.confirmationPage.waitForVisible();
            },
            'Нажать на кнопку "Изменить" в блоке заказа.': {
                async beforeEach() {
                    await this.addressEditableCard.isChangeButtonDisabled()
                        .should.eventually.to.be.equal(
                            false,
                            'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                        );

                    await this.addressEditableCard.changeButtonClick();
                },
                'Откроется попап выбора адресов доставки.': {
                    async beforeEach() {
                        await this.editPopup.waitForVisibleRoot();
                        await this.popupDeliveryTypes.waitForVisible();
                    },
                    'Присутствует пресет с адресом доставки "Москва, Самотёчная улица, д. 5, 012345678012345"': makeCase({
                        async test() {
                            const address = 'Москва, Самотёчная улица, д. 5, 012345678012345';
                            await this.addressList.isCardWithAddressExisting(address)
                                .should.eventually.to.be.equal(true, `Должен отображаться пресет с адресом "${address}".`);
                        },
                    }),
                    'Выбрать пресет с адресом доставки "Москва, Самотёчная улица, д. 5, 012345678012345".': {
                        async beforeEach() {
                            const address = 'Москва, Самотёчная улица, д. 5, 012345678012345';

                            await this.addressList.clickItemByAddress(address);
                            await this.editPopup.waitForChooseButtonEnabled();
                        },
                        'На пресете отображается аллерт "Превышено ограничение в 10 символов"': makeCase({
                            async test() {
                                const address = 'Москва, Самотёчная улица, д. 5, 012345678012345';
                                const invalidText = 'Превышено ограничение в 10 символов';

                                await this.addressList.isCardWithAddressExisting(address)
                                    .should.eventually.to.be.equal(true, `Должен отображаться пресет с адресом "${address}".`);

                                await this.addressList.isCardWithAddressAndSubtitleExisting(address, invalidText)
                                    .should.eventually.to.be.equal(true, `Должен отображаться пресет с адресом "${address}" и ошибкой "${invalidText}".`);
                            },
                        }),
                        'Кнопка "Выбрать" недоступна.': makeCase({
                            async test() {
                                await this.editPopup.isChooseButtonDisabled()
                                    .should.eventually.to.be.equal(true, 'Кнопка "Выбрать" должна быть недоступна.');
                            },
                        }),
                    },
                },
            },
        },
    },
});
