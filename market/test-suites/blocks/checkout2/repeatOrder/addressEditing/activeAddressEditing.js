import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addressFieldChecker,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject';
import {TextField} from '@self/root/src/uikit/components/TextField/__pageObject';

import {ADDRESSES} from '../../constants';

const newApartment = '321';
const newFloor = '29';
const newEntrance = '1';
const newIntercom = '45677';
const newComment = 'Тестирование редактирования адреса';
const newAddress = [`${ADDRESSES.MOSCOW_ADDRESS.address}, ${newApartment}`] +
    [`${newEntrance} подъезд, ${newFloor} этаж, домофон ${newIntercom}, "${newComment}"`];

export default makeSuite('Редактирование активного пресета.', {
    feature: 'Редактирование активного пресета.',
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
            await this.browser.setState(`persAddress.address.${ADDRESSES.MOSCOW_ADDRESS.id}`, ADDRESSES.MOSCOW_ADDRESS);
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
                        await this.popupDeliveryTypes.waitForVisible();
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
                        const apartmentFieldSelector = AddressForm.apartament;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: apartmentFieldSelector,
                                inputText: newApartment,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Поле "Этаж".',
                    async () => {
                        const floorFieldSelector = AddressForm.floor;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: floorFieldSelector,
                                inputText: newFloor,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Поле "Подъезд".',
                    async () => {
                        const entranceFieldSelector = AddressForm.entrance;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: entranceFieldSelector,
                                inputText: newEntrance,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Поле "Домофон".',
                    async () => {
                        const intercomFieldSelector = AddressForm.intercom;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: intercomFieldSelector,
                                inputText: newIntercom,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Поле "Комментарий для курьера".',
                    async () => {
                        const commentFieldSelector = AddressForm.comment;
                        await this.browser.yaScenario(
                            this,
                            addressFieldChecker,
                            {
                                selector: commentFieldSelector,
                                inputType: TextField.textArea,
                                inputText: newComment,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Редактируемый пресет отображается активным.',
                    async () => {
                        await this.editAddressPopup.clickSaveButton();
                        await this.editAddressPopup.waitForEditFragmentInvisible();
                        await this.editPopup.waitForChooseButtonEnabled();

                        await this.browser.allure.runStep(
                            'В пресете отображается веденная информация.',
                            async () => {
                                await this.addressList.getActiveCardText()
                                    .should.eventually.to.be.equal(
                                        newAddress,
                                        'Текст активного пресета должен соответствовать отредактированному адресу.'
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
                    }
                );

                await this.browser.allure.runStep(
                    'В блоке доставки отображается адрес со всеми внесенными изменениями',
                    async () => {
                        await this.addressCard.getText()
                            .then(text => text.replace('\n', ''))
                            .should.eventually.to.be.equal(
                                newAddress,
                                'Текст в блоке адреса должен соответствовать отредактированному адресу.'
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
                            'В блоке доставки отображается адрес со всеми внесенными изменениями',
                            async () => {
                                await this.addressCard.getText()
                                    .then(text => text.replace('\n', ''))
                                    .should.eventually.to.be.equal(
                                        newAddress,
                                        'Текст в блоке адреса должен соответствовать отредактированному адресу.'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
