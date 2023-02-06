import assert from 'assert';
import {makeSuite, makeCase, mergeSuites} from 'ginny';
import url from 'url';
import querystring from 'query-string';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {ORDER_STATUS} from '@self/root/src/entities/order';
import {PAYMENT_METHODS} from '@self/root/src/entities/payment/constants';
import {clearAll as clearAllCookies} from '@self/root/src/spec/hermione/scenarios/cookies';

import OrderConfirmationSubpageHeader from '@self/root/src/widgets/content/OrderConfirmationSubpageHeader/__pageObject';
import CheckoutSubpageHeader from '@self/root/src/components/Checkout/CheckoutSubpageHeader/__pageObject';
import {Preloader} from '@self/root/src/components/Preloader/__pageObject';
import {prepareUnpaidOrderState} from '@self/root/src/spec/hermione/scenarios/unpaidOrder';
import InnerPayment from '@self/root/src/components/InnerPayment/__pageObject';
import {generateRandomId} from '@self/root/src/spec/utils/randomData';


/**
 * Тесты на проверку контролов в шапке при оплате неоплаченного заказа
 * @param {PageObject.OrderPaymentButton} orderPaymentButton
 * @param {PageObject.PaymentMethodChange} paymentMethodChange
 * @param pageId
 *
 */
module.exports = makeSuite('Контролы в шапке.', {
    environment: 'kadavr',
    feature: 'Дооплата',
    issue: 'BLUEMARKET-10245',
    params: {
        pageId: 'Идентификатор страницы',
    },
    defaultParams: {
        isAuthWithPlugin: true,
    },
    story: {
        async beforeEach() {
            const {pageId} = this.params;

            assert(pageId, 'Param pageId must be defined');
            assert(this.orderPaymentButton, 'PageObject.orderPaymentButton must be defined');
            assert(this.paymentMethodChange, 'PageObject.paymentMethodChange must be defined');
            assert(this.orderPayment, 'PageObject.orderPayment must be defined');

            this.setPageObjects({
                orderConfirmationSubpageHeader: () => this.createPageObject(OrderConfirmationSubpageHeader),
                checkoutSubpageHeader: () => this.createPageObject(CheckoutSubpageHeader),
                preloader: () => this.createPageObject(Preloader),
                innerPayment: () => this.createPageObject(InnerPayment),
            });

            const orderId = generateRandomId();
            const entryPointPath = await getEntryPointPagePath.call(this, {orderId});
            const paymentMethodChangePagePath = await this.browser.yaBuildURL(
                PAGE_IDS_COMMON.ORDER_PAYMENT_METHOD_CHANGE,
                {orderId}
            );

            await this.preloader.waitForVisible();
            await this.preloader.waitForHidden(5000);

            this.yaTestData = {
                orderId,
                entryPointPath,
                paymentMethodChangePagePath,
            };
        },

        async afterEach() {
            await this.browser.yaScenario(this, clearAllCookies);
        },

        'При отсутствии ошибок,': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(
                        this,
                        prepareUnpaidOrderState,
                        {
                            orderId: this.yaTestData.orderId,
                            paymentMethod: PAYMENT_METHODS.YANDEX,
                        }
                    );

                    await openPaymentMethodChangePageFromEntryPoint.call(this);
                },
            },

            {
                'на странице изменения способов оплаты': {
                    'кнопка "крестик" в шапке работает корректно': makeCase({
                        id: 'bluemarket-3350',
                        async test() {
                            const currentUrl = await clickCloseButton.call(this);

                            return checkEntryPointPageUrl.call(this, {currentUrl});
                        },
                    }),
                },
            }
        ),

        'При возникновении ошибки на странице оплаты,': mergeSuites(
            {
                async beforeEach() {
                    await this.browser.yaScenario(
                        this,
                        prepareUnpaidOrderState,
                        {
                            orderId: this.yaTestData.orderId,
                            paymentMethod: PAYMENT_METHODS.YANDEX,
                        }
                    );

                    await openPaymentMethodChangePageFromEntryPoint.call(this);

                    const {orderId} = this.yaTestData;

                    await this.allure.runStep(
                        'Имитируем отмену заказа, чтобы получить ошибку на странице оплаты',
                        () => (
                            this.browser.setState(`Checkouter.collections.order.${orderId}`, {
                                id: orderId,
                                status: ORDER_STATUS.CANCELLED,
                            })
                        )
                    );

                    await this.preloader.waitForHidden(5000);

                    await submitChosenPaymentMethod.call(this);

                    await this.allure.runStep(
                        'Ожидаем переход на страницу "Спасибо',
                        () => this.orderConfirmation.waitForCheckoutThankyouIsVisible(5000)
                    );

                    await this.allure.runStep(
                        'Проверяем, что отображается страница "Cпасибо" c неоплаченным заказом',
                        async () => {
                            await this.orderConfirmation.isOrderPaymentStatusVisible()
                                .should.eventually.be.equal(
                                    true,
                                    'Статус заказа в блоке состава заказа должен быть виден'
                                );

                            const paymentText = 'Не оплачено';

                            await this.orderConfirmation.getOrderPaymentStatusText()
                                .should.eventually.be.equal(
                                    paymentText,
                                    `Текст статуса заказа в блоке состава заказа должен быть "${paymentText}"`
                                );
                        }
                    );
                },
            },
            {
                'кнопка "крестик" в шапке работает': makeCase({
                    id: 'bluemarket-3351',
                    async test() {
                        await clickConfirmationCloseButton.call(this);
                    },
                }),
            }
        ),
    },
});

async function openPaymentMethodChangePageFromEntryPoint() {
    await openEntryPointPage.call(this);

    const paymentMethodChangePageActualPath = await this.allure.runStep(
        'Кликаем на кнопку оплаты заказа',
        async () => {
            await this.orderPaymentButton
                .isVisible()
                .should.eventually.be.equal(
                    true,
                    'Кнопка оплаты заказа должна быть видна'
                );

            return this.browser.yaWaitForChangeUrl(() => (
                this.orderPaymentButton.click()
            ));
        }
    );

    const {
        paymentMethodChangePagePath,
        entryPointPath,
    } = this.yaTestData;

    await this.allure.runStep(
        'Проверяем переход на страницу изменения способа оплаты c корректными параметрами url',
        () => (
            this.expect(paymentMethodChangePageActualPath).to.be.link({
                pathname: paymentMethodChangePagePath,
                query: {
                    entryPoint: entryPointPath,
                },
            }, {
                mode: 'equal',
                skipProtocol: true,
                skipHostname: true,
            }, 'Переход на страницу изменения способа оплаты')
        )
    );
}

function openEntryPointPage() {
    const {pageId} = this.params;
    const {orderId} = this.yaTestData;

    return this.allure.runStep(
        `Переходим на страницу ${pageId}`,
        () => {
            const pageParams = getEntryPointPageParams({pageId, orderId});

            return this.browser.yaOpenPage(...pageParams);
        }
    );
}

function getEntryPointPageParams({pageId, orderId}) {
    switch (pageId) {
        case PAGE_IDS_COMMON.ORDER:
            return [pageId, {orderId}];
        case PAGE_IDS_COMMON.INDEX:
            return [pageId, {mock: '1'}];
        default:
            return [pageId];
    }
}

function getEntryPointPagePath({orderId}) {
    const {pageId} = this.params;
    const pageParams = getEntryPointPageParams({pageId, orderId});

    return this.browser.yaBuildURL(...pageParams);
}

async function clickCloseButton() {
    return this.allure.runStep(
        'Кликаем по кнопке "крестик"',
        async () => {
            await this.checkoutSubpageHeader
                .isCloseButtonVisible()
                .should.eventually.be.equal(
                    true,
                    'Кнопка "крестик" в шапке должна быть видна'
                );

            return this.browser.yaWaitForChangeUrl(() => (
                this.checkoutSubpageHeader.clickCloseButton()
            ));
        }
    );
}
async function clickConfirmationCloseButton() {
    return this.allure.runStep(
        'Кликаем по кнопке "крестик"',
        async () => {
            await this.orderConfirmationSubpageHeader
                .isCloseLinkVisible()
                .should.eventually.be.equal(
                    true,
                    'Кнопка "крестик" в шапке должна быть видна'
                );

            return this.browser.yaWaitForChangeUrl(() => (
                this.orderConfirmationSubpageHeader.clickCloseLink()
            ));
        }
    );
}

async function checkEntryPointPageUrl({currentUrl}) {
    const {entryPointPath} = this.yaTestData;
    const {pathname, query: entryPointQueryString} = url.parse(entryPointPath);
    const query = querystring.parse(entryPointQueryString);

    return this.allure.runStep(
        'Проверяем переход на точку входа с корректными параметрами url',
        () => (
            this.expect(currentUrl).to.be.link({
                pathname,
                query,
            }, {
                mode: 'equal',
                skipProtocol: true,
                skipHostname: true,
            }, 'Переход на точку входа')
        )
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
