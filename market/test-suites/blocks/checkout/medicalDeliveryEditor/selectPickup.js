import {makeCase, makeSuite} from 'ginny';

import {
    checkPickupInfo,
    selectMedicalOutlet,
    fillDeliveryTypeCheckoutForm,
} from '@self/root/src/spec/hermione/scenarios/checkout';
import * as pharma from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';

/**
 * Тест на блок medicalDeliveryEditor
 */
export default ({isCourierAvailable = false, isFillDelivery = false, isCheckPickupInfo = false} = {}) => makeSuite('Самовывоз.', {
    feature: 'Окно доставки лекарств',
    story: {
        'Выбираем аутлет.': makeCase({
            id: 'marketfront-5842',
            issue: 'MARKETFRONT-91160',
            async test() {
                if (isCourierAvailable) {
                    const titleText = 'Доставка лекарств';

                    await this.medicalCartDeliveryEditorCheckoutWizard
                        .getTitleText()
                        .should.eventually.to.be.equal(
                            titleText,
                            `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                        );

                    await this.browser.allure.runStep(
                        'По дефолту отображается способ доставки "Курьером"',
                        async () => {
                            await this.deliveryTypes
                                .isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться доставка "Курьером."'
                                );
                        }
                    );
                }

                if (isFillDelivery) {
                    await this.browser.yaScenario(
                        this,
                        fillDeliveryTypeCheckoutForm
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" активна.',
                        async () => {
                            await this.medicalCartDeliveryEditorCheckoutWizard
                                .isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(
                                    false,
                                    'Кнопка "Продолжить" должна быть активна.'
                                );
                        }
                    );
                }

                await this.deliveryTypes.waitForVisibleDeliveryTypePickup();
                await this.deliveryTypes.setDeliveryTypePickup();

                await this.browser.allure.runStep(
                    'Отображается форма ввода с поиском по городу или улице',
                    async () => {
                        await this.geoSuggest
                            .isVisible()
                            .should.eventually.to.be.equal(
                                true,
                                'Форма должна быть видна'
                            );
                    }
                );

                if (isFillDelivery) {
                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" стала неактивна.',
                        async () => {
                            await this.medicalCartDeliveryEditorCheckoutWizard
                                .isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Кнопка "Продолжить" должна быть неактивна.'
                                );
                        }
                    );
                }

                await this.browser.yaScenario(
                    this,
                    selectMedicalOutlet,
                    {
                        latitude: pharma.outletMock.gpsCoord.latitude,
                        longitude: pharma.outletMock.gpsCoord.longitude,
                        zoom: 15,
                    }
                );

                if (isCheckPickupInfo) {
                    await this.browser.yaScenario(
                        this,
                        checkPickupInfo
                    );
                }

                await this.browser.allure.runStep(
                    'Кнопка "Продолжить" активна.',
                    async () => {
                        await this.medicalCartDeliveryEditorCheckoutWizard.waitForSubmitButton();
                        await this.medicalCartDeliveryEditorCheckoutWizard.waitForEnabledSubmitButton(
                            5000
                        );
                        await this.medicalCartDeliveryEditorCheckoutWizard
                            .isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(
                                false,
                                'Кнопка "Продолжить" должна быть активна.'
                            );
                    }
                );
            },
        }),
    },
});
