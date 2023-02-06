import {makeCase, makeSuite} from 'ginny';

// scenarios
import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// pageObjects
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import EditAddressPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditAddressPopup/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import PopupBase from '@self/root/src/components/PopupBase/__pageObject';

import {ADDRESSES} from '../../constants';

const address = ADDRESSES.MOSCOW_ADDRESS;
const carts = [
    buildCheckouterBucket({
        items: [{
            skuMock: kettle.skuMock,
            offerMock: kettle.offerMock,
            count: 1,
        }],
    }),
];

export default makeSuite('Возврат к попапу "Изменить адрес".', {
    feature: 'Возврат к попапу "Изменить адрес".',
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
                editAddressPopup: () => this.createPageObject(EditAddressPopup, {
                    parent: this.popupBase,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
                addressForm: () => this.createPageObject(AddressForm, {
                    parent: this.editAddressPopup,
                }),
            });
            await this.browser.setState(`persAddress.address.${address.id}`, address);
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
        'При нажатии на кнопку "Назад" на экране "Куда доставить заказ?".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Изменить" в блоке заказа.',
                    async () => {
                        await this.addressEditableCard.isChangeButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'На карточке блока доставки должна отображатся кнопка "Изменить" и быть активной.'
                            );

                        await this.addressEditableCard.changeButtonClick();
                    }
                );

                await this.browser.allure.runStep(
                    'Открывается попап "Способ доставки"',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                    }
                );

                await this.browser.allure.runStep(
                    'В активном пресете нажать на кнопку "Карандаш"',
                    async () => {
                        await this.addressList.clickOnEditButtonByAddress(ADDRESSES.MOSCOW_ADDRESS.address);
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем появления формы "Изменить адрес".',
                    async () => {
                        await this.editAddressPopup.waitForEditFragmentVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Под полем "Адрес" нажать на кнопку "Выбрать на карте".',
                    async () => {
                        await this.editAddressPopup.clickGoToMapLink();

                        await this.allure.runStep(
                            'Открывается экран "Как доставить заказ?"', async () => {
                                await this.deliveryEditorCheckoutWizard.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Назад".',
                    async () => {
                        await this.deliveryEditorCheckoutWizard.backButtonClick();

                        await this.browser.allure.runStep(
                            'Происходит возврат к попапу "Изменить адрес".',
                            async () => {
                                await this.confirmationPage.waitForVisible();
                                await this.editAddressPopup.waitForEditFragmentVisible();
                            }
                        );
                    }
                );
            },
        }),
    },
});
