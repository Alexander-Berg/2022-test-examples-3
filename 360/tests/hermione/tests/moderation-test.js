const {
    assertView,
    participantSelector,
    showPopupButtonSelector,
    muteAudioSelector
} = require('../helpers/moderationHelper');
const clientObjects = require('../page-objects/client');
const ConfMembersManager = require('../helpers/confMembersManager');

const admin = 'yndx-telemost-test-0';
const member = 'yndx-telemost-test-1';

hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');
describe('Модерация ->', () => {
    it('telemost-321: Отображение попапа модерации для админа и участника в сетке', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser(admin);
        await confManager.addUserToConference(member);
        await confManager.switchUser(admin);
        await bro.refresh();
        await confManager.proceedToConference();
        await bro.yaWaitForVisible(participantSelector('Def-Имя'));
        await bro.moveToObject(participantSelector('Def-Имя'));
        await bro.yaWaitForVisible(participantSelector('Def-Имя'));
        await bro.pause(300);
        await bro.click(showPopupButtonSelector('Def-Имя'));

        await bro.yaResetPointerPosition();
        await bro.pause(200);
        await assertView(bro, 'telemost-321-open-own-popup');

        await bro.moveToObject(participantSelector(member));
        await bro.click(showPopupButtonSelector(member));

        await bro.yaResetPointerPosition();
        await bro.pause(200);
        await assertView(bro, 'telemost-321-open-user-popup');
    });

    it('telemost-322: Мьют микрофона администратором у участника', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser(admin);
        await confManager.addUserToConference(member);
        await confManager.switchUser(admin);
        await bro.refresh();
        await confManager.proceedToConference();
        await bro.yaWaitForVisible(participantSelector(member));
        await bro.moveToObject(participantSelector(member));

        await bro.click(muteAudioSelector(member));
        await confManager.switchUser(member);
        await bro.yaResetPointerPosition();
        await bro.pause(200);
        await bro.yaWaitForVisible(clientObjects.common.messageBox());
        await assertView(bro, 'telemost-322');
    });

    it('telemost-323: Мьют видео администратором у участника', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser(admin);
        await confManager.addUserToConference(member);
        await confManager.switchUser(admin);
        await bro.refresh();
        await confManager.proceedToConference();
        await bro.yaWaitForVisible(participantSelector(member));
        await bro.moveToObject(participantSelector(member));
        await bro.yaWaitForVisible(showPopupButtonSelector(member));
        await bro.click(showPopupButtonSelector(member));

        await bro.yaWaitForVisible(clientObjects.common.moderationControlsMuteVideo());
        await bro.pause(200);
        await bro.click(clientObjects.common.moderationControlsMuteVideo());
        await confManager.switchUser(member);
        await bro.yaResetPointerPosition();
        await bro.pause(200);
        await bro.yaWaitForVisible(clientObjects.common.messageBox());
        await assertView(bro, 'telemost-323');
    });

    it('telemost-324: Назначение участника администратором', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser(admin);
        await confManager.addUserToConference(member);
        await confManager.switchUser(admin);
        await bro.refresh();
        await confManager.proceedToConference();
        await bro.yaWaitForVisible(participantSelector(member));
        await bro.moveToObject(participantSelector(member));
        await bro.click(showPopupButtonSelector(member));

        await bro.yaWaitForVisible(clientObjects.common.moderationControlsMakeAnModerator());
        await bro.click(clientObjects.common.moderationControlsMakeAnModerator());
        await bro.yaWaitForVisible(clientObjects.common.messageBox());
        await bro.yaResetPointerPosition();
        await bro.pause(500);
        await assertView(bro, 'telemost-324-i-appointed-new-moderator');
        await confManager.switchUser(member);
        await bro.yaWaitForVisible(clientObjects.common.messageBox());
        await bro.yaResetPointerPosition();
        await bro.pause(500);
        await assertView(bro, 'telemost-324-i-was-appointed-new-moderator');
    });

    it('telemost-325: Открытие попапа администратора в списке участников', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser(admin);
        await confManager.addUserToConference(member);
        await confManager.switchUser(admin);
        await bro.refresh();
        await confManager.proceedToConference();
        await bro.yaWaitForVisible(participantSelector(member));
        await bro.click(clientObjects.common.toolbar.addUsersButton());
        await bro.moveToObject(clientObjects.common.participantInUserList());
        await bro.click(clientObjects.common.moderationControlsShowPopupButtonInUserList());

        await assertView(bro, 'telemost-325');
    });
});
