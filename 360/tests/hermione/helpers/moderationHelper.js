const clientObjects = require('../page-objects/client');
const ConfMembersManager = require('./confMembersManager');
const { assertCountOfMembers } = require('./conferenceHelper');
const { connectToConference } = require('./joinToConferenceHelper');

module.exports = {
    async initConference(bro) {
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-0');
        await confManager.addAnonymousUserToConference('Anon1');

        await bro.refresh();
        await bro.yaWaitForVisible(clientObjects.common.participant.connectButton());
        await connectToConference(bro);
        await assertCountOfMembers(bro, 2);
    },

    participantSelector(userName) {
        return clientObjects.common.moderationControlsXpath().replace(/:userName/, userName);
    },

    showPopupButtonSelector(userName) {
        return clientObjects.common.moderationControlsShowPopupButtonXpath().replace(/:userName/, userName);
    },

    muteAudioSelector(userName) {
        return clientObjects.common.moderationControlsMuteAudioXpath().replace(/:userName/, userName);
    },

    async assertView(bro, assertName) {
        await bro.yaAssertView(assertName, 'body', {
            invisibleElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar(),
                clientObjects.common.avatarInMessageBox(),
                clientObjects.common.linkInUsersList(),
                clientObjects.common.avatarsInUsersList()
            ],
            customCSS: '[class*=Participant__inner]:after{ content: initial!important }'
        });
    }
};
