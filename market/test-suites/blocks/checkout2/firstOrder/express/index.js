import {
    makeSuite,
    mergeSuites,
    makeCase,
} from 'ginny';

import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';
import {prepareCheckoutPage} from '@self/root/src/spec/hermione/scenarios/checkout';
import {prepareMultiCartState} from '@self/root/src/spec/hermione/scenarios/cartResource';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';
import {goToConfirmationPage} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPage';

import expressOfferMock from '@self/root/src/spec/hermione/kadavr-mock/report/offer/express';
import expressSkuMock from '@self/root/src/spec/hermione/kadavr-mock/report/sku/express';
import {deliveryExpressMock} from '@self/root/src/spec/hermione/kadavr-mock/checkouter/delivery';
import cardValid from '@self/root/src/spec/hermione/configs/checkout/cards/valid';

import EditPaymentOption from '@self/root/src/components/EditPaymentOption/__pageObject';
import GroupedParcels from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';
import CheckoutGlobalNotification from '@self/root/src/widgets/content/checkout/common/CheckoutGlobalNotification/__pageObject';

const EXPEXT_TIMEOUT = 3000;
const EXPEXT_INTERVAL = 500;

export default makeSuite('Оформление заказа с экспресс-доставкой', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                this.setPageObjects({
                    paymentOptionsBlock: () => this.createPageObject(EditPaymentOption, {
                        parent: this.confirmationPage,
                    }),
                    groupedParcels: () => this.createPageObject(GroupedParcels),
                    checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton),
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

                this.yaTestData = this.yaTestData || {};
                this.yaTestData.testState = testState;

                await this.browser.yaScenario(
                    this,
                    prepareCheckoutPage,
                    {
                        items: testState.checkoutItems,
                        reportSkus: testState.reportSkus,
                        checkout2: true,
                    }
                );

                await this.browser.yaScenario(this, goToConfirmationPage, {userFormData});
            },
            'Заказ с экспресс-доставкой должен оформляться': makeCase({
                id: 'marketfront-5072',
                issue: 'MARKETFRONT-55435',
                async test() {
                    await this.allure.runStep('Проверяем заголовок страницы подтверждения', async () => {
                        await this.groupedParcels.getAddressTitleByCardIndex(0).should.eventually.to.be.equal('Экспресс-доставка, 99 ₽\nИзменить', 'текст верный');
                    });

                    await this.allure.runStep('Проверяем способ оплаты', async () => {
                        // Ожидаем когда загрузятся данные карты
                        await this.browser.waitUntil(
                            () => this.paymentOptionsBlock.getText().should.eventually.include(
                                // последние цифры тестовой карты
                                cardValid.cardNumber[cardValid.cardNumber.length - 1],
                                'Способ оплаты - по умолчанию "Картой онлайн"'
                            ),
                            EXPEXT_TIMEOUT,
                            'Способ оплаты "Картой онлайн", не отобразился',
                            EXPEXT_INTERVAL
                        );
                    });

                    await this.allure.runStep('Переходим к оплате', async () => {
                        await this.checkoutOrderButton.waitForEnabledButton();
                        await this.checkoutOrderButton.isButtonDisabled().should.eventually.to.be.equal(false, 'кнопка доступна');

                        await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});

                        await this.browser.yaWaitForChangeUrl(async () => {
                            await this.checkoutOrderButton.click();
                        });

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
