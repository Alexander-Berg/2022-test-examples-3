import {makeCase} from 'ginny';

import {outletMock as pharmaOutletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';

export default index => makeCase({
    async test() {
        await this.browser.allure.runStep(
            'Нажать на кнопку "Выбрать пункт выдачи" в блоке заказа.',
            async () => {
                await this.addressBlocks.clickChangeButtonByIndex(index);
                await this.editPopup.waitForVisibleRoot(5000);
            }
        );

        await this.browser.allure.runStep(
            'Нажать кнопку "Добавить новый".',
            async () => {
                await this.browser.allure.runStep(
                    'Над чипсами присутствует кнопка "Добавить новый".',
                    async () => {
                        await this.editPopup.isAddNewButtonVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Над чипсами должна быть кнопка "Добавить новый".'
                            );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Добавить новый".',
                    async () => {
                        await this.editPopup.clickAddNewButton();
                    }
                );
            }
        );

        await this.allure.runStep(
            'Для C&C выбрать доступный ПВЗ.', async () => {
                await this.placemarkMap.waitForVisible(2000);
                await this.placemarkMap.waitForReady(4000);

                await this.placemarkMap.clickOnPlacemark([
                    pharmaOutletMock.gpsCoord.latitude,
                    pharmaOutletMock.gpsCoord.longitude,
                ], 20);
            }
        );

        await this.allure.runStep(
            'Нажать кнопку "Выбрать".', async () => {
                await this.deliveryEditor.chooseButtonClick();
            }
        );

        await this.allure.runStep(
            'Открыть страницу чекаута', async () => {
                await this.confirmationPage.waitForVisible();
                await this.preloader.waitForHidden(5000);
            }
        );
    },
});
