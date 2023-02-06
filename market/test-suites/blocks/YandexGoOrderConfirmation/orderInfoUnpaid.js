import {makeSuite, makeCase} from '@yandex-market/ginny';

//* @param {PageObject.YandexGoOrderInfoUnpaid} yandexGoOrderInfoUnpaid

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Информация о неоплаченном заказе', {
    id: 'marketfront-todo-add',
    tags: ['Контур#Интеграции'],
    story: {
        'Кнопка "Оплатить"': {
            'По-умолчанию': {
                'содержит правильный URL': makeCase({
                    id: 'm-touch-3845',
                    async test() {
                        const {orderId} = this.params;
                        await this.yandexGoOrderInfoUnpaid.waitForVisible();
                        const url = await this.yandexGoOrderInfoUnpaid.getPayButtonLinkUrl();
                        const previousPage = `/yandex-go/my/orders/confirmation\\?orderId=${orderId}`;

                        return this.browser.expect(url).to.be.link({
                            pathname: '/yandex-go/my/orders/payment',
                            query: {
                                orderId,
                                selectedPaymentMethod: 'YANDEX',
                                previousPage,
                                entrypoint: previousPage,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                    },
                }),
            },
            'При клике': {
                'выполняет переход на другую страницу': makeCase({
                    id: 'm-touch-3846',
                    async test() {
                        await this.yandexGoOrderInfoUnpaid.waitForVisible();

                        return this.browser.allure.runStep(
                            'Клик по кнопке оплаты уводит пользователя с текущей страницы',
                            () => this.browser.yaWaitForPageUnloaded(
                                () => this.yandexGoOrderInfoUnpaid.clickPayButton()
                            )
                        );
                    },
                }),
            },
        },
    },
});
