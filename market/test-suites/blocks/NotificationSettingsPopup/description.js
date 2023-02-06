import {makeCase, makeSuite} from 'ginny';

/*
 * Проверка email пользователя в попапе настроек
 * @param {this.params.email} string
 * @param {this.prompt} PabeObject.Prompt
 * @param {this.notificationSettingsPopup} PageObject.NotificationSettingsPopup
 */
export default makeSuite('Описание попапа настроек.', {
    story: {
        'Должно содержать': {
            'email пользователя': makeCase({
                async test() {
                    await this.prompt.waitForPromptVisible();
                    await this.prompt.clickAcceptButton();

                    await this.notificationSettingsPopup.waitForNotificationSettingsPopupVisible();
                    const descriptionText = await this.notificationSettingsPopup.getDescriptionText();

                    return this.expect(descriptionText).to.contain(
                        this.params.email,
                        'Описание содержит email'
                    );
                },
            }),
        },
    },
});
