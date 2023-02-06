import {makeCase, makeSuite} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';

import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import {errorPayment, successPayment} from '@self/root/src/spec/hermione/scenarios/orderPayment';

export default makeSuite('Онлайн оплата.', {
    feature: 'Способы оплаты',
    environment: 'testing',
    params: {
        ...commonParams.description,
        isMultiOrder: 'Создаем мультизаказ',
    },
    defaultParams: {
        ...commonParams.value,
        isMultiOrder: false,
    },
    story: {
        'Валидная карта.': {
            'При оплате заказа': {
                'оплата проходит успешно и пользователь видит экран "Спасибо! Заказ оформлен"': makeCase({
                    id: 'bluemarket-155',
                    issue: 'MARKETVERSTKA-24197',
                    async test() {
                        await this.browser.yaScenario(this, successPayment);
                    },
                }),
            },
        },

        'Пустая карта.': {
            'При оплате заказа': {
                'отображается ошибка "Не удалось оплатить заказ"': makeCase({
                    id: 'bluemarket-745',
                    issue: 'MARKETVERSTKA-24200',
                    async test() {
                        await this.browser.yaScenario(this, errorPayment);
                    },
                }),
            },

            'При повторной оплате заказа после ошибки': {
                'оплата проходит успешно и пользователь видит экран "Спасибо! Заказ оформлен"': makeCase({
                    id: 'bluemarket-839',
                    issue: 'MARKETVERSTKA-24201',
                    async test() {
                        await this.browser.yaScenario(this, errorPayment);
                        await this.orderPayment.actions.tryAgainButton.click();

                        await this.browser.yaScenario(this, successPayment);
                    },
                }),
            },

            'При переходе на выбор другого способа оплаты.': {
                'Одиночный заказ. Редирект к доступным способам оплаты.': makeCase({
                    id: 'marketfront-4413',
                    issue: 'MARKETFRONT-35247',
                    async test() {
                        await this.browser.yaScenario(this, errorPayment);
                        await this.orderPayment.actions.methodChangeButton.click();

                        await this.paymentOptions.waitForPaymentOptionsVisible();
                        await this.paymentOptions.setPaymentTypeYandex();
                        await this.paymentMethodChange.clickSubmitButton();

                        await this.browser.yaScenario(this, successPayment);
                    },
                }),
                'Мультизаказ. Редирект на мои заказаы.': makeCase({
                    id: 'marketfront-4414',
                    issue: 'MARKETFRONT-35247',
                    defaultParams: {
                        isMultiOrder: true,
                    },
                    async test() {
                        await this.browser.yaScenario(this, errorPayment);
                        await this.browser.yaWaitForChangeUrl(() =>
                            this.orderPayment.actions.methodChangeButton.click()
                        );

                        const currentUrl = await this.browser.yaParseUrl();
                        const targetUrl = await this.browser.yaBuildURL(PAGE_IDS_COMMON.ORDERS);

                        await this.expect(currentUrl).to.be.link({
                            pathname: targetUrl,
                        }, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
        },
    },
});
