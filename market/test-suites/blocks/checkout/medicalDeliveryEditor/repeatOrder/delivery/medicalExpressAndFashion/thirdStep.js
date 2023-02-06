import {makeCase, makeSuite} from 'ginny';

import DeliveryTypeList from '@self/root/src/components/DeliveryTypes/__pageObject';

import {goToConfirmationPageAfterMedicalPickupDelivery} from '@self/root/src/spec/hermione/scenarios/checkout/goToConfirmationPageAfterMedical';
import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';

export default makeSuite('Оформление повторного заказа. Шаг 3.', {
    id: 'marketfront-5900',
    issue: 'MARKETFRONT-81908',
    feature: 'Оформление повторного заказа. Шаг 3',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                deliveryTypes: () => this.createPageObject(DeliveryTypeList),
            });

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

        'Проверка заказа фешена и переход к оплате".': makeCase({
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
