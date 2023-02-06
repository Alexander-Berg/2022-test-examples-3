import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES} from '../../constants';

export default makeSuite('Способ доставки "Курьером".', {
    feature: 'Способ доставки "Курьером".',
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
