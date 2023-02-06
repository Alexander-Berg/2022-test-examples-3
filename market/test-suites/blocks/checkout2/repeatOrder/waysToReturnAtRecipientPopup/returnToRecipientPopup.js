import {makeCase} from 'ginny';

export default makeCase({
    issue: 'MARKETFRONT-54581',
    id: 'marketfront-4913',

    async test() {
        await this.allure.runStep(
            'Нажать на кнопку "Добавить получателя".', async () => {
                await this.recipientList.addRecipientButtonClick();
                await this.allure.runStep('Открывается форма указания данных получателя.', async () => {
                    await this.recepientForm.waitForVisible();
                });
            }
        );

        await this.allure.runStep(
            'Нажать на кнопку "Стрелочка".', async () => {
                await this.recepientForm.arrowLeftClick();
                await this.allure.runStep('Происходит возврат к списку получателей.', async () => {
                    await this.recipientList.waitForVisible();
                });
            }
        );

        await this.allure.runStep(
            'Нажать на кнопку "Добавить получателя".', async () => {
                await this.recipientList.addRecipientButtonClick();
                await this.allure.runStep('Открывается форма указания данных получателя.', async () => {
                    await this.recepientForm.waitForVisible();
                });
            }
        );

        await this.allure.runStep(
            'Нажать на кнопку "Отмена".', async () => {
                await this.recepientForm.cancelButtonClick();
                await this.allure.runStep('Происходит возврат к списку получателей.', async () => {
                    await this.recipientList.waitForVisible();
                });
            }
        );
    },
});
