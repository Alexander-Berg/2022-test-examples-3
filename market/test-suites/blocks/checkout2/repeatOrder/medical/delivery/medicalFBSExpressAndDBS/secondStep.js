import {makeCase, makeSuite} from 'ginny';

// PageObjects
import RecipientForm from '@self/root/src/components/RecipientForm/__pageObject';
import RecipientPopupContainer
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/__pageObject/index.touch';
import RecipientListTouch
    from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject/index.touch';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/touch/goToConfirmationPageAfterMedical';

import ADDRESSES from '@self/root/src/spec/hermione/test-suites/blocks/checkout/constants/addresses';

export default makeSuite('Оформление повторного заказа. Шаг 2.', {
    feature: 'Оформление повторного заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                recipientForm: () => this.createPageObject(RecipientForm),
                recipientPopup: () => this.createPageObject(RecipientPopupContainer),
                recipientList: () => this.createPageObject(RecipientListTouch),
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
                    'В блоке "Доставка курьером" с DBS товаром отображается адрес доставки',
                    async () => {
                        const MOCK_ADDRESS = ADDRESSES.MOSCOW_LAST_ADDRESS.address;

                        await this.addressBlocks.getInfoTitleByCardIndex(0)
                            .should.eventually.include(
                                MOCK_ADDRESS,
                                `Текст в поле адрес должен быть "${MOCK_ADDRESS}".`
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
                    'Кнопка "Перейти к оплате" отображается активной.',
                    async () => {
                        await this.checkoutOrderButton.isButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Перейти к оплате" должна быть активна'
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
