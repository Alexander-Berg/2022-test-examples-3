import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import {ACTUALIZATION_TIMEOUT} from '@self/root/src/spec/hermione/scenarios/checkout';

import expressOfferMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/express';
import expressSkuMock from '@self/root/src/spec/hermione/kadavr-mock/report/sku/express';
import {deliveryExpressMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import {fillAddressForm, fillDeliveryType} from '@self/platform/spec/hermione/scenarios/checkout';

import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';
import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import GroupedParcels from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import DeliveryTypeOptions from '@self/root/src/components/DeliveryTypeOptions/__pageObject/index.touch.js';
import FullAddressForm from '@self/root/src/components/FullAddressForm/__pageObject';
import CheckoutGlobalNotification from '@self/root/src/widgets/content/checkout/common/CheckoutGlobalNotification/__pageObject';
import cardValid from '@self/root/src/spec/hermione/configs/checkout/cards/valid';

import {ADDRESSES, CONTACTS} from '../constants';

export default makeSuite('Оформление заказа с экспресс-доставкой', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    street: () => this.createPageObject(GeoSuggest, {
                        parent: this.addressForm,
                    }),
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                        parent: this.confirmationPage,
                    }),
                    groupedParcels: () => this.createPageObject(GroupedParcels),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton),
                    deliveryTypeOptions: () => this.createPageObject(DeliveryTypeOptions),
                    fullAddressForm: () => this.createPageObject(FullAddressForm),
                    citySuggest: () => this.createPageObject(GeoSuggest, {
                        parent: this.fullAddressForm,
                    }),
                    streetSuggest: () => this.createPageObject(GeoSuggest, {
                        parent: FullAddressForm.street,
                    }),
                    checkoutGlobalNotification: () => this.createPageObject(CheckoutGlobalNotification),
                });

                const carts = [
                    buildCheckouterBucket({
                        items: [{
                            skuMock: expressSkuMock,
                            offerMock: expressOfferMock,
                            count: 1,
                        }],
                        deliveryOptions: [deliveryExpressMock],
                    }),
                ];

                const testState = await this.browser.yaScenario(
                    this,
                    prepareMultiCartState,
                    carts
                );

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );

                await this.allure.runStep(
                    'Выбираем тип доставки "Курьером"', async () => {
                        await this.browser.yaScenario(
                            this,
                            fillDeliveryType,
                            {type: 'DELIVERY'}
                        );

                        await this.deliveryEditor.waitForSubmitButtonEnabled();
                        await this.deliveryEditor.submitButtonClick();
                    }
                );

                await this.allure.runStep('Заполняем форму адреса доставки', async () => {
                    await this.browser.yaScenario(
                        this,
                        fillAddressForm,
                        ADDRESSES.MOSCOW_HSCH_ADDRESS
                    );

                    await this.deliveryEditor.waitForSubmitButtonEnabled();
                    await this.deliveryEditor.submitButtonClick();
                });

                await this.allure.runStep('Заполняем форму получателя', async () => {
                    await this.recipientForm.setRecipientData(CONTACTS.HSCH_CONTACT, 0);
                    await this.deliveryEditor.submitButtonClick();
                });
            },
            'Заказ с экспресс-доставкой должен оформляться': makeCase({
                id: 'marketfront-5072',
                issue: 'MARKETFRONT-55435',
                async test() {
                    await this.allure.runStep('Проверяем заголовок страницы подтверждения', async () => {
                        await this.groupedParcels.getAddressTitleByCardIndex(0).should.eventually.to.be.equal('Экспресс-доставка, 99 ₽', 'текст верный');
                    });

                    await this.allure.runStep('Проверяем способ оплаты', async () => {
                        await this.paymentOptionsBlock.getText().should.eventually.include(
                            // последние цифры тестовой карты
                            cardValid.cardNumber[cardValid.cardNumber.length - 1],
                            'Способ оплаты - по умолчанию "Картой онлайн"'
                        );
                    });

                    await this.allure.runStep('Переходим к оплате', async () => {
                        await this.checkoutOrderButton.waitForEnabledButton();
                        await this.checkoutOrderButton.isButtonDisabled().should.eventually.to.be.equal(false, 'кнопка доступна');

                        await this.browser.yaWaitForChangeUrl(
                            async () => {
                                await this.checkoutOrderButton.click();
                            },
                            ACTUALIZATION_TIMEOUT
                        );

                        await this.browser.getUrl()
                            .should.eventually.to.be.link({
                                query: {
                                    orderId: /\d+/,
                                },
                                pathname: '/my/orders/payment',
                            }, {
                                mode: 'match',
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    });
                },
            }),
            'Заказ не оформляется после протухания интервалов доставки': makeCase({
                id: 'marketfront-5089',
                issue: 'MARKETFRONT-55435',
                async test() {
                    // пересоздаем состояние чекаутера
                    const cart = buildCheckouterBucket({
                        items: [{
                            skuMock: expressSkuMock,
                            offerMock: expressOfferMock,
                            count: 1,
                        }],
                        deliveryOptions: [deliveryExpressMock],
                        // эмулируем протухшие интервалы доставки
                        changes: ['DELIVERY'],
                        changesReasons: {
                            DELIVERY: [{
                                code: 'DELIVERY_OPTION_MISMATCH',
                                description: 'Опции доставки были возвращены магазином, но не совпадают с корзинными, например по цене.',
                            }],
                        },
                    });

                    const testState = await this.browser.yaScenario(
                        this,
                        prepareMultiCartState,
                        [cart]
                    );

                    await this.browser.setState('Checkouter.collections', testState.checkouterState);
                    // если протухнет интервал доставки, то успешного чекаута не произойдет и вывзовется запрос актуализации
                    await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: false});

                    await this.checkoutOrderButton.click();

                    await this.checkoutGlobalNotification.waitForVisible();
                    await this.checkoutGlobalNotification.getText().should.eventually.to.be.equal('Выбранный вами способ доставки недоступен', 'текст ошибки соответствует ожидаемомму');
                },
            }),
        }
    ),
});
