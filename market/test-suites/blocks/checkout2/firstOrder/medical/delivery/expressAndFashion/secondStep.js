import {makeCase, makeSuite} from 'ginny';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/touch/goToConfirmationPageAfterMedical';
import {waitPreloader} from '@self/root/src/spec/hermione/scenarios/checkout';

import selectFashionDeliveryCase from './selectFashionDelivery';

export default makeSuite('Оформление первого заказа. Шаг 2.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Оформление первого заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );
            await this.browser.yaScenario(this, waitPreloader);
        },

        'Проверка заказа фармы.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'В карточке с адресом фешена не должно быть адреса.',
                    async () => {
                        await this.addressBlocks.getInfoTitleByCardIndex(0)
                            .should.eventually.include('Выбрать пункт выдачи', 'Адрес фешена не проставлен');
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами.',
                    async () => {
                        await this.addressBlocks.getAddressTitleByCardIndex(1)
                            .should.eventually.include(
                                'Самовывоз из аптеки 23 февраля – 8 марта',
                                'Текст заголовка должен содержать "Самовывоз".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Блок "Самовывоз" с лекарственными товарами - адрес аптеки и часы работы.',
                    async () => {
                        const outletInfo = ['Пункт самовывоза Retest Full 1\n'] +
                            ['Москва, Сходненская, д. 11, стр. 1\n'] +
                            ['Ежедневно\n'] +
                            ['10:00 – 22:00'];

                        await this.addressBlocks.getInfoTitleByCardIndex(1)
                            .should.eventually.include(
                                outletInfo,
                                `Текст в поле адрес должен быть "${outletInfo}".`
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
                                'На карточке получателя должна отображаться кнопка "Укажите данные получателя".'
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

        'Выбираем способ доставки для фешн товара.': selectFashionDeliveryCase(0),
    },
});
