import {makeSuite} from 'ginny';

// scenarios
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

// pageObjects
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// cases
import returnToDeliveryPopupByBackButton from './returnToDeliveryPopupByBackButton';
import returnToDeliveryPopupByAnotherRegion from './returnToDeliveryPopupByAnotherRegion';
import returnToDeliveryPopupByUnavailableAddress from './returnToDeliveryPopupByUnavailableAddress';

import {ADDRESSES, CONTACTS} from '../../constants';

const unavailableRegionId = 11333;
const simpleCarts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Создание адреса.', {
    issue: 'MARKETFRONT-54555',
    feature: 'Создание адреса.',
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
                popupBase: () => this.createPageObject(PopupBase, {
                    root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                }),
                editPopup: () => this.createPageObject(EditPopup),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList, {
                    parent: this.editPopup,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.deliveryEditorCheckoutWizard,
                }),
                street: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
            });

            await this.browser.setState('regionsWithoutDelivery', [unavailableRegionId]);
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
                prepareCheckouterPageWithCartsForRepeatOrder,
                {
                    carts: simpleCarts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Возврат к попапу "Способ доставки".': {
            async beforeEach() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'В блоке доставки нажать кнопку "Изменить".', async () => {
                        await this.addressEditableCard.changeButtonClick();
                    }
                );

                await this.browser.allure.runStep(
                    'Открывается попап "Способ доставки"',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                        await this.popupDeliveryTypes.waitForVisible();

                        await this.browser.allure.runStep(
                            'Выбран способ доставки "Курьером".',
                            async () => {
                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                    .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Добавить новый адрес".',
                    async () => {
                        await this.editPopup.addButtonClick();

                        await this.allure.runStep(
                            'Открывается экран "Как доставить заказ?"', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForVisible();
                            }
                        );
                    }
                );
            },
            'По нажатию на кнопку "Назад" на экране "Как доставить заказ?".': returnToDeliveryPopupByBackButton,
            'Создание адреса, который не соответствует выбранному региону на сайте.': returnToDeliveryPopupByAnotherRegion,
            'Отсутствие возможности создания адреса, по которому доставка недоступна.': returnToDeliveryPopupByUnavailableAddress,
        },
    },
});
