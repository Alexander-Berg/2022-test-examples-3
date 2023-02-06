import {
    makeSuite,
    makeCase,
} from 'ginny';

import {
    goToConfirmationPageAfterMedicalCourierDelivery,
} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';

export default makeSuite('Доставка фармы курьером.', {
    id: 'marketfront-5846',
    issue: 'MARKETFRONT-91160',
    feature: 'Покупка списком. Чекаут. Флоу первого заказа',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                geoSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
                courierSuggestInput: () => this.createPageObject(GeoSuggest, {
                    parent: this.medicalCartDeliveryEditorCheckoutWizard,
                }),
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
                                'Доставка курьером 23 февраля – 8 марта',
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
                        await this.recipientBlock
                            .isChooseRecipientButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'В блоке "Получатель" отображается кнопка "Укажите данные получателя"'
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
                    'Кнопка "Перейти к оплате" отображается не активной.',
                    async () => {
                        await this.checkoutOrderButton.isButtonDisabled()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка "Перейти к оплате" должна быть не активна'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Указываем получателя.',
                    async () => {
                        await this.recipientBlock.chooseRecipientButtonClick();
                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recipientForm.waitForVisible();
                            }
                        );
                        await this.browser.allure.runStep(
                            'Заполняем форму получателя.',
                            async () => {
                                await this.recipientForm.setTextForm(userFormData.recipient);
                                await this.recipientForm.saveButtonClick();
                            }
                        );
                        await this.browser.allure.runStep(
                            'Выбираем получателя.',
                            async () => {
                                await this.recipientList.waitForVisible();
                                await this.recipientList.chooseRecipientButtonClick();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Подтвердить заказ" отображается активной.',
                    async () => {
                        await this.checkoutOrderButton.waitForEnabledButton(3000);
                        await this.checkoutOrderButton.isButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Подтвердить заказ" должна быть активна'
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
