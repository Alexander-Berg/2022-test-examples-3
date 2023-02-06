const _isString = require('lodash/isString');
const assert = require('chai').assert;
const clientObjects = require('../page-objects/client');
const toolbarHelper = require('../helpers/toolbarHelper');
const ConfMembersManager = require('../helpers/confMembersManager');

const hideElements = [
    clientObjects.common.backgroundImage(),
    clientObjects.common.videoOfParticipant(),
    clientObjects.common.psHeader(),
    clientObjects.common.messageBox(),
    clientObjects.common.participantsPopup.input(),
    clientObjects.common.participantsPopup.icon(),
    clientObjects.common.toolbar.cancelRecording.timer()
];

/**
 * @param {Object} bro
 */
async function assertParticipantsPopupData(bro) {
    const linkRegex = /\/j\/(\d+)/;

    const url = await bro.getUrl();
    const inputUrl = await bro.getValue(clientObjects.common.participantsPopup.input());

    const [, id] = linkRegex.exec(inputUrl) || [];
    const [, expectedId] = linkRegex.exec(url) || [];

    assert(_isString(id) && id === expectedId, 'Неверная ссылка на видеовстречу');
}

/**
 * @param {Object} bro
 * @param {Object} options
 */
async function run(
    bro,
    {
        assertPostfix = '',
        turnOffDevices,
        startSharing,
        cancelSharing,
        openChat,
        startRecording,
        skipToolbarRecording,
        waitForIcon = true
    }
) {
    /**
     * @param {string} assertName
     * @returns {string}
     */
    function makeAssertName(assertName) {
        return ['telemost-toolbar', assertName, `(${assertPostfix})`].join('-');
    }

    /**
     * @param {string} assertName
     * @returns {Promise}
     */
    function assertView(assertName) {
        return bro.yaAssertView(makeAssertName(assertName), 'body', { hideElements });
    }

    const confManager = new ConfMembersManager(bro);
    await confManager.createConferenceByUser('yndx-telemost-test-9');
    await bro.deleteCookie();

    const url = await bro.getUrl();
    await bro.url(url + '?test-id=0');
    await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
    await bro.click(clientObjects.common.enterConferenceButton());
    await bro.yaWaitForVisible(clientObjects.common.toolbar());
    if (waitForIcon) {
        await bro.yaWaitForVisible(clientObjects.common.toolbar.videoButtonOff());
    }
    await assertView('join-conference');

    await toolbarHelper.openParticipantsPopup(bro);
    await assertParticipantsPopupData(bro);
    await assertView('open-participants-popup');

    await turnOffDevices(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('turn-off-devices');

    await startSharing(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('start-sharing');

    await cancelSharing(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('cancel-sharing');

    await openChat(bro);
    await assertView('open-chat');

    await toolbarHelper.closeChat(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('close-chat');

    await toolbarHelper.setPresenterView(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('presenter-view');

    await toolbarHelper.setGalleryView(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('gallery-view');

    await startRecording(bro);
    await toolbarHelper.openShowMorePopup(bro);
    await assertView('start-recording');

    await toolbarHelper.cancelRecordingFromPopup(bro);
    await toolbarHelper.openShowMorePopup(bro);
    // После отмены записи экрана, браузер сохранит файл,
    // который отобразится внизу в загрузках, из-за чего изменится вьюпорт,
    // и скриншот может не совпасть, поэтому здесь ждём, что файл скачался,
    // чтобы вьюпорты скринов совпали.
    // При повторных записях, задержки не потребуются, так как панель загрузок
    // уже будет отображена
    await bro.pause(5000);
    await assertView('cancel-recording-from-popup');

    if (!skipToolbarRecording) {
        await startRecording(bro);
        await toolbarHelper.openShowMorePopup(bro);
        await assertView('start-recording-again');

        await toolbarHelper.cancelRecordingFromToolbar(bro);
        await toolbarHelper.openShowMorePopup(bro);
        await assertView('cancel-recording-from-toolbar');
    }

    await toolbarHelper.openSettings(bro);
    await assertView('open-settings');

    await toolbarHelper.closeSettings(bro);
    await assertView('close-settings');

    await toolbarHelper.cancelCall(bro);
    await assertView('cancel-call');
}

hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');
describe('Тулбар ->', () => {
    it('telemost-290: Тулбар на очень больших экранах (>1280px)', async function () {
        await run(this.browser, {
            assertPostfix: 'extra-large-screen',
            turnOffDevices: toolbarHelper.turnOffDevicesFromToolbar,
            startSharing: toolbarHelper.startSharingFromToolbar,
            cancelSharing: toolbarHelper.cancelSharingFromToolbar,
            openChat: toolbarHelper.openChatFromToolbarWithShowMore,
            startRecording: toolbarHelper.startRecordingWithTimer
        });
    });

    it('telemost-290: Тулбар на больших экранах (>919px)', async function () {
        const bro = this.browser;
        await bro.windowHandleSize({ width: 1280, height: 800 });
        await run(bro, {
            assertPostfix: 'large-screen',
            turnOffDevices: toolbarHelper.turnOffDevicesFromToolbar,
            startSharing: toolbarHelper.startSharingFromToolbar,
            cancelSharing: toolbarHelper.cancelSharingFromToolbar,
            openChat: toolbarHelper.openChatFromToolbarWithShowMore,
            startRecording: toolbarHelper.startRecordingWithTimer
        });
    });

    it('telemost-291: Тулбар на средних экранах (920px > x > 592px)', async function () {
        const bro = this.browser;
        await bro.windowHandleSize({ width: 919, height: 800 });
        await run(bro, {
            assertPostfix: 'medium-screen',
            turnOffDevices: toolbarHelper.turnOffDevicesFromToolbar,
            startSharing: toolbarHelper.startSharingFromToolbar,
            cancelSharing: toolbarHelper.cancelSharingFromToolbar,
            openChat: toolbarHelper.openChatFromToolbarWithShowMore,
            startRecording: toolbarHelper.startRecording
        });
    });

    it('telemost-292: Тулбар на маленьких экранах (593px > x > 559px)', async function () {
        const bro = this.browser;
        await bro.windowHandleSize({ width: 592, height: 800 });
        await run(bro, {
            assertPostfix: 'small-screen',
            turnOffDevices: toolbarHelper.turnOffDevicesFromToolbar,
            startSharing: toolbarHelper.startSharingFromToolbar,
            cancelSharing: toolbarHelper.cancelSharingFromToolbar,
            openChat: toolbarHelper.openChatFromPopupWithShowMore,
            startRecording: toolbarHelper.startRecording,
            skipToolbarRecording: true
        });
    });

    it('telemost-293: Тулбар на очень маленьких экранах (< 560px)', async function () {
        const bro = this.browser;
        await bro.windowHandleSize({ width: 559, height: 800 });
        await run(bro, {
            assertPostfix: 'extra-small-screen',
            turnOffDevices: toolbarHelper.turnOffDevicesFromPopup,
            startSharing: toolbarHelper.startSharingFromPopup,
            cancelSharing: toolbarHelper.cancelSharingFromPopup,
            openChat: toolbarHelper.openChatFromPopup,
            startRecording: toolbarHelper.startRecording,
            skipToolbarRecording: true,
            waitForIcon: false
        });
    });
});
