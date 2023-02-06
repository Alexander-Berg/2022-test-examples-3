import {
    makeSuite,
    makeCase,
} from 'ginny';

import {
    goToConfirmationPageAfterMedicalCourierDelivery,
} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import {PAYMENT_METHOD, PAYMENT_TYPE} from '@self/root/src/entities/payment';

export default makeSuite('Курьер.', {
    id: 'marketfront-5860',
    issue: 'MARKETFRONT-91160',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.setState('persAddress.lastState', {
                paymentType: PAYMENT_TYPE.PREPAID,
                paymentMethod: PAYMENT_METHOD.YANDEX,
                contactId: null,
                parcelsInfo: null,
            });

            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalCourierDelivery
            );
        },
        'Открыть страницу главного чекаута': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Доставка" с лекарственными товарами.',
                    async () => {
                        await this.groupedParcels
                            .getAddressTitleByCardIndex(0)
                            .should.eventually.include(
                                'Доставка курьером 23 февраля – 8 марта•250₽',
                                'Текст заголовка должен содержать "Доставка".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Доставка" с лекарственными товарами - адрес аптеки.',
                    async () => {
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                'Курьером\nМосква, Красная площадь, д. 1',
                                'Текст информации о магазине должен содержать адрес доставки.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Получатель".',
                    async () => {
                        const MOCK_CONTACT = 'Вася Пупкин\npupochek@yandex.ru, 89876543210';
                        await this.recipientBlock
                            .getContactText()
                            .should.eventually.to.be.equal(
                                MOCK_CONTACT,
                                'В блоке "Получатель" отображаются данные, которые были указаны в моках'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" отображается "Новой картой"',
                    async () => {
                        await this.paymentOptionsBlock
                            .getText()
                            .should.eventually.include(
                                'Новой картой',
                                'В блоке "Способ оплаты" отображается "Новой картой"'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем изменения урла на: "/my/orders/payment".',
                    async () => {
                        await this.browser.setState('Checkouter.options', {isCheckoutSuccessful: true});
                        await this.browser.yaWaitForChangeUrl(
                            async () => {
                                await this.checkoutOrderButton.click();
                            },
                            5000
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
                    }
                );
            },
        }),
    },
});
