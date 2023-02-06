import {makeCase, makeSuite} from 'ginny';
import {isEqual} from 'ambar';

/*
 * Проверка настроек по умолчанию в попапе настроек
 * @param {this.prompt} PabeObject.Prompt
 * @param {this.notificationSettingsPopup} PageObject.NotificationSettingsPopup
 * @param {this.params.settings.isAdChecked} boolean ожидаемое значение подписки на рекламу
 * @param {this.params.settings.isWishlistChecked} boolean ожидаемое значение подписки на вишлист
 */
export default makeSuite('Конфигурация попапа настроек', {
    story: {
        'должна иметь ожидаемые стандартные настройки': makeCase({
            async test() {
                await this.prompt.waitForPromptVisible();
                await this.prompt.clickAcceptButton();

                await this.notificationSettingsPopup.waitForNotificationSettingsPopupVisible();

                const [isAdChecked, isWishlistChecked] = await Promise.all([
                    this.notificationSettingsPopup.isAdvCheckboxChecked(),
                    this.notificationSettingsPopup.isWishlistCheckboxChecked(),
                ]);

                const defaultSettings = {isAdChecked, isWishlistChecked};

                return this.expect(isEqual(this.params.settings, defaultSettings)).to.equal(
                    true,
                    'Ожидаемые настройки и настройки по умолчанию совпадают'
                );
            },
        }),
    },
});
