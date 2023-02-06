import {makeCase, makeSuite} from 'ginny';

import SubpageHeader from '@self/root/src/components/Checkout/CheckoutSubpageHeader/__pageObject';
import GeoSuggest from '@self/root/src/components/GeoSuggest/__pageObject';

import {selectMedicalOutlet} from '@self/root/src/spec/hermione/scenarios/checkout';

import {outletMock as pharmaOutletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/pharma';

export default makeSuite('Оформление повторного заказа. Шаг 1.', {
    feature: 'Оформление повторного заказа. Шаг 1',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            this.setPageObjects({
                subpageHeader: () => this.createPageObject(SubpageHeader, {
                    parent: this.medicalCartDeliveryEditor,
                }),
                geoSuggest: () => this.createPageObject(GeoSuggest, {
                    parent: this.medicalCartDeliveryEditor,
                }),
            });

            await this.deliveryTypeOptions.waitForVisible();
        },
        'Открыть страницу чекаута.': {
            'Флоу оформления заказа фармы курьером и самовывозом".': makeCase({
                async test() {
                    const titleText = 'Способ доставки';

                    await this.subpageHeader
                        .getTitleText()
                        .should.eventually.to.be.equal(
                            titleText,
                            `Текст заголовка блока должен быть "${titleText}".`
                        );

                    await this.browser.allure.runStep(
                        'По дефолту отображается способ доставки "Курьером"',
                        async () => {
                            await this.deliveryTypeOptions
                                .isCheckedDeliveryTypeDelivery()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Должна отображаться доставка "Курьером."'
                                );
                        }
                    );

                    await this.deliveryTypeOptions.setDeliveryTypePickup();
                    await this.medicalDeliveryButton.click();

                    await this.browser.allure.runStep(
                        'Отображается форма ввода с поиском по городу или улице',
                        async () => {
                            await this.geoSuggest.waitForVisible();
                            await this.geoSuggest
                                .isVisible()
                                .should.eventually.to.be.equal(
                                    true,
                                    'Форма должна быть видна'
                                );
                        }
                    );

                    await this.browser.yaScenario(
                        this,
                        selectMedicalOutlet,
                        {
                            latitude: pharmaOutletMock.gpsCoord.latitude,
                            longitude: pharmaOutletMock.gpsCoord.longitude,
                            zoom: 20,
                        }
                    );

                    await this.browser.allure.runStep(
                        'Попап с информацией об аутлете',
                        async () => {
                            await this.popupSlider.waitForPopupVisible();
                        }
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" активна.',
                        async () => {
                            await this.medicalDeliveryButton
                                .isDisabled()
                                .should.eventually.to.be.equal(
                                    false,
                                    'Кнопка "Продолжить" должна быть активна.'
                                );
                            await this.medicalDeliveryButton.click();
                            await this.confirmationPage.waitForVisible();
                        }
                    );
                },
            }),
        },
    },
});
