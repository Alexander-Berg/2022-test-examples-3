import {makeCase, makeSuite} from 'ginny';

import {fillFirstStepOfFirstOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import {
    outletMock as farmaOutletMock,
} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';

import {carts} from './helpers';

export default makeSuite('Шаг 2.', {
    id: 'marketfront-4426',
    issue: 'MARKETFRONT-36074',
    feature: 'Шаг 2.',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            await this.browser.yaScenario(
                this,
                fillFirstStepOfFirstOrder,
                {hasOtherParcel: true},
                carts
            );

            await this.placemarkMap.waitForVisible(4000);
            await this.placemarkMap.waitForReady(4000);
        },
        'Для второй посылки (C&C).': {
            'Должен появиться экран "Как доставить заказ?".': makeCase({
                async test() {
                    const titleText = 'Как доставить заказ?';

                    await this.deliveryEditorCheckoutWizard.getTitleText()
                        .should.eventually.be.equal(
                            titleText,
                            `Текст заголовка блока с оформлением заказа должен быть "${titleText}".`
                        );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" заблокирована.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(true, 'Кнопка "Продолжить" должна быть заблокирована.');
                        }
                    );
                },
            }),
            'Выбрать доступное ПВЗ.': makeCase({
                async test() {
                    await this.browser.allure.runStep(
                        'Кликнуть по доступному ПВЗ.',
                        async () => {
                            await this.placemarkMap.clickOnOutlet([
                                farmaOutletMock.gpsCoord.longitude,
                                farmaOutletMock.gpsCoord.latitude,
                            ]);
                        }
                    );

                    await this.browser.allure.runStep(
                        'Кнопка "Продолжить" активна.',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.waitForEnabledSubmitButton(3000);
                            await this.deliveryEditorCheckoutWizard.isSubmitButtonDisabled()
                                .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');
                        }
                    );

                    await this.browser.allure.runStep(
                        'Нажать кнопку "Продолжить" для перехода к экрану "Получатель".',
                        async () => {
                            await this.deliveryEditorCheckoutWizard.submitButtonClick();
                            await this.recipientWizard.waitForVisible();
                        }
                    );
                },
            }),
        },
    },
});
