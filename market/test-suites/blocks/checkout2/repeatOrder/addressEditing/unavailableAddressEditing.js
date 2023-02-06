import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addressFieldChecker, addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';

import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';
import {ADDRESSES, CONTACTS} from '../../constants';

const newApartament = '42';
const newUnavailableAddress = `${ADDRESSES.VOLGOGRAD_ADDRESS.address}, ${newApartament}`;

export default makeSuite('Редактирование недоступного пресета.', {
    feature: 'Редактирование недоступного пресета.',
    params: {
        region: 'Регион',
        isAuth: 'Авторизован ли пользователь',
        carts: 'Корзины',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
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
                    address: ADDRESSES.VOLGOGRAD_ADDRESS,
                    contact: CONTACTS.DEFAULT_CONTACT,
                    carts: this.params.carts.map(cart => ({
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
                    carts: this.params.carts,
                    options: {
                        region: this.params.region,
                        checkout2: true,
                    },
                }
            );
        },
        'Открыть главную страницу чекаута.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    `В блоке доставки отображается адрес ${ADDRESSES.MOSCOW_ADDRESS.address}`,
                    async () => {
                        await this.addressCard.getText()
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_ADDRESS.address,
                                'Текст в блоке адреса должен соответствовать отредактированному адресу.'
                            );
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
                        await this.popupDeliveryTypes.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    `В активном пресете отображается адрес ${ADDRESSES.MOSCOW_ADDRESS.address}.`,
                    async () => {
                        await this.addressList.getActiveItemText()
                            .should.eventually.to.be.equal(
                                ADDRESSES.MOSCOW_ADDRESS.address,
                                `Текст активного пресета должен быть ${ADDRESSES.MOSCOW_ADDRESS.address}.`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'В недоступном пресете нажать на кнопку "Карандаш"',
                    async () => {
                        await this.addressList.clickOnEditButtonByAddress(ADDRESSES.VOLGOGRAD_ADDRESS.address);
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем появления формы "Изменить адрес".',
                    async () => {
                        await this.editAddressPopup.waitForEditFragmentVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Поле "Адрес" не кликабельно.',
                    async () => {
                        const addressSelector = await this.street.getSelector(this.street.input);
                        await this.browser.click(addressSelector);

                        await this.browser.getAttribute(addressSelector, 'disabled')
                            .should.eventually.to.be.equal(
                                'true',
                                'Поле "Адрес" должно быть не активно.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Поле "Квартира".',
                    async () => {
                        const apartamentFieldSelector = AddressForm.apartament;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: apartamentFieldSelector,
                                inputText: newApartament,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Сохранить".',
                    async () => {
                        await this.editAddressPopup.clickSaveButton();
                        await this.editAddressPopup.waitForEditFragmentInvisible();
                        await this.editPopup.waitForChooseButtonEnabled();

                        await this.browser.allure.runStep(
                            'Недоступный пресет отображается неактивным.',
                            async () => {
                                await this.addressList.getActiveItemText()
                                    .should.eventually.not.to.be.equal(
                                        newUnavailableAddress,
                                        'Текст активного пресета не должен соответствовать недоступному адресу.'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'У отредактированного пресета отображается сообщение "Недоступен для посылки".',
                            async () => {
                                await this.addressList.isCardWithAddressAndSubtitleExisting(
                                    newUnavailableAddress,
                                    'Недоступен для посылки'
                                )
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Текст подзаголовка пресета должен быть "Недоступен для посылки".'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
