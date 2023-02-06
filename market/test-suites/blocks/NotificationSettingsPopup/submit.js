import {makeCase, makeSuite} from 'ginny';

/*
 * Сохранение настроек пользователя
 * @param {this.prompt} PabeObject.Prompt
 * @param {this.notificationSettingsPopup} PageObject.NotificationSettingsPopup
 */
export default makeSuite('Сохранение настроек уведомления в попапе настроек', {
    story: {
        'Попап настроек при клике на кнопку "Сохранить"': {
            'должен сохранить настройки и закрыться': makeCase({
                async test() {
                    await this.prompt.waitForPromptVisible();
                    await this.prompt.clickAcceptButton();

                    await this.notificationSettingsPopup.waitForNotificationSettingsPopupVisible();

                    await Promise.all([
                        this.notificationSettingsPopup.clickSubmitButton(),
                        this.notificationSettingsPopup.waitForNotificationSettingsPopupHide(),
                    ]);
                },
            }),
        },
    },
});
