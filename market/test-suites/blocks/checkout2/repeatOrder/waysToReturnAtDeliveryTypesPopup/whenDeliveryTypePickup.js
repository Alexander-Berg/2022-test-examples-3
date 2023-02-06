import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';
import x5outlet from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/x5outlet';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES} from '../../constants';

export default makeSuite('Способ доставки "Самовывоз".', {
    feature: 'Способ доставки "Самовывоз".',
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
        'Открыть главную страницу чекаута.': {
            async beforeEach() {
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
                        await this.popupDeliveryTypes.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Выбрать способ доставки "Самовывоз"',
                    async () => {
                        await this.popupDeliveryTypes.setDeliveryTypePickup();
                        await this.browser.allure.runStep(
                            'Выбран способ доставки "Самовывоз".',
                            async () => {
                                await this.popupDeliveryTypes.isCheckedDeliveryTypePickup()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен быть выбран способ доставки "Самовывоз".'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Отображается список пресетов с доступными ПВЗ.',
                    async () => {
                        await this.pickupList.isRootVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Выбран пресет с доступным ПВЗ.',
                    async () => {
                        await this.pickupList.getActiveItemText()
                            .should.eventually.to.be.include(
                                x5outlet.address.fullAddress,
                                `Текст выбранного ПВЗ должен содержать "${x5outlet.address.fullAddress}".`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Выбрать".',
                    async () => {
                        await this.editPopup.chooseButtonClick();

                        await this.browser.allure.runStep(
                            'Попап "Способ доставки" закрывается.',
                            async () => {
                                await this.editPopup.waitForHidden();
                            }
                        );

                        await this.browser.allure.runStep(
                            'На экране отображается информация о выбранном ПВЗ.',
                            async () => {
                                await this.pickupCard.getContentText()
                                    .should.eventually.to.be.include(
                                        x5outlet.address.fullAddress,
                                        `Текст в поле адрес должен быть "${x5outlet.address.fullAddress}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Изменить" в блоке заказа.',
                    async () => {
                        await this.addressEditableCard.changeButtonClick();
                    }
                );

                await this.browser.allure.runStep(
                    'Выбрать способ доставки "Курьером"',
                    async () => {
                        await this.popupDeliveryTypes.setDeliveryTypeDelivery();
                        await this.browser.allure.runStep(
                            'Выбран способ доставки "Курьером".',
                            async () => {
                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен быть выбран способ доставки "Курьером".'
                                    );
                            }
                        );
                    }
                );

                /**
                 * @ticket MARKETFRONT-53506
                 * @start
                 * Удалить после починки
                 */
                await this.browser.allure.runStep(
                    `Выбрать пресет с адресом "${ADDRESSES.MOSCOW_ADDRESS.address}"`,
                    async () => {
                        await this.addressList.clickItemByAddress(ADDRESSES.MOSCOW_ADDRESS.address);
                    }
                );
                /**
                 * @ticket MARKETFRONT-53506
                 * @end
                 */

                await this.browser.allure.runStep(
                    'В пресете нажать на кнопку "Карандаш"',
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
            },
            'В попапе "Изменить адрес" нажать на кнопку "Отмена".': makeCase({
                async test() {
                    await this.editAddressPopup.clickCancelButton();

                    await this.browser.allure.runStep(
                        'Возврат к попапу "Способ доставки".',
                        async () => {
                            await this.browser.allure.runStep(
                                'Отображается попап "Способ доставки".',
                                async () => {
                                    await this.editPopup.waitForVisibleRoot();
                                    await this.popupDeliveryTypes.waitForVisible();
                                }
                            );

                            await this.browser.allure.runStep(
                                'Выбран способ доставки "Курьером".',
                                async () => {
                                    await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                        .should.eventually.to.be.equal(
                                            true,
                                            'Должен быть выбран способ доставки "Курьером".'
                                        );
                                }
                            );

                            await this.browser.allure.runStep(
                                'Отображается список пресетов с адресами доставки курьером.',
                                async () => {
                                    await this.addressList.isRootVisible();

                                    await this.addressList.isCardWithAddressExisting(ADDRESSES.MOSCOW_ADDRESS.address)
                                        .should.eventually.to.be.equal(
                                            true,
                                            `Должен отображаться пресет с адресом "${ADDRESSES.MOSCOW_ADDRESS.address}".`
                                        );
                                }
                            );
                        }
                    );
                },
            }),
            'В попапе "Изменить адрес" нажать на "Стрелочку".': makeCase({
                async test() {
                    await this.editAddressPopup.clickBackArrowButton();

                    await this.browser.allure.runStep(
                        'Возврат к попапу "Способ доставки".',
                        async () => {
                            await this.browser.allure.runStep(
                                'Отображается попап "Способ доставки".',
                                async () => {
                                    await this.editPopup.waitForVisibleRoot();
                                    await this.popupDeliveryTypes.waitForVisible();
                                }
                            );

                            await this.browser.allure.runStep(
                                'Выбран способ доставки "Курьером".',
                                async () => {
                                    await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                        .should.eventually.to.be.equal(
                                            true,
                                            'Должен быть выбран способ доставки "Курьером".'
                                        );
                                }
                            );

                            await this.browser.allure.runStep(
                                'Отображается список пресетов с адресами доставки курьером.',
                                async () => {
                                    await this.addressList.isRootVisible();

                                    await this.addressList.isCardWithAddressExisting(ADDRESSES.MOSCOW_ADDRESS.address)
                                        .should.eventually.to.be.equal(
                                            true,
                                            `Должен отображаться пресет с адресом "${ADDRESSES.MOSCOW_ADDRESS.address}".`
                                        );
                                }
                            );
                        }
                    );
                },
            }),
        },
    },
});
