import {makeCase, makeSuite} from 'ginny';

import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import {ADDRESSES} from '../../constants';

const address = ADDRESSES.MOSCOW_ADDRESS;

export default makeSuite('У пользователя сохранен один адрес доставки.', {
    feature: 'У пользователя сохранен один адрес доставки.',
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
            await this.browser.setState(`persAddress.address.${address.id}`, address);
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
                        await this.addressList.clickOnEditButtonByAddress(address.address);
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
                        const title = 'И правда хотите удалить этот адрес?';
                        await this.editAddressPopup.clickDeleteButton();

                        await this.browser.allure.runStep(
                            `Появляется попап "${title}".`,
                            async () => {
                                await this.editAddressPopup.waitForDeleteFragmentVisible();
                                await this.editAddressPopup.getDeleteFragmentTitleText()
                                    .should.eventually.to.be.equal(
                                        title,
                                        `Заголовок попапа должен быть "${title}".`
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            `В попапе отображается адрес "${address.address}".`,
                            async () => {
                                await this.editableAddressCard.getText()
                                    .should.eventually.to.be.equal(
                                        address.address,
                                        `В попапе должен отображается адрес "${address.address}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Удалить" в попапе подтверждения удаления.',
                    async () => {
                        const emptyTitleText = 'Нет сохраненных адресов';

                        await this.editAddressPopup.clickDeleteAddressButton();
                        await this.editAddressPopup.waitForDeleteFragmentInvisible();

                        await this.browser.allure.runStep(
                            'Открывается попап "Способ доставки"',
                            async () => {
                                await this.editPopup.waitForVisibleRoot();

                                await this.browser.allure.runStep(
                                    `В попапе отображается заголовок "${emptyTitleText}".`,
                                    async () => {
                                        await this.editPopup.isAddressEmptyTitleVisible()
                                            .should.eventually.to.be.equal(true, 'Должен отображаться пустой заголовок.');

                                        await this.editPopup.getAddressEmptyText()
                                            .should.eventually.to.be.equal(
                                                emptyTitleText,
                                                `Текст заголовка блока доставки должен быть "${emptyTitleText}".`
                                            );
                                    }
                                );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Закрыть попап "Способ доставки".',
                    async () => {
                        await this.popupBase.clickOnCrossButton();
                        await this.editPopup.waitForRootInvisible();

                        await this.browser.allure.runStep(
                            'В блоке доставки отображается кнопка "Выбрать адрес доставки".',
                            async () => {
                                await this.deliveryActionButton.isButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'В блоке доставки должна отображается кнопка "Выбрать адрес доставки".'
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
                                await this.deliveryActionButton.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'В блоке доставки отображается кнопка "Выбрать адрес доставки".',
                            async () => {
                                await this.deliveryActionButton.isButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'В блоке доставки должна отображается кнопка "Выбрать адрес доставки".'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
