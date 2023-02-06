import {makeCase, makeSuite} from 'ginny';

const SUBMIT_GOAL_ID_UNIVERSAL_PART = 'settings-popup_notification-settings-settings-submit';

/*
 * Сохранение настроек пользователя
 * @param {this.prompt} PabeObject.Prompt
 * @param {this.params.zonePart} PabeObject.counterId
 * @param {this.notificationSettingsPopup} PageObject.NotificationSettingsPopup
 */
export default makeSuite('Метрика попапа настроек', {
    story: {
        'Попап настроек при клике на кнопку "Сохранить"': {
            'должен отправить правильную цель метрики': makeCase({
                async test() {
                    const zonePart = this.params.zonePart || '';

                    await this.prompt.waitForPromptVisible();
                    await this.prompt.clickAcceptButton();

                    await this.notificationSettingsPopup.waitForNotificationSettingsPopupVisible();

                    await Promise.all([
                        this.notificationSettingsPopup.clickSubmitButton(),
                        this.notificationSettingsPopup.waitForNotificationSettingsPopupHide(),
                    ]);

                    let goal;

                    await this.allure.runStep(
                        'Ищем цель метрики после сохранения настроек',
                        () => {
                            goal = this.browser.yaGetMetricaGoal(
                                this.params.counterId,
                                zonePart + SUBMIT_GOAL_ID_UNIVERSAL_PART
                            );
                        }
                    );

                    return this.expect(Boolean(goal)).be.equal(true, 'Цель на сохранение настроен найдена');
                },
            }),
        },
    },
});
