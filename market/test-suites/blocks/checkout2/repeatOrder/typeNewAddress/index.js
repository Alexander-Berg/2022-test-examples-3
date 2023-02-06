import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    goToTypeNewAddressRepeatOrder,
    switchToSpecifiedDeliveryForm,
} from '@self/root/src/spec/hermione/scenarios/checkout';

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
// eslint-disable-next-line max-len
import DeliveryActionButton from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/DeliveryActionButton/__pageObject';

import {ACTUALIZATION_TIMEOUT} from '@self/root/src/spec/hermione/scenarios/checkout';
import {CONTACTS} from '../../constants';

const address = 'Москва, Рочдельская улица, д. 20, 1';

export default makeSuite('Новый пресет.', {
    id: 'marketfront-4629',
    issue: 'MARKETFRONT-45593',
    feature: 'Новый пресет.',
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
                popupBase: () => this.createPageObject(PopupBase, {
                    root: `${PopupBase.root} [data-auto="editableCardPopup"]`,
                }),
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
                deliveryActionButton: () => this.createPageObject(DeliveryActionButton),
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

            await this.browser.setState(`persAddress.contact.${CONTACTS.DEFAULT_CONTACT.id}`, CONTACTS.DEFAULT_CONTACT);
            await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});
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
                await this.deliveryTypes.waitForVisible();
            },
            'Слева отображается блок "Как доставить заказ?", выбран способ доставки "Курьером".': makeCase({
                async test() {
                    const titleText = 'Как доставить заказ?';
                    const region = 'Москва';

                    await this.browser.allure.runStep(
                        'Слева присутствует блок "Как доставить заказ?".',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.getTitleText()
                                .should.eventually.to.be.equal(
                                    titleText,
                                    `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        'По дефолту отображается способ доставки "Курьером" с формой ввода адреса.',
                        async () => {
                            await this.deliveryTypes.isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                        }
                    );

                    await this.browser.allure.runStep(
                        'В поле саджеста указан регион - Москва.',
                        async () => {
                            await this.courierSuggest.waitForVisible();
                            await this.courierSuggestInput.getText()
                                .should.eventually.to.be.equal(
                                    region,
                                    `Текст в поле адрес должен быть "${region}".`
                                );
                        }
                    );
                },
            }),
            'Ввести в поле адрес "Москва, Рочдельская улица, д. 20" и квартиру "1".': {
                async beforeEach() {
                    const newAddress = 'Рочдельская улица, 20';
                    const apartament = '1';

                    await this.browser.yaScenario(this, switchToSpecifiedDeliveryForm);
                    await this.addressForm.waitForVisible();
                    await this.street.setTextAndSelect(newAddress);
                    await this.street.waitForHideSuggestion(newAddress);
                    const tooltipAddress = 'Москва, Рочдельская улица, д. 20';
                    await this.allure.runStep(
                        'Над пином указан адрес',
                        () => this.tooltipAddress.getText().should.eventually.to.be.equal(
                            tooltipAddress,
                            `Адрес над пином "${tooltipAddress}"`
                        )
                    );
                    await this.addressForm.setApartamentField(apartament);
                },
                'Для перехода к странице подтверждения, нажать кнопку "Выбрать".': {
                    async beforeEach() {
                        await this.browser.allure.runStep(
                            'Нажать кнопку "Выбрать".',
                            async () => {
                                await this.browser.allure.runStep(
                                    'Ожидаем доступности кнопки "Выбрать".',
                                    async () => {
                                        await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton();
                                        await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                            .should.eventually.to.be.equal(
                                                false,
                                                'Кнопка "Выбрать" должна быть активна.'
                                            );
                                    }
                                );

                                await this.deliveryEditorCheckoutWizard.submitButtonClick();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Ожидаем появления главной страницы чекаута.',
                            async () => {
                                await this.confirmationPage.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Введенный адрес отображается в блоке информации о доставке.',
                            async () => {
                                await this.deliveryInfo.waitForVisible();
                                await this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        address,
                                        `Текст в поле адрес должен быть "${address}".`
                                    );
                            }
                        );
                    },
                    'Нажать на кнопку "Изменить" в блоке заказа.': {
                        async beforeEach() {
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
                                'Ожидаем появления попапа выбора адреса доставки.',
                                async () => {
                                    await this.editPopup.waitForVisibleRoot();
                                    await this.popupDeliveryTypes.waitForVisible();
                                }
                            );
                        },
                        'Выбран способ доставки Курьером, пресет выбран и в способе доставки отображается введенный адрес.': makeCase({
                            async test() {
                                await this.browser.allure.runStep(
                                    'Выбран способ доставки "Курьером".',
                                    async () => {
                                        await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                            .should.eventually.to.be.equal(
                                                true,
                                                'Должна отображаться доставка "Курьером."'
                                            );
                                    }
                                );

                                await this.browser.allure.runStep(
                                    'Пресет выбран (подсвечен желтой рамкой).',
                                    async () => {
                                        await this.addressList.isAnyItemActive()
                                            .should.eventually.to.be.equal(true, 'Пресет должен быть выделен.');
                                    }
                                );

                                await this.browser.allure.runStep(
                                    'В способе доставки курьером выбран пресет с адресом доставки "Москва, Рочдельская улица, д. 20, 1".',
                                    async () => {
                                        await this.addressList.getActiveItemText()
                                            .should.eventually.to.be.equal(
                                                address,
                                                `Текст карточки должен быть "${address}".`
                                            );
                                    }
                                );
                            },
                        }),
                        'Для скрытия попапа с пресетами, нажать на крестик в правом верхнем углу.': {
                            async beforeEach() {
                                await this.browser.allure.runStep(
                                    'Нажать на крестик.',
                                    async () => {
                                        await this.popupBase.isCrossVisible()
                                            .should.eventually.to.be.equal(true, 'Должен отображаться крестик.');
                                        await this.popupBase.clickOnCrossButton();
                                    }
                                );

                                await this.browser.allure.runStep(
                                    'В блоке информации о доставки отображается адрес "Москва, Рочдельская улица, д. 20, 1".',
                                    async () => {
                                        await this.editPopup.waitForRootInvisible();
                                        await this.deliveryInfo.waitForVisible();

                                        await this.addressCard.getText()
                                            .should.eventually.to.be.equal(
                                                address,
                                                `Текст в поле адрес должен быть "${address}".`
                                            );
                                    }
                                );
                            },
                            'Нажать на кнопку "Изменить" в блоке заказа.': {
                                async beforeEach() {
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
                                        'Откроется попап выбора адресов доставки.',
                                        async () => {
                                            await this.editPopup.waitForVisibleRoot();
                                            await this.popupDeliveryTypes.waitForVisible();
                                        }
                                    );
                                },
                                'Выбран способ доставки Курьером, пресет выбран и в способе доставки отображается введенный адрес.': makeCase({
                                    async test() {
                                        await this.browser.allure.runStep(
                                            'Выбран способ доставки "Курьером".',
                                            async () => {
                                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                                    .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                                            }
                                        );

                                        await this.browser.allure.runStep(
                                            'Пресет выбран (подсвечен желтой рамкой).',
                                            async () => {
                                                await this.addressList.isAnyItemActive()
                                                    .should.eventually.to.be.equal(true, 'Пресет должен быть выделен.');
                                            }
                                        );

                                        await this.browser.allure.runStep(
                                            'В способе доставки курьером выбран пресет с адресом доставки "Москва, Рочдельская улица, д. 20, 1".',
                                            async () => {
                                                await this.addressList.getActiveItemText()
                                                    .should.eventually.to.be.equal(
                                                        address,
                                                        `Текст карточки должен быть "${address}".`
                                                    );
                                            }
                                        );
                                    },
                                }),
                                'Для возврата в чекаут, нажать на кнопку "Выбрать".': {
                                    async beforeEach() {
                                        await this.browser.allure.runStep(
                                            'Нажать на кнопку "Выбрать".',
                                            async () => {
                                                await this.editPopup.waitForChooseButtonEnabled();
                                                await this.editPopup.chooseButtonClick();
                                            }
                                        );

                                        await this.browser.allure.runStep(
                                            'В блоке информации о доставки отображается адрес "Москва, Рочдельская улица, д. 20, 1".',
                                            async () => {
                                                await this.editPopup.waitForRootInvisible();
                                                await this.deliveryInfo.waitForVisible();

                                                await this.addressCard.getText()
                                                    .should.eventually.to.be.equal(
                                                        address,
                                                        `Текст в поле адрес должен быть "${address}".`
                                                    );
                                            }
                                        );
                                    },
                                    'Перезагрузить страницу.': {
                                        async beforeEach() {
                                            await this.browser.yaPageReload(5000, ['state']);

                                            await this.browser.allure.runStep(
                                                'Ожидаем появления главной страницы чекаута.',
                                                async () => {
                                                    await this.confirmationPage.waitForVisible();
                                                }
                                            );
                                        },
                                        'Нажать на кнопку "Изменить" в блоке заказа.': makeCase({
                                            async test() {
                                                await this.browser.allure.runStep(
                                                    'Нажать на кнопку "Изменить".',
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
                                                    'Откроется попап выбора адресов доставки.',
                                                    async () => {
                                                        await this.editPopup.waitForVisibleRoot();
                                                        await this.popupDeliveryTypes.waitForVisible();
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Выбран способ доставки Курьером.',
                                                    async () => {
                                                        await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                                            .should.eventually.to.be.equal(true, 'Должна отображаться доставка "Курьером."');
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Присутствует пресет с адресом доставки "Москва, Рочдельская улица, д. 20, 1".',
                                                    async () => {
                                                        await this.addressList.isCardWithAddressExisting(address)
                                                            .should.eventually.to.be.equal(true, `Должен отображаться пресет с адресом "${address}".`);
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Выбрать данный пресет.',
                                                    async () => {
                                                        await this.addressList.clickAddressListItemByAddress(address);
                                                        await this.addressList.isAnyItemActive()
                                                            .should.eventually.to.be.equal(
                                                                true,
                                                                'Пресет должен быть выделен.'
                                                            );
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Нажать на кнопку "Выбрать".',
                                                    async () => {
                                                        await this.editPopup.waitForChooseButtonEnabled();
                                                        await this.editPopup.chooseButtonClick();
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'В блоке информации о доставки отображается адрес "Москва, Рочдельская улица, д. 20, 1".',
                                                    async () => {
                                                        await this.editPopup.waitForRootInvisible();
                                                        await this.deliveryInfo.waitForVisible();

                                                        await this.addressCard.getText()
                                                            .should.eventually.to.be.equal(
                                                                address,
                                                                `Текст в поле адрес должен быть "${address}".`
                                                            );
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Ожидаем доступности кнопки перейти к оплате под саммари.',
                                                    async () => {
                                                        await this.checkoutOrderButton.waitForEnabledButton();
                                                    }
                                                );

                                                await this.browser.allure.runStep(
                                                    'Ожидаем изменения урла на: "/my/orders/payment".',
                                                    async () => {
                                                        await this.browser.yaWaitForChangeUrl(
                                                            async () => {
                                                                await this.checkoutOrderButton.click();
                                                            },
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
                                                    }
                                                );
                                            },
                                        }),
                                    },
                                },
                            },
                        },
                    },
                },
            },
        },
    },
});
