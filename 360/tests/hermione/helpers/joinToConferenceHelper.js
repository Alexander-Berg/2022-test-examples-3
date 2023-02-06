const assert = require('chai').assert;
const clientObjects = require('../page-objects/client');
const { assertCountOfMembers } = require('./conferenceHelper');
const ConfMembersManager = require('./confMembersManager');

/**
 * @param {Object} bro
 * @param {Object} options
 */
async function init(bro, { anonymous }) {
    const confManager = new ConfMembersManager(bro);

    await confManager.createConferenceByUser('yndx-telemost-test-0');

    if (anonymous) {
        await confManager.addAnonymousUserToConference('Anon1');
    } else {
        await confManager.addUserToConference('yndx-telemost-test-1');
    }

    const url = await bro.getUrl();

    // Очищаем все старые локальные записи, удаляя indexedDB
    await bro.executeAsync((done) => {
        window.indexedDB
            .databases()
            .then((dbs) => {
                for (let i = 0; i < dbs.length; i++) {
                    window.indexedDB.deleteDatabase(dbs[i].name);
                }
            })
            .then(done);
    });

    await bro.setCookie({ name: 'background', value: '1' });
    await bro.setCookie({ name: 'agreementShown', value: '1' });
    await bro.url(`${url}?test-id=0`);
}

module.exports = {
    async openConnectionScreen(bro, { anonymous = false } = {}) {
        await init(bro, { anonymous });
        await bro.yaWaitForVisible(clientObjects.common.participant.videoContainer());
        if (bro.desiredCapabilities.browserName !== 'firefox') {
            await bro.yaWaitForVisible(clientObjects.common.toolbar.videoButtonOff());
        }

        if (anonymous) {
            await bro.yaWaitForVisible(clientObjects.common.participant.input());
            assert(await bro.hasFocus(clientObjects.common.participant.input()), 'Нет фокуса у инпута');

            // Убрать фокус с инпута, чтобы скрин всегда был без курсора,
            // иначе могут быть конфликты
            await bro.click(clientObjects.common.overlay());
        }
    },

    async openConnectionScreenWin(bro, { anonymous = false } = {}) {
        await init(bro, { anonymous });
        await bro.yaWaitForVisible(clientObjects.common.waitingToContinue.button());
    },

    async openSettings(bro) {
        await bro.yaWaitForVisible(clientObjects.common.settings());
        await bro.click(clientObjects.common.settings());

        await bro.yaWaitForVisible(clientObjects.common.settingsModal());
        await bro.pause(1000);
    },

    async turnOffDevices(bro) {
        await bro.click(clientObjects.common.participant.audioButtonOff());
        await bro.yaWaitForVisible(clientObjects.common.participant.audioButtonOn());
        await bro.click(clientObjects.common.participant.videoButtonOff());
        await bro.yaWaitForVisible(clientObjects.common.participant.videoButtonOn());
    },

    async turnOnDevices(bro) {
        await bro.click(clientObjects.common.participant.audioButtonOn());
        await bro.yaWaitForVisible(clientObjects.common.participant.audioButtonOff());
        await bro.click(clientObjects.common.participant.videoButtonOn());
        await bro.yaWaitForVisible(clientObjects.common.participant.videoButtonOff());
    },

    async connectToConference(bro) {
        await bro.click(clientObjects.common.participant.connectButton());
        await assertCountOfMembers(bro, 2);
        // Анимации
        await bro.pause(1000);
    },

    async login(bro, loginButton) {
        await bro.yaWaitForVisible(loginButton);
        await bro.click(loginButton);

        await bro.waitUntil(
            () => bro.getUrl().then((url) => url.startsWith('https://passport.yandex.ru/auth/welcome')),
            5000,
            'Не произошёл переход на страницу входа'
        );
    },

    async assertView(bro, assertName, hideElements = []) {
        await bro.yaAssertView(assertName, 'body', {
            hideElements: [
                clientObjects.common.psHeader.user.unreadTicker(),
                clientObjects.common.psHeader.services(),
                clientObjects.common.participant.videoContainer(),
                clientObjects.common.participant.placeholderImage(),
                ...hideElements
            ]
        });
    }
};
