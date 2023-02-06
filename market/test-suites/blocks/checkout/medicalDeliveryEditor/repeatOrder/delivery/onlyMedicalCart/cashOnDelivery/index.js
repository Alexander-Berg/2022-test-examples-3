import {
    makeSuite,
    makeCase,
} from 'ginny';

import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {
    goToConfirmationPageAfterMedicalPickupDelivery,
} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';

export default makeSuite('Самовывоз. Оплата наличными.', {
    id: 'marketfront-5859',
    issue: 'MARKETFRONT-81686',
    feature: 'Покупка списком. Чекаут. Флоу повторного заказа',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );
        },
        'Выбрать способ оплаты "Наличными при получении"': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами.',
                    async () => {
                        await this.groupedParcels
                            .getAddressTitleByCardIndex(0)
                            .should.eventually.include(
                                'Самовывоз из аптеки 23 февраля – 8 марта',
                                'Текст заголовка должен содержать "Самовывоз".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами - адрес аптеки и часы работы.',
                    async () => {
                        const MOCK_ADDRESS = pharma.outletMock.address.fullAddress;
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                MOCK_ADDRESS,
                                'Текст информации о магазине должен содержать адрес доставки.'
                            );

                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.to.be.include(
                                '10:00 – 22:00',
                                'Должны отображаться часы работы.'
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
                    'В блоке "Способ оплаты" отображается кнопка "Изменить"',
                    async () => {
                        await this.paymentOptionsEditableCard
                            .isChangeButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'В блоке "Способ оплаты" отображается кнопка "Изменить"'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'В блоке "Способ оплаты" выбираем "Наличными при получении"',
                    async () => {
                        await this.paymentOptionsEditableCard.changeButtonClick();
                        await this.editPopup.waitForVisibleRoot();
                        return this.browser.allure.runStep(
                            'Установить способ оплаты "Наличными при получении"',
                            async () => {
                                await this.paymentOptions.setPaymentTypeCashOnDelivery();
                                return this.paymentOptions.submitButtonClick();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Ожидаем изменения урла на: "/my/orders/confirmation".',
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
                                pathname: '/my/orders/confirmation',
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
