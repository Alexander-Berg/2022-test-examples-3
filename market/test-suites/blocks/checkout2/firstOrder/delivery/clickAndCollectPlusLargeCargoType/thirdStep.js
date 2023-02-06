import {makeCase, makeSuite} from 'ginny';

import {fillRecipientForm, fillFirstStepOfFirstOrder} from '@self/root/src/spec/hermione/scenarios/checkout';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';
import {outletMock as farmaOutletMock} from '@self/root/src/spec/hermione/kadavr-mock/report/farma';

import {carts} from './helpers';

export default makeSuite('Шаг 3.', {
    id: 'marketfront-4426',
    issue: 'MARKETFRONT-36074',
    feature: 'Шаг 3.',
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
            await this.placemarkMap.clickOnOutlet([
                farmaOutletMock.gpsCoord.longitude,
                farmaOutletMock.gpsCoord.latitude,
            ]);
            await this.deliveryEditorCheckoutWizard
                .waitForEnabledSubmitButton(3000)
                .catch(() => this.placemarkMap.clickOnOutlet([
                    farmaOutletMock.gpsCoord.longitude,
                    farmaOutletMock.gpsCoord.latitude,
                ]));
            await this.deliveryEditorCheckoutWizard.submitButtonClick();
            await this.recipientWizard.waitForVisible();

            await this.browser.yaScenario(
                this,
                fillRecipientForm,
                {
                    formName: 'user-postpaid',
                    formData: userFormData,
                    recipientForm: this.recipientForm,
                }
            );
        },
        'Указать в форме получателя произвольные данные.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Кнопка "Продолжить" активна.',
                    async () => {
                        await this.recipientWizard.waitForEnabledSubmitButton(3000);
                        await this.recipientWizard.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Продолжить".',
                    async () => {
                        await this.recipientWizard.submitButtonClick();

                        await this.confirmationPage.waitForVisible();
                    }
                );
            },
        }),
    },
});
