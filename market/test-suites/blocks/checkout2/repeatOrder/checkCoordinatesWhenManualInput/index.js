import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder, goToTypeNewAddressRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import EditPopup from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import AddressList from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import PinMap from '@self/root/src/components/VectorPinMap/__pageObject';
import CourierSuggest
    from '@self/root/src/widgets/content/checkout/common/CheckoutDeliveryEditor/components/CourierSuggest/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import Tooltip from '@self/root/src/widgets/content/checkout/common/CheckoutPin/components/Tooltip/__pageObject';

const forestCoordinates = [55.83831449265888, 37.720007662109346];
const carts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Проброс координат адреса.', {
    id: 'marketfront-4997',
    issue: 'MARKETFRONT-51926',
    feature: 'Проброс координат адреса.',
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
                courierSuggest: () => this.createPageObject(CourierSuggest, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                    parent: this.courierSuggest,
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
                pinMap: () => this.createPageObject(PinMap, {}),
                tooltip: () => this.createPageObject(Tooltip, {
                    parent: this.pinMap,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                    parent: this.confirmationPage,
                }),
            });

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
        'Открыть страницу с картой.': {
            async beforeEach() {
                const zoom = 16;
                const lat = forestCoordinates[0];
                const lon = forestCoordinates[1];
                const lngLatCenter = [lon, lat];

                await this.deliveryTypes.waitForVisible();

                await this.pinMap.waitForVisible(5000);
                await this.pinMap.waitForReady(5000);

                await this.pinMap.setCenter(lngLatCenter, zoom);
            },
            'Переместить пин определения адреса на зеленую зону (лес).': makeCase({
                async test() {
                    await this.deliveryEditorCheckoutWizard.waitForSubmitButton();
                    await this.allure.runStep(
                        'Алерт об уточнении адреса отображается.', () =>
                            this.courierSuggestInput.getErroredText()
                                .should.eventually.to.be.equal(
                                    'Не удалось определить точный адрес',
                                    'Должно отображаться сообщение "Не удалось определить точный адрес"'
                                )
                    );
                },
            }),
            'Ввести вручную улицу и дом.': makeCase({
                async test() {
                    const address = 'Москва, Рочдельская улица, д. 20';
                    const street = 'Рочдельская улица';
                    const house = '20';

                    await this.tooltip.needCorrectionButtonClick();

                    await this.addressForm.setStreetField(street);
                    await this.addressForm.setHouseField(house);

                    await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                    await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                        .should.eventually.to.be.equal(false, 'Кнопка "Выбрать" должна быть активна.');

                    await this.deliveryEditorCheckoutWizard.submitButtonClick();
                    await this.confirmationPage.waitForVisible();
                    await this.deliveryInfo.waitForVisible();

                    await this.allure.runStep(
                        'Введенный адрес отображается в блоке информации о доставке.', () =>
                            this.addressCard.getText()
                                .should.eventually.to.be.equal(
                                    address,
                                    `Текст в поле адрес должен быть "${address}".`
                                )
                    );
                },
            }),
        },
    },
});
