import {makeCase} from 'ginny';

import {CONTACTS} from '../../constants';

const recipient =
    `${CONTACTS.DEFAULT_CONTACT.recipient}\n${CONTACTS.DEFAULT_CONTACT.email}, ${CONTACTS.DEFAULT_CONTACT.phone}`;

export default makeCase({
    issue: 'MARKETFRONT-54581',
    id: 'marketfront-4913',

    async test() {
        await this.browser.allure.runStep(
            'Нажать на кнопку "Карандаш".',
            async () => {
                await this.recipientList.clickOnEditButtonByRecipient(recipient);
                await this.browser.allure.runStep(
                    'Открывается форма редактирования данных пользователя "Изменить получателя".',
                    async () => {
                        await this.recepientForm.waitForVisible();
                    }
                );
            }
        );

        await this.allure.runStep(
            'Нажать на кнопку "Удалить".', async () => {
                await this.recepientForm.deleteButtonClick();
                await this.allure.runStep('Появляется попап "И правда хотите удалить получателя?".', async () => {
                    await this.deleteForm.waitForVisible();
                });
            }
        );

        await this.allure.runStep(
            'Нажать на кнопку "Отмена".', async () => {
                await this.deleteForm.cancelButtonClick();
                await this.allure.runStep('Происходит возврат к форме "Изменить получателя".', async () => {
                    await this.recepientForm.waitForVisible();
                });
            }
        );
    },
});
