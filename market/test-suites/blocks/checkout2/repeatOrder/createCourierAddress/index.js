import {
    makeSuite,
    makeCase,
} from 'ginny';

// mocks
import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import {region} from '@self/root/src/spec/hermione/configs/geo';

// scenarious
import {prepareCheckouterPageWithCartsForRepeatOrder} from '@self/root/src/spec/hermione/scenarios/checkout';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {fillAddressForm} from '@self/platform/spec/hermione/scenarios/checkout';

// pageObjects
import DeliveryInfo from '@self/root/src/components/Checkout/DeliveryInfo/__pageObject/index.touch';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import AddressCard from '@self/root/src/components/AddressCard/__pageObject';

import {ADDRESSES} from '../constants';

export default makeSuite('Presets 2.0. Повторная покупка. Пользователь без пресетов.', {
    id: 'm-touch-3762',
    issue: 'MARKETFRONT-48978',
    feature: 'Presets 2.0. Повторная покупка. Пользователь без пресетов.',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryInfo: () => this.createPageObject(DeliveryInfo, {
                    parent: this.confirmationPage,
                }),
                popupDeliveryTypes: () => this.createPageObject(DeliveryTypeList),
                fullAddressForm: () => this.createPageObject(FullAddressForm),
                citySuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.fullAddressForm,
                }),
                streetSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: FullAddressForm.street,
                }),
                addressCard: () => this.createPageObject(AddressCard, {
                    parent: this.addressBlock,
                }),
            });
            const carts = [
                buildCheckouterBucket({
                    cartIndex: 0,
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

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
        'Создание адреса доставки курьером.': makeCase({
            async test() {
                await this.allure.runStep(
                    'Открыть страницу чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Выбрать пункт выдачи" в блоке заказа.',
                    async () => {
                        await this.deliveryInfo.click();
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
                    'Нажать кнопку "Добавить новый".',
                    async () => {
                        await this.browser.allure.runStep(
                            'Над чипсами присутствует кнопка "Добавить новый".',
                            async () => {
                                await this.editPopup.isAddNewButtonVisible()
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Над чипсами должна быть кнопка "Добавить новый".'
                                    );
                            }
                        );

                        await this.browser.allure.runStep(
                            'Нажать кнопку "Добавить новый".',
                            async () => {
                                await this.editPopup.clickAddNewButton();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'В форме заполнить поля нового адреса.', async () => {
                        await this.allure.runStep(
                            'Заполняем форму нового адреса.', () =>
                                this.browser.yaScenario(
                                    this,
                                    fillAddressForm,
                                    ADDRESSES.MINIMAL_ADDRESS
                                )
                        );
                        await this.allure.runStep(
                            'Нажать кнопку "Сохранить".', async () => {
                                await this.editPopup.waitForChooseButtonEnabled();
                                await this.editPopup.chooseButtonClick();
                            }
                        );
                    }
                );

                await this.allure.runStep(
                    'Открыть страницу чекаута', async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.preloader.waitForHidden(5000);
                    }
                );

                await this.allure.runStep(
                    'В поле "Адрес доставки" указан полный адрес доставки, который был указан в форме "Новый адрес".', async () => {
                        await this.addressEditableCard.getTitle()
                            .should.eventually.include(
                                'Доставка курьером',
                                'Текст заголовка должен содержать "Доставка курьером".'
                            );

                        await this.addressCard.getText()
                            .should.eventually.to.be.include(
                                ADDRESSES.MINIMAL_ADDRESS.fullDeliveryInfo,
                                'На карточке адреса доставки должны отображаться указанные пользователем данные адреса'
                            );
                    }
                );
            },
        }),
    },
});
