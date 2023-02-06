const clientObjects = require('../page-objects/client');
const { assertCountOfMembers } = require('./conferenceHelper');

// класс предназначен для управления конференцией: добавление и переключение между пользователями
module.exports = class ConfMembersManager {
    constructor(bro) {
        this._bro = bro;
        this._users = [];
    }

    async createConferenceByUser(username) {
        const bro = this._bro;
        await bro.yaLoginFast(username);
        await bro.yaWaitForVisible(clientObjects.common.createConferenceButton());
        await bro.click(clientObjects.common.createConferenceButton());

        await bro.yaWaitForVisible(clientObjects.common.createdConferenceMessageBox(), 10000);
        let text = await bro.getText(clientObjects.common.createdConferenceMessageBox());
        text = text
            .split('\n')
            .join(' ')
            .split(' ')
            .filter((x) => x.startsWith('https://telemost.'));
        this._link = text[0].split('/j/')[1];
        const cookies = await bro.getCookie();
        const tabId = await bro.getCurrentTabId();
        this._users.push({ username, cookies, tabId });
        this._currentUser = username;
    }

    getCurrentPage() {
        return this._users.find((x) => x.username === this._currentUser).tabId;
    }

    async addAnonymousUserToConference(tag, options) {
        options = options || {};
        const bro = this._bro;
        await bro.deleteCookie();

        const urlParams = options.urlParams ? `?${options.urlParams}` : '';
        await bro.newWindow(`/j/${this._link}${urlParams}`);

        if (options.displayName) {
            await bro.yaWaitForVisible(clientObjects.common.overlay.nameInput());
            await bro.yaSetValue(clientObjects.common.overlay.nameInput(), options.displayName);
        }

        const cookies = await bro.getCookie();
        const tabId = await bro.getCurrentTabId();
        this._users.push({ tag, cookies, tabId });
        this._currentUser = tag;
    }

    async addUserToConference(username) {
        const bro = this._bro;
        await bro.deleteCookie();
        await bro.newWindow('/');
        await bro.yaLoginFast(username);
        await bro.url(`/j/${this._link}`);

        await bro.yaWaitForVisible(clientObjects.common.participant.videoContainer());
        await bro.click(clientObjects.common.enterConferenceButton());
        await bro.pause(1000);
        await bro.yaWaitForVisible(clientObjects.common.participant.videoContainer());

        const cookies = await bro.getCookie();
        const tabId = await bro.getCurrentTabId();
        this._users.push({ username, cookies, tabId });
        this._currentUser = username;
    }

    async proceedToConference() {
        const bro = this._bro;

        await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
        await bro.click(clientObjects.common.enterConferenceButton());

        await assertCountOfMembers(bro, 2);
    }

    async switchUser(username) {
        const bro = this._bro;
        const userData = this._users.find((x) => x.username === username || x.tag === username);

        await bro.switchTab(userData.tabId);
        await bro.deleteCookie();

        for (const cookie of userData.cookies) {
            await bro.setCookie(cookie);
        }

        this._currentUser = username;
    }
};
