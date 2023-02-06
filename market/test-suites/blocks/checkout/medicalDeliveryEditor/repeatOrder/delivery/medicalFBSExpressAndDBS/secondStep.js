import {makeCase, makeSuite} from 'ginny';

import GroupedParcels from '@self/root/src/widgets/content/checkout/common/CheckoutParcels/components/View/__pageObject';
import CheckoutRecipient from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import CheckoutOrderButton from '@self/root/src/widgets/content/checkout/common/CheckoutOrderButton/components/View/__pageObject';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import {outletMock as pharmaOutletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';
import {ADDRESSES} from '../../../../constants';

export default makeSuite('Оформление повторного заказа. Шаг 2.', {
    feature: 'Оформление повторного заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                groupedParcels: () => this.createPageObject(GroupedParcels, {
                    parent: this.confirmationPage,
                }),
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                checkoutOrderButton: () => this.createPageObject(CheckoutOrderButton, {
                    parent: this.confirmationPage,
                }),
            });

            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );
        },

        'Проверка заказа и переход к оплате".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами.',
                    async () => {
                        await this.groupedParcels
                            .getAddressTitleByCardIndex(1)
                            .should.eventually.include(
                                'Самовывоз из аптеки 23 февраля – 8 марта',
                                'Текст заголовка должен содержать "Самовывоз".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами - адрес аптеки и часы работы.',
                    async () => {
                        const MOCK_ADDRESS = pharmaOutletMock.address.fullAddress;
                        await this.groupedParcels
                            .getInfoContentByCardIndex(1)
                            .should.eventually.include(
                                MOCK_ADDRESS,
                                'Текст информации о магазине должен содержать адрес доставки.'
                            );

                        await this.groupedParcels
                            .getInfoContentByCardIndex(1)
                            .should.eventually.to.be.include(
                                '10:00 – 22:00',
                                'Должны отображаться часы работы.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'В блоке "Доставка курьером" с DBS товаром отображается адрес доставки',
                    async () => {
                        const MOCK_ADDRESS = ADDRESSES.MOSCOW_LAST_ADDRESS.address;
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                MOCK_ADDRESS,
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
                    'Кнопка "Перейти к оплате" активна.',
                    async () => {
                        await this.checkoutOrderButton
                            .isButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Перейти к оплате" должна быть активна.'
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
