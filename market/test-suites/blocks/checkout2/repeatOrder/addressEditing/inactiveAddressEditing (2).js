import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES} from '../constants';

const address = ADDRESSES.MINIMAL_ADDRESS;
const lastAddress = ADDRESSES.MOSCOW_ADDRESS;
const newStreet = 'Медынская улица';
const newAddress = `${address.city}, ${newStreet}, д. ${address.house}`;

export default makeSuite('Редактирование не активного пресета.', {
    feature: 'Редактирование не активного пресета.',
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
            await this.browser.setState(`persAddress.address.${address.id}`, address);
            await this.browser.setState(`persAddress.address.${lastAddress.id}`, lastAddress);

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
                    }
                );

                await this.allure.runStep(
                    'В блоке доставки отображается адрес доставки последнего заказа.', async () => {
                        await this.addressCard.getText()
                            .should.eventually.to.be.include(
                                lastAddress.address,
                                `На карточке адреса доставки должен отображаться адрес "${lastAddress.address}".`
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Кликнуть на адрес доставки в блоке заказа.',
                    async () => {
                        await this.deliveryInfo.click();
                        await this.browser.allure.runStep(
                            'Откроется попап "Мои способы доставки".',
                            async () => {
                                await this.editPopup.waitForVisibleRoot();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Выбран способ доставки "Курьер".',
                            async () => {
                                await this.popupDeliveryTypes.isCheckedDeliveryTypeDelivery()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Должен быть выбран способ доставки "Курьер".'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `Выбран пресет с адресом "${lastAddress.address}".`,
                            async () => {
                                await this.addressList.isItemWithAddressChecked(lastAddress.address)
                                    .then(({value}) => value)
                                    .should.eventually.to.be.equal(
                                        'true',
                                        `Должен быть выбран пресет с адресом "${lastAddress.address}".`
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `Присутствует пресет с адресом "${address.fullDeliveryInfo}".`,
                            async () => {
                                await this.addressList.getCardIndexByAddress(address.fullDeliveryInfo)
                                    .should.eventually.not.to.be.equal(
                                        -1,
                                        `Должен присутствовать пресет с адресом "${address.fullDeliveryInfo}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на Карандаш напротив не активного пресета.',
                    async () => {
                        await this.addressList.clickEditButtonByAddress(address.fullDeliveryInfo);

                        await this.browser.allure.runStep(
                            'Ожидаем появления формы редактирования адреса.',
                            async () => {
                                await this.fullAddressForm.waitForVisibleRoot();
                            }
                        );
                    }
                );
            },
            'Поля формы адреса заполнены в соответствие с пресетом.': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        `В поле "Населенный пункт" - ${address.city}.`,
                        async () => {
                            await this.citySuggest.getText()
                                .should.eventually.to.be.equal(
                                    address.city,
                                    `Текст в поле "Населенный пункт" должен быть "${address.city}".`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        `В поле "Улица" - ${address.street}.`,
                        async () => {
                            await this.streetSuggest.getText()
                                .should.eventually.to.be.equal(
                                    address.street,
                                    `Текст в поле "Улица" должен быть "${address.street}".`
                                );
                        }
                    );

                    await this.browser.allure.runStep(
                        `В поле "Дом" - ${address.house}.`,
                        async () => {
                            await this.fullAddressForm.getHouseText()
                                .should.eventually.to.be.equal(
                                    address.house,
                                    `Текст в поле "Дом" должен быть "${address.house}".`
                                );
                        }
                    );
                },
            }),
            'Ввести в поле "Улица" новое значение.': {
                async beforeEach() {
                    await this.browser.allure.runStep(
                        `Ввести в поле "Улица" - "${newStreet}".`,
                        async () => {
                            await this.streetSuggest.setTextAndSelect(newStreet);
                        }
                    );
                },
                'В форме адреса поле "Улица" подставилось новое значение, остальные поля без изменений.': makeCase({
                    async test() {
                        await this.browser.allure.runStep(
                            `В поле "Населенный пункт" - ${address.city}.`,
                            async () => {
                                await this.citySuggest.getText()
                                    .should.eventually.to.be.equal(
                                        address.city,
                                        `Текст в поле "Населенный пункт" должен быть "${address.city}".`
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `В поле "Улица" - ${newStreet}.`,
                            async () => {
                                await this.streetSuggest.getText()
                                    .should.eventually.to.be.equal(
                                        newStreet,
                                        `Текст в поле "Улица" должен быть "${newStreet}".`
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `В поле "Дом" - ${address.house}.`,
                            async () => {
                                await this.fullAddressForm.getHouseText()
                                    .should.eventually.to.be.equal(
                                        address.house,
                                        `Текст в поле "Дом" должен быть "${address.house}".`
                                    );
                            }
                        );
                    },
                }),
                'Нажать кнопку "Сохранить".': makeCase({
                    async test() {
                        await this.editPopup.chooseButtonClick();

                        await this.allure.runStep(
                            'Ожидаем появления попапа "Мои способы доставки".', async () => {
                                await this.popupDeliveryTypes.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            `Выбран пресет с адресом "${newAddress}".`,
                            async () => {
                                await this.addressList.isItemWithAddressChecked(newAddress)
                                    .then(({value}) => value)
                                    .should.eventually.to.be.equal(
                                        'true',
                                        `Должен быть выбран пресет с адресом "${newAddress}".`
                                    );
                            }
                        );

                        await this.editPopup.waitForBottomButtonEnabled();

                        await this.allure.runStep(
                            'Нажать кнопку "Выбрать".', async () =>
                                this.editPopup.chooseButtonClick()
                        );

                        await this.allure.runStep(
                            'Возврат к странице чекаута', async () => {
                                await this.confirmationPage.waitForVisible();
                                await this.summaryPlaceholder.waitForHidden(5000);
                            }
                        );

                        await this.allure.runStep(
                            'В поле "Адрес доставки" указан отредактированный адрес доставки.', async () => {
                                await this.addressCard.getText()
                                    .should.eventually.to.be.include(
                                        newAddress,
                                        'На карточке адреса доставки должен отображаться отредактированный адрес.'
                                    );
                            }
                        );
                    },
                }),
            },
        },
    },
});
