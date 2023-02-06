const clientObjects = require('../page-objects/client');
const {
    assertView,
    openConnectionScreen,
    connectToConference,
    turnOffDevices,
    turnOnDevices
} = require('../helpers/joinToConferenceHelper');

hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');
describe('Экран подключения ->', () => {
    it('telemost-281: Переход на главную страницу с экрана подключения (залогин)', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro);
        await assertView(bro, 'telemost-281-open-connection-screen');

        await bro.yaWaitForVisible(clientObjects.common.overlay.closeButton());
        await bro.click(clientObjects.common.overlay.closeButton());

        await bro.yaWaitForVisible(clientObjects.common.createConferenceButton());
        await assertView(bro, 'telemost-281-click-close-button');
    });

    it('telemost-282: Переход на главную страницу с экрана подключения (незалогин)', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });
        await assertView(bro, 'telemost-282-open-connection-screen');

        await bro.yaWaitForVisible(clientObjects.common.overlay.closeButton());
        await bro.click(clientObjects.common.overlay.closeButton());

        await bro.yaWaitForVisible(clientObjects.common.createConferenceButton());
        await assertView(bro, 'telemost-282-click-close-button');
    });

    it('telemost-287: Ввод имени участника встречи (незалогин)', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });

        // Почему-то этот метод не стирает содержимое, перед вводом нового значения,
        // поэтому вначале строки поставил пробел, чтобы отделить прошлое от текущего значения
        // https://github.com/webdriverio/webdriverio/issues/1140
        await bro.setValue(clientObjects.common.participant.input(), ' Екатерина Анатольевна');
        // Убрать фокус с инпута, чтобы скрин всегда был без курсора,
        // иначе могут быть конфликты
        await bro.click(clientObjects.common.overlay());
        await assertView(bro, 'telemost-287-type-guest-name');

        await connectToConference(bro);
        await bro.yaResetPointerPosition();
        await bro.pause(500);
        await assertView(bro, 'telemost-287-connect-to-conference');
    });

    it('telemost-288: Вход во встречу с замьюченным видео и замьюченным микрофоном', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });

        await turnOffDevices(bro);
        await connectToConference(bro);
        await bro.yaResetPointerPosition();
        await bro.pause(500);
        await assertView(bro, 'telemost-muted-devices-connect-to-conference');
    });

    it('telemost-289: Мьют/анмьют аудио и видео на экране подключения к встрече', async function () {
        const bro = this.browser;
        await openConnectionScreen(bro, { anonymous: true });

        await turnOffDevices(bro);
        await assertView(bro, 'telemost-devices-turn-off-devices');

        await turnOnDevices(bro);
        await assertView(bro, 'telemost-devices-turn-on-devices');
    });
});
