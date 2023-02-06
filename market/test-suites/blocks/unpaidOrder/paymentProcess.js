import assert from 'assert';
import {makeSuite, makeCase} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_TYPE} from '@self/root/src/entities/payment';
import {PAYMENT_METHODS} from '@self/root/src/entities/payment/constants';
import {clearAll as clearAllCookies} from '@self/root/src/spec/hermione/scenarios/cookies';
import {
    prepareUnpaidOrderState,
    checkPaymentButtonTextAndUrl,
    openEntryPointPage,
} from '@self/root/src/spec/hermione/scenarios/unpaidOrder';
import {
    checkPaymentPageError,
    checkPaymentPageUrl,
} from '@self/root/src/spec/hermione/scenarios/orderPayment';
import {getEntryPointPagePath} from '@self/root/src/spec/utils/unpaidOrder';

import PaymentOptionsList from '@self/root/src/components/PaymentOptionsList/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';

import {checkSelectedOption} from '@self/root/src/spec/hermione/scenarios/checkout';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';

/**
 * Тесты на оплату уже оформленного, но неоплаченного заказа с предоплатным типом оплаты
 *
 * @param {PageObject.OrderPaymentButton} orderPaymentButton
 * @param {PageObject.PaymentMethodChange} paymentMethodChange
 * @param {PageObject.OrderPayment} orderPayment
 * @param pageId
 * @param paymentButtonText
 */
module.exports = makeSuite('Дооплата.', {
    environment: 'kadavr',
    feature: 'Дооплата',
    params: {
        pageId: 'Идентификатор страницы',
        paymentButtonText: 'Текст кнопки оплаты заказа',
    },
    defaultParams: {
        isAuthWithPlugin: true,
        paymentButtonText: 'Оплатить заказ',
    },
    story: {
        async beforeEach() {
            assert(this.params.pageId, 'Param pageId must be defined');
            assert(this.paymentMethodChange, 'PageObject.paymentMethodChange must be defined');
            assert(this.orderPaymentButton, 'PageObject.orderPaymentButton must be defined');
            assert(this.orderPayment, 'PageObject.orderPayment must be defined');

            this.setPageObjects({
                paymentOptions: () => this.createPageObject(PaymentOptionsList, {
                    parent: this.paymentMethodChange,
                }),
                preloader: () => this.createPageObject(Preloader),
            });

            const orderId = generateRandomId();
            const entryPointPath = await getEntryPointPagePath.call(this, {
                orderId,
                pageId: this.params.pageId,
            });

            this.yaTestData = {
                orderId,
                entryPointPath,
            };
        },

        async afterEach() {
            await this.browser.yaScenario(this, clearAllCookies);
        },

        'Переход с точки входа на страницу изменения способа оплаты.': {
            'При переходе к оплате без изменения способа оплаты,': {
                'происходит переход на траст с корректными параметрами': makeCase({
                    id: 'bluemarket-3319',
                    issue: 'BLUEMARKET-9697',
                    async test() {
                        const {orderId, entryPointPath} = this.yaTestData;

                        await this.browser.yaScenario(
                            this,
                            prepareUnpaidOrderState,
                            {
                                orderId,
                                paymentMethod: PAYMENT_METHODS.YANDEX,
                            }
                        );

                        await this.browser.yaScenario(
                            this,
                            openEntryPointPage,
                            {
                                orderId,
                                pageId: this.params.pageId,
                            }
                        );

                        await this.browser.yaScenario(
                            this,
                            checkPaymentButtonTextAndUrl,
                            {
                                orderId,
                                entryPointPath,
                                paymentButtonText: this.params.paymentButtonText,
                            }
                        );
                    },
                }),
            },
        },

        'Если при оформлении заказа в приложении был выбран способ оплаты Apple Pay,': {
            'на вебе должна быть предвыбрана опция оплаты картой': makeCase({
                id: 'bluemarket-3318',
                issue: 'BLUEMARKET-9697',
                async test() {
                    await this.browser.yaScenario(
                        this,
                        prepareUnpaidOrderState,
                        {
                            orderId: this.yaTestData.orderId,
                            paymentMethod: PAYMENT_METHODS.APPLE_PAY,
                        }
                    );

                    await openPaymentMethodChangePage.call(this);
                    await checkPaymentOptions.call(this);

                    await this.allure.runStep(
                        'Проверяем, что выбран способ оплаты "Картой онлайн"',
                        () => (
                            this.browser.yaScenario(this, checkSelectedOption, {paymentMethod: PAYMENT_METHODS.YANDEX})
                        )
                    );
                },
            }),
        },
        'При переходе на страницу изменения способа оплаты для отмененного заказа,': {
            'происходит редирект на страницу траста с отображением ошибки': makeCase({
                id: 'bluemarket-3347',
                issue: 'BLUEMARKET-10245',
                async test() {
                    const {orderId} = this.yaTestData;

                    await this.browser.setState('Checkouter.collections', {
                        order: {
                            [orderId]: {
                                id: orderId,
                                status: ORDER_STATUS.CANCELLED,
                                paymentType: PAYMENT_TYPE.PREPAID,
                                paymentMethod: PAYMENT_METHODS.YANDEX,
                            },
                        },
                    });

                    await openPaymentMethodChangePage.call(this);

                    return checkPaymentPageUrlAndError.call(this);
                },
            }),
        },
        'Если в доступных способах оплаты только опции для мобильных приложений,': {
            'происходит редирект на страницу траста с отображением ошибки': makeCase({
                id: 'bluemarket-3348',
                issue: 'BLUEMARKET-10245',
                async test() {
                    await this.browser.yaScenario(
                        this,
                        prepareUnpaidOrderState,
                        {
                            orderId: this.yaTestData.orderId,
                            paymentMethod: PAYMENT_METHODS.YANDEX,
                            paymentOptions: [
                                PAYMENT_METHODS.APPLE_PAY,
                                PAYMENT_METHODS.GOOGLE_PAY,
                            ],
                            validFeatures: [],
                        }
                    );

                    await openPaymentMethodChangePage.call(this);

                    return checkPaymentPageUrlAndError.call(this);
                },
            }),
        },
        'При возникновении ошибки при сохранении способа оплаты,': {
            'происходит редирект на страницу траста с отображением ошибки': makeCase({
                id: 'bluemarket-3349',
                issue: 'BLUEMARKET-10245',
                async test() {
                    const {orderId} = this.yaTestData;

                    await this.browser.yaScenario(
                        this,
                        prepareUnpaidOrderState,
                        {
                            orderId,
                            paymentMethod: PAYMENT_METHODS.YANDEX,
                        }
                    );

                    await openPaymentMethodChangePage.call(this);

                    await this.browser.setState('Checkouter.collections.orderEditPossibilities', {
                        [orderId]: [],
                    });

                    await submitChosenPaymentMethod.call(this);

                    await this.allure.runStep(
                        'Ожидаем переход на страницу "Спасибо',
                        () => this.orderConfirmation.waitForCheckoutThankyouIsVisible(5000)
                    );
                },
            }),
        },
    },
});

async function openPaymentMethodChangePage(waitForPreloader = true) {
    const {orderId, entryPointPath} = this.yaTestData;

    await this.allure.runStep(
        `Переходим на страницу изменения способа оплаты c параметром entryPoint=${entryPointPath}`,
        () => (
            this.browser.yaOpenPage(
                PAGE_IDS_COMMON.ORDER_PAYMENT_METHOD_CHANGE,
                {
                    orderId,
                    entryPoint: entryPointPath,
                }
            )
        )
    );

    if (waitForPreloader) {
        await this.preloader.waitForVisible();
        await this.preloader.waitForHidden(5000);
    }
}

async function checkPaymentOptions() {
    return this.allure.runStep(
        'Проверяем доступные способы оплаты',
        async () => {
            await this.paymentMethodChange.waitForVisible();

            await this.paymentOptions.isPaymentTypeYandexVisible()
                .should.eventually.to.be.equal(true,
                    'Способ оплаты "Картой онлайн" должен быть виден'
                );

            await this.paymentOptions.isPaymentTypeSpasiboVisible()
                .should.eventually.to.be.equal(true,
                    'Способ оплаты "Бонусами СберСпасибо" должен быть виден'
                );
        }
    );
}

async function submitChosenPaymentMethod() {
    return this.allure.runStep(
        'Кликаем на кнопку "Перейти к оплате"',
        async () => {
            await this.paymentMethodChange.isSubmitButtonVisible()
                .should.eventually.to.be.equal(true,
                    'Кнопка "Перейти к оплате" должна быть видна'
                );

            return this.paymentMethodChange.clickSubmitButton();
        }
    );
}

async function checkPaymentPageUrlAndError() {
    const currentUrl = await this.browser.getUrl();

    const {
        orderId,
        entryPointPath,
    } = this.yaTestData;

    await this.browser.yaScenario(
        this,
        checkPaymentPageUrl,
        {
            orderId,
            entryPointPath,
            currentUrl,
            allowSpasibo: false,
            hasError: true,
        }
    );

    return this.browser.yaScenario(this, checkPaymentPageError);
}
