const clientObjects = require('../page-objects/client');
const { openSettings, openConnectionScreen } = require('../helpers/joinToConferenceHelper');
const { openSettings: openSettingsFromToolbar, openShowMorePopup } = require('../helpers/toolbarHelper');
const {
    openMenuTab,
    openSelect,
    setSelectValue,
    changeTumblerValue,
    closeSettings,
    assertView,
    clickAndAssertUrl
} = require('../helpers/settingsHelper');

// В firefox-desktop переодически начали падать тесты (причём разные) с ошибкой «Document is cookie-averse»
hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');
describe('Настройки ->', () => {
    it('telemost-298: Открытие попапа настроек на экране подключения незалогином', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);
        await assertView(bro, 'telemost-298-open-settings-anonymous');
    });

    it('telemost-283: Открытие попапа настроек на экране подключения', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: false });
        await openSettings(bro);
        await assertView(bro, 'telemost-283-open-settings');
    });

    it('telemost-294: Измененение имени и аватарки через настройки', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: false });
        await openSettings(bro);
        await clickAndAssertUrl(
            bro,
            clientObjects.common.settingsModal.changeAvatarButton(),
            'https://passport.yandex.ru/profile/public'
        );
    });

    it('telemost-284: Вход в аккаунт на экране подключения через настройки', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);
        await clickAndAssertUrl(
            bro,
            clientObjects.common.settingsModal.loginButton(),
            'https://passport.yandex.ru/auth'
        );
    });

    hermione.skip.in('chrome-desktop', 'В Chrome другой экран');
    it('telemost-295: [Настройки] Вкладка "Звук"', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);
        await openMenuTab(bro, clientObjects.common.settingsModal.menu.audio());
        await assertView(bro, 'telemost-295-audio-settings');
    });

    hermione.skip.in('chrome-desktop', 'В Chrome другой экран');
    it('telemost-296: [Настройки] Вкладка "Камера"', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);
        await openMenuTab(bro, clientObjects.common.settingsModal.menu.video());
        await assertView(bro, 'telemost-296-video-settings');
    });

    hermione.skip.in('firefox-desktop', 'В Firefox другой экран');
    it('telemost-299: [Настройки] Вкладка "Звук"', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);

        await openMenuTab(bro, clientObjects.common.settingsModal.menu.audio());
        await assertView(bro, 'telemost-299-open-audio-tab');

        await openSelect(bro, clientObjects.common.settingsModal.micSelect());
        await assertView(bro, 'telemost-299-open-mic-select');

        await setSelectValue(bro, clientObjects.common.micSelectSecondOption());
        await assertView(bro, 'telemost-299-select-second-mic-option');

        await openSelect(bro, clientObjects.common.settingsModal.micSelect());
        await setSelectValue(bro, clientObjects.common.micSelectThirdOption());
        await assertView(bro, 'telemost-299-select-third-mic-option');

        await openSelect(bro, clientObjects.common.settingsModal.audioSelect());
        await assertView(bro, 'telemost-299-open-audio-select');

        await setSelectValue(bro, clientObjects.common.audioSelectSecondOption());
        await assertView(bro, 'telemost-299-select-second-audio-option');

        await openSelect(bro, clientObjects.common.settingsModal.audioSelect());
        await setSelectValue(bro, clientObjects.common.audioSelectThirdOption());
        await assertView(bro, 'telemost-299-select-third-audio-option');
    });

    hermione.skip.in('firefox-desktop', 'В Firefox другой экран');
    it('telemost-300: [Настройки] Вкладка "Камера"', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);

        await openMenuTab(bro, clientObjects.common.settingsModal.menu.video());
        await assertView(bro, 'telemost-300-open-video-tab');

        await openSelect(bro, clientObjects.common.settingsModal.videoSelect());
        await assertView(bro, 'telemost-300-open-video-select');

        await setSelectValue(bro, clientObjects.common.videoSelectFirstOption());
        await assertView(bro, 'telemost-300-select-first-option');
    });

    it('telemost-297: Переход на страницу "Справка и поддержка" из Настроек', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await openSettings(bro);

        await openMenuTab(bro, clientObjects.common.settingsModal.menu.support());
        await assertView(bro, 'telemost-297-open-support-tab');
        await clickAndAssertUrl(
            bro,
            clientObjects.common.settingsModal.supportLink(),
            'https://yandex.ru/support/telemost/?lang=ru',
            { newTab: true }
        );
    });

    it('telemost-199: Отключение отображения своего видео для себя', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await bro.click(clientObjects.common.continueConnectButton());
        await bro.yaWaitForVisible(clientObjects.common.toolbar());
        await openShowMorePopup(bro);
        await openSettingsFromToolbar(bro);

        await openMenuTab(bro, clientObjects.common.settingsModal.menu.video());
        await changeTumblerValue(bro);
        await closeSettings(bro);
        await bro.pause(1000);
        await assertView(bro, 'telemost-199-turn-off-showing-video');

        await openShowMorePopup(bro);
        await openSettingsFromToolbar(bro);
        await openMenuTab(bro, clientObjects.common.settingsModal.menu.video());
        await changeTumblerValue(bro);
        await closeSettings(bro);
        await bro.pause(1000);
        await assertView(bro, 'telemost-199-turn-on-showing-video');
    });
});
