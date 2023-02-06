import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES} from '../../constants';

const firstAddress = ADDRESSES.MOSCOW_ADDRESS;
const secondAddress = ADDRESSES.MOSCOW_LAST_ADDRESS;

export default makeSuite('У пользователя сохранено несколько адресов доставки.', {
    feature: 'У пользователя сохранено несколько адресов доставки.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
        carts: 'Корзины',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.setState(`persAddress.address.${firstAddress.id}`, firstAddress);
            await this.browser.setState(`persAddress.address.${secondAddress.id}`, secondAddress);

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
                        await this.browser.allure.runStep(
                            `Активным отображается адрес "${secondAddress.address}".`,
                            async () => {
                                await this.addressList.getActiveItemText()
                                    .should.eventually.to.be.equal(
                                        secondAddress.address,
                                        `Текст активного пресета должен быть "${secondAddress.address}".`
                                    );
                            }
                        );
                        await this.addressList.clickOnEditButtonByAddress(secondAddress.address);
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем появления формы "Изменить адрес".',
                    async () => {
                        await this.editAddressPopup.waitForEditFragmentVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить".',
                    async () => {
                        await this.editAddressPopup.clickDeleteButton();

                        await this.browser.allure.runStep(
                            'Появляется попап "И правда хотите удалить этот адрес?".',
                            async () => {
                                await this.editAddressPopup.waitForDeleteFragmentVisible();
                                await this.editAddressPopup.getDeleteFragmentTitleText()
                                    .should.eventually.to.be.equal(
                                        'И правда хотите удалить этот адрес?',
                                        'Заголовок попапа должен быть "И правда хотите удалить этот адрес?".'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `В попапе отображается адрес "${secondAddress.address}".`,
                            async () => {
                                await this.editableAddressCard.getText()
                                    .should.eventually.to.be.equal(
                                        secondAddress.address,
                                        `В попапе должен отображается адрес "${secondAddress.address}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить" в попапе подтверждения удаления.',
                    async () => {
                        await this.editAddressPopup.clickDeleteAddressButton();
                        await this.editAddressPopup.waitForDeleteFragmentInvisible();

                        await this.browser.allure.runStep(
                            'Открывается попап "Способ доставки"',
                            async () => {
                                await this.editPopup.waitForVisibleRoot();
                                await this.editPopup.waitForChooseButtonEnabled();

                                await this.browser.allure.runStep(
                                    'В попапе отсутствует отображение пресета с удаленным адресом',
                                    async () => {
                                        await this.addressList.isCardWithAddressExisting(secondAddress.address)
                                            .should.eventually.to.be.equal(
                                                false,
                                                'В попапе не должен отображаться пресет с удаленным адресом.'
                                            );
                                    }
                                );
                            }
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
                                await this.editPopup.waitForRootInvisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'В блоке доставки отображается адрес первого доступного пресета.',
                            async () => {
                                await this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        firstAddress.address,
                                        'В блоке доставки должна отображается адрес первого доступного пресета.'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Перезагрузить страницу.',
                    async () => {
                        await this.browser.refresh();
                        await this.browser.allure.runStep(
                            'Отображается главный экран чекаута.',
                            async () => {
                                await this.confirmationPage.waitForVisible();
                                await this.deliveryInfo.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'В блоке доставки отображается адрес первого доступного пресета.',
                            async () => {
                                await this.addressCard.getText()
                                    .should.eventually.to.be.equal(
                                        firstAddress.address,
                                        'В блоке доставки должна отображается адрес первого доступного пресета.'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
