import {makeCase, makeSuite} from 'ginny';

import OutletInfoCard from '@self/root/src/components/OutletInfoCard/__pageObject/';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';

import selectFashionDeliveryCase from '../../../selectFashionDelivery';

export default makeSuite('Оформление первого заказа. Шаг 2.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Оформление первого заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                outletInfoCard: () => this.createPageObject(OutletInfoCard),
            });

            await this.medicalCartDeliveryEditorCheckoutWizard.waitForVisible();
            await this.deliveryTypes.waitForVisible();

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
                        // в этом кейсе ставим индекс 0, так как информацию по доставке на первой карточке нет
                        // поэтому адрес только на нулевом индексе
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                'Москва, Сходненская, д. 11, стр. 1',
                                'Текст информации о магазине должен содорежать адрес доставки.'
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
                        await this.recipientBlock
                            .isChooseRecipientButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'На карточке получателя должна отображается кнопка "Укажите данные получателя".'
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
                                'Кнопка "Перейти к оплате" дожна быть не активна.'
                            );
                    }
                );
            },
        }),

        'Выбираем способ доставки для фешн товара.': selectFashionDeliveryCase,
    },
});
