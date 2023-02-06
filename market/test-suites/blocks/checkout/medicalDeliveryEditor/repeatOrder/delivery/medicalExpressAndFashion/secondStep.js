import {makeCase, makeSuite} from 'ginny';

import CheckoutRecipient from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/CheckoutRecipient/__pageObject';
import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';
import OutletInfoCard from '@self/root/src/components/OutletInfoCard/__pageObject';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import {outletMock as pharmaOutletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';

import selectFashionDeliveryCase from '../../../selectFashionDelivery';

export default makeSuite('Оформление повторного заказа. Шаг 2.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Оформление повторного заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList),
                recipientBlock: () => this.createPageObject(CheckoutRecipient, {
                    parent: this.confirmationPage,
                }),
                outletInfoCard: () => this.createPageObject(OutletInfoCard),
            });

            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );
        },

        'Проверка заказа фармы".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Самовывоз из пункта выдачи" с фэшн товаром',
                    async () => {
                        await this.deliveryFashionActionButton
                            .isButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Активная кнопка "Выбрать пункт выдачи".'
                            );
                    }
                );

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
                        // в этом кейсе ставим индекс 0, так как информацию по доставке на первой карточке нет
                        // поэтому адрес только на нулевом индексе
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
                    'Кнопка "Перейти к оплате" не активна.',
                    async () => {
                        await this.checkoutOrderButton
                            .isButtonDisabled()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка "Перейти к оплате" должна быть не активна.'
                            );
                    }
                );
            },
        }),

        'Выбираем способ доставки для фешн товара.': selectFashionDeliveryCase,
    },
});
