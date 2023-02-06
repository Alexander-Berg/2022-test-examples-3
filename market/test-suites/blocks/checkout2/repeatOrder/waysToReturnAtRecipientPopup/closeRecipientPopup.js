import {makeCase} from 'ginny';

export default makeCase({
    issue: 'MARKETFRONT-54581',
    id: 'marketfront-4913',

    async test() {
        await this.browser.allure.runStep(
            'Нажать на кнопку "Х".',
            async () => {
                await this.popupBase.clickOnCrossButton();
                await this.browser.allure.runStep(
                    'Закрывается попап "Получатели" со списком получателей.',
                    async () => {
                        await this.editPopup.waitForRootInvisible();
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Нажать кнопку "Изменить" в блоке "Получатель".',
            async () => {
                await this.recipientEditableCard.changeButtonClick();
                await this.browser.allure.runStep(
                    'Открывается попап "Получатели" со списком получателей.',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Кликнуть в область вне попапа "Получатели".',
            async () => {
                await this.popupBase.clickOutsideContent();
                await this.browser.allure.runStep(
                    'Закрывается попап "Получатели" со списком получателей.',
                    async () => {
                        await this.editPopup.waitForRootInvisible();
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Нажать кнопку "Изменить" в блоке "Получатель".',
            async () => {
                await this.recipientEditableCard.changeButtonClick();
                await this.browser.allure.runStep(
                    'Открывается попап "Получатели" со списком получателей.',
                    async () => {
                        await this.editPopup.waitForVisibleRoot();
                    }
                );
            }
        );

        await this.browser.allure.runStep(
            'Нажать на клавишу ESC.',
            async () => {
                await this.popupBase.closeOnEscape();
                await this.browser.allure.runStep(
                    'Закрывается попап "Получатели" со списком получателей.',
                    async () => {
                        await this.editPopup.waitForRootInvisible();
                    }
                );
            }
        );
    },
});
