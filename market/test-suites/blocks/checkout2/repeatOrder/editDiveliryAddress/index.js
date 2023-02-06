import {makeCase, makeSuite} from 'ginny';

import {
    prepareCheckouterPageWithCartsForRepeatOrder,
    addPresetForRepeatOrder,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

import CheckoutWizard from '@self/root/src/widgets/content/checkout/layout/components/wizard/__pageObject';
import EditPopup
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/__pageObject';
import AddressForm from '@self/root/src/components/AddressForm/__pageObject/index.js';
import AddressList
    from '@self/root/src/widgets/content/checkout/common/CheckoutParcel/components/EditPopup/components/AddressList/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';

import {ADDRESSES, CONTACTS} from '../../constants';

export default makeSuite('Редактирование адреса в форме экрана "Как доставить заказ?" с последующим сохранением внесенных изменений.', {
    id: 'marketfront-4923',
    issue: 'MARKETFRONT-54552',
    feature: 'Редактирование адреса в форме экрана "Как доставить заказ?"',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: false,
    },
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryEditorCheckoutWizard: () => this.createPageObject(CheckoutWizard),
                editPopup: () => this.createPageObject(EditPopup),
                addressForm: () => this.createPageObject(AddressForm),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.deliveryInfo,
                }),
                street: () => this.createPageObject(GeoSuggest, {
                    parent: this.addressForm,
                }),
                addressList: () => this.createPageObject(AddressList, {
                    parent: this.editPopup,
                }),
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
        'Открыть страницу чекаута.': {
            async beforeEach() {
                await this.confirmationPage.waitForVisible();
                await this.deliveryInfo.waitForVisible();
            },
            'В блоке "Доставки"': {
                'Нажать на кнопку "Изменить"': {
                    async beforeEach() {
                        await this.addressEditableCard.changeButtonClick();
                        await this.editPopup.waitForVisibleRoot();
                    },

                    'Открывается попап "Способ доставки"': makeCase({
                        async test() {
                            await this.allure.runStep(
                                'В пресете нажать на кнопку "Карандаш"', async () => {
                                    const address = ADDRESSES.MOSCOW_ADDRESS.address;
                                    await this.addressList.clickOnEditButtonByAddress(address);

                                    await this.allure.runStep(
                                        'Появляется форма редактирования адреса', async () => {
                                            await this.addressForm.waitForVisibleRoot();
                                        }
                                    );
                                }
                            );

                            await this.allure.runStep(
                                'Под полем "Адрес" нажать на кнопку "Выбрать на карте"', async () => {
                                    await this.addressForm.clickOnChooseOnMapLink();
                                    await this.allure.runStep(
                                        'Открывается экран "Как доставить заказ?"', async () => {
                                            const titleText = 'Как доставить заказ?';

                                            await this.deliveryEditorCheckoutWizard.waitForVisible();

                                            await this.deliveryEditorCheckoutWizard.getTitleText()
                                                .should.eventually.to.be.equal(
                                                    titleText,
                                                    `Текст заголовка блока должен быть "${titleText}".`
                                                );
                                        }
                                    );
                                }
                            );

                            await this.allure.runStep(
                                'Установить фокус в поле ввода "Адрес"', async () => {
                                    await this.street.focusField();
                                    await this.street.waitForVisibleClearIcon();

                                    await this.allure.runStep(
                                        'Нажать на кнопку "Х"', async () => {
                                            await this.street.clearField();

                                            await this.allure.runStep(
                                                'Поле ввода очищается', async () => {
                                                    await this.street.getText()
                                                        .should.eventually.to.be.equal(
                                                            '',
                                                            'Поле должно быть пустым'
                                                        );
                                                }
                                            );

                                            await this.allure.runStep(
                                                'В поле отсутствует отображение кнопки "Х"', async () => {
                                                    await this.street.clearIconIsVisible()
                                                        .should.eventually.to.be.equal(
                                                            false,
                                                            'Должно выдать "false"'
                                                        );
                                                }
                                            );

                                            await this.allure.runStep(
                                                'Фокус остается в поле ввода', async () => {
                                                    await this.street.isFocusedGeoField()
                                                        .should.eventually.to.be.equal(
                                                            true,
                                                            'Должно выдать "true"'
                                                        );
                                                }
                                            );
                                        }
                                    );

                                    await this.allure.runStep(
                                        'Ввести в поле ввода "Москва, Кооперативная улица, д. 10"', async () => {
                                            const newAddress = 'Москва, Кооперативная улица, д. 10';
                                            await this.street.setText(newAddress, false);
                                            await this.allure.runStep(
                                                'В поле ввода отображается веденный текст', async () => {
                                                    await this.street.getText()
                                                        .should.eventually.to.be.equal(
                                                            newAddress,
                                                            `Поле должно содержать адрес ${newAddress}`
                                                        );
                                                }
                                            );
                                            await this.allure.runStep(
                                                'В поле отображается кнопка "Х"', async () => {
                                                    await this.street.clearIconIsVisible()
                                                        .should.eventually.to.be.equal(
                                                            true,
                                                            'Должно выдать "true"'
                                                        );
                                                }
                                            );
                                            await this.allure.runStep(
                                                'Под полем ввода отображается саджест', async () => {
                                                    await this.street.waitForSuggestion('Кооперативная улица, 10');
                                                }
                                            );
                                        }
                                    );

                                    await this.allure.runStep(
                                        'Из саджеста выбрать введенный адрес', async () => {
                                            const addressFromSuggest = 'Кооперативная улица, 10';
                                            await this.street.selectSuggestion(addressFromSuggest);
                                            await this.browser.yaDelay(2000);

                                            await this.allure.runStep(
                                                'Фокус из поля ввода пропадает', async () => {
                                                    await this.street.isFocusedGeoField()
                                                        .should.eventually.to.be.equal(
                                                            false,
                                                            'Должно выдать "false"'
                                                        );
                                                }
                                            );

                                            await this.allure.runStep(
                                                'В поле ввода отображается веденный текст', async () => {
                                                    const newAddress = 'Москва, Кооперативная улица, д. 10';
                                                    await this.street.getText()
                                                        .should.eventually.to.be.equal(
                                                            newAddress,
                                                            `В поле ввода отображается адрес, который был выбран в саджесте "${newAddress}"`
                                                        );
                                                }
                                            );
                                        }
                                    );
                                }
                            );

                            await this.allure.runStep(
                                'Нажать на кнопку "Выбрать"', async () => {
                                    await this.deliveryEditorCheckoutWizard.submitButtonClick();
                                    await this.confirmationPage.waitForVisible();
                                    await this.deliveryInfo.waitForVisible();

                                    await this.allure.runStep(
                                        'Отображается главный экран чекаута', async () => {
                                            const titleText = 'Оформление';

                                            await this.confirmationPage.getTitle()
                                                .should.eventually.to.be.equal(
                                                    titleText,
                                                    `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                                                );
                                        }
                                    );

                                    await this.allure.runStep(
                                        'На главном экране отображается адрес "Москва, Кооперативная улица, д. 10"', async () => {
                                            const newAddress = 'Москва, Кооперативная улица, д. 10';
                                            await this.addressCard.getText()
                                                .should.eventually.to.be.equal(
                                                    newAddress,
                                                    `Отображается, который был выбран в саджесте "${newAddress}"`
                                                );
                                        }
                                    );
                                }
                            );

                            await this.allure.runStep(
                                'Нажать на кнопку "Изменить" в блоке с адресом доставки', async () => {
                                    await this.addressEditableCard.changeButtonClick();

                                    await this.allure.runStep(
                                        'Открывается попап "Способ доставки"', async () => {
                                            await this.editPopup.waitForVisibleRoot();
                                        }
                                    );

                                    await this.allure.runStep(
                                        'В попапе отображается активный пресет с адресом "Москва, Кооперативная улица, д. 10"', async () => {
                                            const newAddress = 'Москва, Кооперативная улица, д. 10';

                                            await this.addressList.getActiveItemText()
                                                .should.eventually.to.be.equal(
                                                    newAddress,
                                                    `Отображается адрес "${newAddress}"`
                                                );
                                        }
                                    );
                                }
                            );
                        },
                    }),
                },
            },
        },
    },
});
