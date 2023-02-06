import {makeCase} from 'ginny';

import withTrying from '@self/root/src/spec/hermione/kadavr-mock/report/outlets/withTrying';

export default makeCase({
    async test() {
        await this.browser.allure.runStep(
            'Должен открыться попап выбора способа доставки',
            async () => {
                await this.deliveryFashionActionButton.click();

                await this.editPopup.isRootVisible()
                    .should.eventually.to.be.equal(
                        true,
                        'Должен открыться попап выбора способа доставки'
                    );
            }
        );

        await this.browser.allure.runStep(
            'Должен открыться экран выбора способа доставки для фешн товара',
            async () => {
                await this.deliveryTypes.setDeliveryTypePickup();
                await this.editPopup.addButtonClick();

                await this.deliveryEditorCheckoutWizard.waitForVisible();

                await this.deliveryEditorCheckoutWizard.getTitleText()
                    .should.eventually.to.be.equal(
                        'Как доставить заказ?',
                        'Должна отображаться страница "Как доставить заказ"'
                    );

                await this.placemarkMap.waitForVisible();
                await this.placemarkMap.waitForReady();
                await this.placemarkMap.waitForPlacemarksVisible();
            }
        );

        await this.browser.allure.runStep(
            'Выбрать любой пункт самовывоза на карте',
            async () => {
                await this.placemarkMap.clickOnOutlet([
                    withTrying.gpsCoord.longitude,
                    withTrying.gpsCoord.latitude,
                ], 15);

                await this.deliveryEditorCheckoutWizard.waitForSubmitButtonSpinnerHidden();
                await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(
                    3000
                );

                await this.browser.allure.runStep(
                    'Отображается информация о дате доставки',
                    async () => {
                        await this.outletInfoCard.getTitleText()
                            .should.eventually.include('Доставка 23 февраля – 8 марта', 'Должна отображаться информация о дате доставки');
                    }
                );

                await this.browser.allure.runStep(
                    'Отображается информация о пункте выдачи',
                    async () => {
                        const MOCK_ADDRESS = withTrying.address.fullAddress;
                        await this.outletInfoCard.getAddress()
                            .should.eventually.include(MOCK_ADDRESS, 'Должна отображаться информация о пункте выдачи');
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Продолжить" активна.',
                    async () => {
                        await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Нажать кнопку "Продолжить"',
            async () => {
                await this.deliveryEditorCheckoutWizard.submitButtonClick();
                await this.confirmationPage.waitForVisible();
            }
        );
    },
});
