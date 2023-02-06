import {makeCase} from 'ginny';

export default makeCase({
    issue: 'MARKETFRONT-54555',
    id: 'marketfront-5031',

    async test() {
        await this.browser.allure.runStep(
            'Нажать на кнопку "Назад".',
            async () => {
                await this.deliveryEditorCheckoutWizard.backButtonClick();

                await this.browser.allure.runStep(
                    'Происходит возврат к попапу "Изменить адрес".',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.editPopup.waitForVisibleRoot();
                    }
                );
            }
        );
    },
});
