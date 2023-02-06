import {makeCase, makeSuite} from 'ginny';

import {
    addPresetForRepeatOrder,
    prepareCheckouterPageWithCartsForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';
import {DELIVERY_TYPES} from '@self/root/src/constants/delivery';

import {ADDRESSES, CONTACTS} from '../constants';

const lastAddress = ADDRESSES.MOSCOW_ADDRESS;
const unavailableMassage = 'Недоступен для этих товаров';
const unavailableAddress = ADDRESSES.VOLGOGRAD_ADDRESS;
const unavailableCardText = `${unavailableAddress.address}\n${unavailableMassage}`;
const newComment = 'Тестирование редактирования адреса';

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
                    address: lastAddress,
                    contact: CONTACTS.DEFAULT_CONTACT,
                }
            );

            await this.browser.yaScenario(
                this,
                addPresetForRepeatOrder,
                {
                    address: unavailableAddress,
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
                            `Присутствует пресет с адресом "${unavailableAddress.address}" и сообщением "${unavailableMassage}".`,
                            async () => {
                                await this.addressList.getCardIndexByAddress(unavailableCardText)
                                    .should.eventually.not.to.be.equal(
                                        -1,
                                        `Должен присутствовать пресет с текстом "${unavailableCardText}".`
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на Карандаш напротив не активного пресета.',
                    async () => {
                        await this.addressList.clickEditButtonByAddress(unavailableCardText);

                        await this.browser.allure.runStep(
                            'Ожидаем появления формы редактирования адреса.',
                            async () => {
                                await this.fullAddressForm.waitForVisibleRoot();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    `Ввести в поле "Комментарий" - "${newComment}".`,
                    async () => {
                        await this.fullAddressForm.setCommentFieldValue(newComment);
                    }
                );

                await this.editPopup.bottomButtonClick();

                await this.allure.runStep(
                    'Ожидаем появления попапа "Мои способы доставки".', async () => {
                        await this.popupDeliveryTypes.waitForVisible();
                        await this.editPopup.waitForChooseButtonEnabled();
                    }
                );

                await this.allure.runStep(
                    'Ожидаем появления попапа списка пресетов.', async () => {
                        await this.addressList.waitForVisibleRoot();
                    }
                );

                await this.browser.allure.runStep(
                    `Отредактированный пресет отображается неактивным, отображается сообщение "${unavailableMassage}".`,
                    async () => {
                        await this.addressList.getCardIndexByAddress(unavailableCardText)
                            .should.eventually.not.to.be.equal(
                                -1,
                                `Пресет с текстом "${unavailableCardText}" должен быть не активен.`
                            );
                    }
                );
            },
        }),
    },
});
