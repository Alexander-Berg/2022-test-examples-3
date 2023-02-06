import {makeCase, makeSuite} from 'ginny';

import RecipientForm from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientForm/__pageObject';
import RecipientList from '@self/root/src/widgets/content/checkout/common/CheckoutRecipient/components/Popup/components/RecipientList/__pageObject';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';

export default makeSuite('Оформление первого заказа. Шаг 3.', {
    id: 'marketfront-5899',
    issue: 'MARKETFRONT-81900',
    feature: 'Оформление первого заказа. Шаг 3',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                recipientForm: () => this.createPageObject(RecipientForm),
                recipientList: () => this.createPageObject(RecipientList),
            });

            await this.medicalCartDeliveryEditorCheckoutWizard.waitForVisible();
            await this.deliveryTypes.waitForVisible();

            await this.browser.yaScenario(
                this,
                goToConfirmationPageAfterMedicalPickupDelivery
            );

            await this.deliveryFashionActionButton.click();
            await this.deliveryTypes.setDeliveryTypePickup();
            await this.editPopup.addButtonClick();
            await this.deliveryEditorCheckoutWizard.waitForVisible();

            await this.placemarkMap.waitForVisible();
            await this.placemarkMap.waitForReady();
            await this.placemarkMap.waitForPlacemarksVisible();

            await this.placemarkMap.clickOnOutlet([
                withTrying.gpsCoord.longitude,
                withTrying.gpsCoord.latitude,
            ], 25);

            await this.deliveryEditorCheckoutWizard.waitForSubmitButtonSpinnerHidden();
            await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(
                3000
            );

            await this.deliveryEditorCheckoutWizard.submitButtonClick();
            await this.confirmationPage.waitForVisible();
        },

        'Проверка заказа фешена".': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Блок "Самовывоз" фешн заказа - адрес пункта выдачи и часы работы.',
                    async () => {
                        const MOCK_ADDRESS = withTrying.address.fullAddress;
                        await this.groupedParcels
                            .getInfoContentByCardIndex(0)
                            .should.eventually.include(
                                MOCK_ADDRESS,
                                'Текст информации о магазине должен содержать адрес доставки.'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Перейти к оплате" отображается не активной.',
                    async () => {
                        await this.checkoutOrderButton.isButtonDisabled()
                            .should.eventually.to.be.equal(
                                true,
                                'Кнопка "Подтвердить заказ" должна быть не активна'
                            );
                    }
                );
            },
        }),

        'Указать данные в форме Получатель и перейти к оплате.': makeCase({
            async test() {
                await this.recipientBlock.chooseRecipientButtonClick();
                await this.recipientForm.waitForVisible();
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

                await this.browser.allure.runStep(
                    'Кнопка "Перейти к оплате" отображается активной.',
                    async () => {
                        await this.checkoutOrderButton.waitForEnabledButton(3000);
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
