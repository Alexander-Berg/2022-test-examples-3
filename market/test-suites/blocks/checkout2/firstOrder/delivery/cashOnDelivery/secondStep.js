import {makeCase, makeSuite} from 'ginny';

import {fillRecipientForm, fillFirstStepOfFirstOrder} from '@self/root/src/spec/hermione/scenarios/checkout';

import * as kettle from '@self/root/src/spec/hermione/kadavr-mock/report/kettle';
import userFormData from '@self/root/src/spec/hermione/configs/checkout/formData/user-postpaid';
import {buildCheckouterBucket} from '@self/root/src/spec/utils/checkouter';

export default makeSuite('Оформление первого заказа. Шаг 2.', {
    id: 'marketfront-4425',
    issue: 'MARKETFRONT-45602',
    feature: 'Оформление первого заказа. Шаг 2',
    environment: 'kadavr',
    story: {
        async beforeEach() {
            const carts = [
                buildCheckouterBucket({
                    items: [{
                        skuMock: kettle.skuMock,
                        offerMock: kettle.offerMock,
                        count: 1,
                    }],
                }),
            ];

            await this.browser.yaScenario(
                this,
                fillFirstStepOfFirstOrder,
                carts
            );
        },
        'Указать в форме получателя произвольные данные.': makeCase({
            async test() {
                await this.browser.allure.runStep(
                    'Заполняем форму получателя.',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            fillRecipientForm, {
                                formName: 'user-postpaid',
                                formData: userFormData,
                                recipientForm: this.recipientForm,
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Кнопка "Продолжить" должна стать активной.',
                    async () => {
                        await this.recipientWizard.isSubmitButtonDisabled()
                            .should.eventually.to.be.equal(false, 'Кнопка "Продолжить" должна быть активна.');
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Продолжить" для перехода на страницу подтверждения заказа.',
                    async () => {
                        await this.recipientWizard.submitButtonClick();

                        await this.confirmationPage.waitForVisible();
                    }
                );
            },
        }),
    },
});
