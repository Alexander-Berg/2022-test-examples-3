const clientObjects = require('../page-objects/client');
const toolbarHelper = require('../helpers/toolbarHelper');
const ConfMembersManager = require('../helpers/confMembersManager');

const hideElements = [
    clientObjects.common.backgroundImage(),
    clientObjects.common.videoOfParticipant(),
    clientObjects.common.psHeader(),
    // clientObjects.common.messageBox(),
    clientObjects.common.participantsPopup.input(),
    clientObjects.common.participantsPopup.icon(),
    clientObjects.common.toolbar.cancelRecording.timer()
];

const limitationRecordTooltip = clientObjects.common.showLimitationRecord();

/**
 * @param {Object} bro
 * @param {string} assertName
 * @param {Object} selector
 * @returns {Promise}
 */
function assertView(bro, assertName, selector) {
    return bro.yaAssertView(['telemost-record', assertName].join('-'), selector || 'body', { hideElements });
}

/**
 * @param {Object} bro
 * @param {Object} options
 * @param {string} options.search
 */
async function run(bro, { search = '' } = {}) {
    const confManager = new ConfMembersManager(bro);
    await confManager.createConferenceByUser('yndx-telemost-test-9');
    await bro.deleteCookie();

    const url = await bro.getUrl();

    await bro.url(url + '?disable_datasync=1' + search);
    await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
    await bro.click(clientObjects.common.enterConferenceButton());
    await bro.yaWaitForVisible(clientObjects.common.toolbar());

    await bro.yaWaitForVisible(clientObjects.common.toolbar.videoButtonOff());
}

/**
 * @param {Object} bro
 * @param {boolean} isFirst
 */
async function record(bro, isFirst = false) {
    await toolbarHelper.openShowMorePopup(bro);
    await toolbarHelper.startRecordingWithTimer(bro);

    if (isFirst) {
        await toolbarHelper.waitShowLimitationRecord(bro);
    }
}

hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');

describe('Запись ->', () => {
    it('telemost-361: Показ тултипа при включении записи', async function () {
        const bro = this.browser;
        await run(bro);
        await record(bro, true);
        await assertView(bro, 'start-record', limitationRecordTooltip);
        await toolbarHelper.cancelRecordingFromToolbar(bro);
        await bro.pause(5000);
        await record(bro);
        await assertView(bro, 'restart-record');
    });

    it('telemost-362: Скачивание приложения ПО по кнопке "Установить программу" из тултипа', async function () {
        const bro = this.browser;
        await run(bro);
        await record(bro, true);
        await assertView(bro, 'start-record', limitationRecordTooltip);
        await toolbarHelper.downloadApp(bro);
        await assertView(bro, 'download');
        await toolbarHelper.cancelRecordingFromToolbar(bro);
        await bro.pause(5000);
        await record(bro);
        await assertView(bro, 'restart-record');
    });

    it('telemost-364: Закрытие тултипа ограничения записи по крестику', async function () {
        const bro = this.browser;
        await run(bro);
        await record(bro, true);
        await assertView(bro, 'start-record', limitationRecordTooltip);
        await toolbarHelper.closeLimitationRecord(bro);

        await assertView(bro, 'close-tooltip');
        await toolbarHelper.cancelRecordingFromToolbar(bro);
        await bro.pause(5000);
        await record(bro);
        await assertView(bro, 'restart-record');
        await toolbarHelper.cancelCall(bro);
        await run(bro);
        await record(bro, true);
        await assertView(bro, 'start-record-second-session');
    });

    it('telemost-365: Автоматическая остановка записи через 30 минут', async function () {
        const bro = this.browser;
        await run(bro, { search: '&hermione_limit_record=5' });
        await record(bro, true);
        await assertView(bro, 'start-record', limitationRecordTooltip);

        await bro.pause(6000);
        await assertView(bro, 'timeout');
    });

    it('telemost-366: Тултип ограничения записи по крестику не закрывается при открытии других попапов, меню и нажатии на кнопки в тулбаре.', async function () {
        const bro = this.browser;
        await run(bro);
        await record(bro, true);
        await assertView(bro, 'start-record', limitationRecordTooltip);

        await toolbarHelper.openParticipantsPopup(bro);
        await assertView(bro, 'open-participants-popup', [
            limitationRecordTooltip,
            clientObjects.common.participantsPopup()
        ]);

        await toolbarHelper.startSharingFromToolbar(bro);
        await assertView(bro, 'start-sharing', [
            limitationRecordTooltip,
            clientObjects.common.toolbar.cancelSharingButton()
        ]);

        await toolbarHelper.cancelSharingFromToolbar(bro);
        await assertView(bro, 'cancel-sharing', [
            limitationRecordTooltip,
            clientObjects.common.toolbar.sharingButton()
        ]);

        await bro.click(clientObjects.common.toolbar.chatButton());
        await bro.yaWaitForVisible(clientObjects.common.chatWidget());
        await assertView(bro, 'open-chat', [limitationRecordTooltip, clientObjects.common.chatWidget()]);

        await toolbarHelper.closeChat(bro);
        await assertView(bro, 'close-chat', limitationRecordTooltip);
        await toolbarHelper.openShowMorePopup(bro);
        await assertView(bro, 'open-show-more', [
            clientObjects.common.toolbar.chatButton(),
            clientObjects.common.showMorePopup()
        ]);
    });
});
