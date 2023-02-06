const bemPageObject = require('bem-page-object');
const Entity = bemPageObject.Entity;

const CommonObjects = {};

CommonObjects.submit = new Entity('[type="submit"]');

CommonObjects.createConferenceButton = new Entity('.Button2_size_xxxl.Button2_view_blur');
CommonObjects.loginButton = new Entity('.Button2_size_xxl.Button2_view_blur');

CommonObjects.tooltip = new Entity('.Tooltip');

CommonObjects.messageBox = new Entity('.MessageBox');
CommonObjects.messageBox.content = new Entity('.MessageBox-Content');

CommonObjects.xlButton = new Entity('.Button2_size_xl');
CommonObjects.xxlButton = new Entity('.Button2_size_xxl');

CommonObjects.continueConnectButton = new Entity('//button[contains(., "Подключиться")]');
CommonObjects.planCallButton = new Entity('//button[contains(., "Запланировать")]');

CommonObjects.createdConferenceMessageBox = new Entity('.MessageBox-Content*=Ссылка скопирована');
CommonObjects.createdConferenceMessageBoxTexts = new Entity('.MessageBox-Content > *');

CommonObjects.enterConferenceButton = new Entity('.Button2.Button2_size_xl.Button2_view_accent');

CommonObjects.exitFromConferenceButton = new Entity('button[title="Выйти из встречи"]');

CommonObjects.backgroundImage = new Entity('.BackgroundImage');

CommonObjects.ratingModal = new Entity('.Modal-Content');
CommonObjects.ratingModal.ratingButton3 = new Entity('button[title="rating_3"]');
CommonObjects.ratingModal.commentInput = new Entity('.Textarea-Control');
CommonObjects.ratingSendButton = new Entity('//div[contains(text(), "Отправить")]/../..');

CommonObjects.toolbar = new Entity('[class*=toolbar]');
CommonObjects.toolbar.addUsersButton = new Entity(
    'button[title="Пригласить на встречу и посмотреть список участников"]'
);
CommonObjects.toolbar.audioButtonOn = new Entity('button[title="Включить микрофон"]');
CommonObjects.toolbar.audioButtonOff = new Entity('button[title="Выключить микрофон"]');
CommonObjects.toolbar.videoButtonOn = new Entity('button[title="Включить камеру"]');
CommonObjects.toolbar.videoButtonOff = new Entity('button[title="Выключить камеру"]');
CommonObjects.toolbar.chatButton = new Entity('button[title="Открыть чат"]');
CommonObjects.toolbar.moreButton = new Entity('button[title="Ещё"]');
CommonObjects.toolbar.cancelCallButton = new Entity('button[title="Выйти из встречи"]');
CommonObjects.toolbar.cancelRecording = new Entity('button[title="Остановить запись"]');
CommonObjects.toolbar.cancelRecording.timer = new Entity('[class*=timer]');
CommonObjects.toolbar.sharingButton = new Entity('button[title="Начать демонстрацию экрана"]');
CommonObjects.toolbar.cancelSharingButton = new Entity('button[title="Закончить демонстрацию экрана"]');

CommonObjects.participantsPopup = new Entity('.Popup2.Popup2_visible:not(.Popup2_nonvisual)');
CommonObjects.participantsPopup.input = new Entity('.Textinput-Control');
CommonObjects.participantsPopup.icon = new Entity('[class*=ParticipantIcon]');

CommonObjects.showMorePopup = new Entity('.Popup2.Popup2_visible:not(.Popup2_nonvisual)');
CommonObjects.showMorePopup.presenterViewButton = new Entity('div[title="Вид докладчика"]');
CommonObjects.showMorePopup.galleryViewButton = new Entity('div[title="Вид галереи"]');
CommonObjects.showMorePopup.startRecording = new Entity('div[title="Включить запись"]');
CommonObjects.showMorePopup.cancelRecording = new Entity('div[title="Остановить запись"]');
CommonObjects.showMorePopup.settingsButton = new Entity('div[title="Открыть настройки"]');
CommonObjects.showMorePopup.chatButton = new Entity('div[title="Открыть чат"]');
CommonObjects.showMorePopup.audioButtonOff = new Entity('div[title="Выключить микрофон"]');
CommonObjects.showMorePopup.videoButtonOff = new Entity('div[title="Выключить камеру"]');
CommonObjects.showMorePopup.sharingButton = new Entity('div[title="Начать демонстрацию экрана"]');
CommonObjects.showMorePopup.cancelSharingButton = new Entity('div[title="Закончить демонстрацию экрана"]');

CommonObjects.chatWidget = new Entity('[class*=chatBlock]:nth-child(1)');
CommonObjects.chatWidget.closeButton = new Entity('span[class*=chatClose]');

CommonObjects.settings = new Entity('[title="Открыть настройки"]');
CommonObjects.settingsModal = new Entity('.Modal-Content');
CommonObjects.settingsModal.changeAvatarButton = new Entity('.СhangeAvatarButton');
CommonObjects.settingsModal.loginButton = new Entity('.Button2.Button2_size_m.Button2_view_accent');
CommonObjects.settingsModalAccountTab = new Entity('//span[contains(text(), "Аккаунт")]/..');
CommonObjects.settingsModalSupportTab = new Entity('//button[contains(., "Поддержка")]');
CommonObjects.settingsModal.supportLink = new Entity('a');
CommonObjects.settingsModal.closeButton = new Entity('.Button2.Button2_view_clear');
CommonObjects.settingsModal.changeAvatarButton = new Entity('[class*=СhangeAvatarButton]');
CommonObjects.settingsModal.title = new Entity('h1');
CommonObjects.settingsModal.menu = new Entity('[class*=Menu]');
CommonObjects.settingsModal.menu.audio = new Entity('button[data-test-id="sound"]');
CommonObjects.settingsModal.menu.video = new Entity('button[data-test-id="camera"]');
CommonObjects.settingsModal.menu.support = new Entity('button[data-test-id="support"]');
CommonObjects.settingsModal.micSelect = new Entity('[data-test-id="mic"] + .Select2 button');
CommonObjects.settingsModal.audioSelect = new Entity('[data-test-id="sound-output"] + .Select2 button');
CommonObjects.settingsModal.videoSelect = new Entity('.Select2 button');
CommonObjects.settingsModal.cameraVideo = new Entity('[class*=CameraVideo]');
CommonObjects.settingsModal.tumbler = new Entity('.Tumbler');

CommonObjects.showLimitationRecord = new Entity(
    '.Popup2.Popup2_visible.Popup2_nonvisual .MessageBox.js-record-limitation-popup'
);
CommonObjects.showLimitationRecord.closeButton = new Entity('button.Button2_view_clear');
CommonObjects.showLimitationRecord.downloadButton = new Entity('a.Button2_view_accent');

CommonObjects.select = new Entity('.Popup2.Popup2_visible.Select2-Popup');
CommonObjects.micSelectSecondOption = new Entity('//span[contains(text(), "Audio Input 1")]');
CommonObjects.micSelectThirdOption = new Entity('//span[contains(text(), "Audio Input 2")]');

CommonObjects.audioSelectSecondOption = new Entity('//span[contains(text(), "Audio Output 1")]');
CommonObjects.audioSelectThirdOption = new Entity('//span[contains(text(), "Audio Output 2")]');

CommonObjects.videoSelectFirstOption = new Entity('//span[@class="Button2-Text"][contains(text(), "device_0")]/..');

CommonObjects.videoOfParticipant = new Entity('div[class^="Participant__inner"]');
CommonObjects.videoOfParticipant.video = new Entity('video');
CommonObjects.videoOfParticipant.avatar = new Entity('[class*=avatarBlock]');

CommonObjects.participantsList = new Entity('div[class^="ParticipantsList__Inner"]');
CommonObjects.participantsList.group = new Entity('div[class^="ParticipantsList__ParticipantsGroup"]');
CommonObjects.participantsList.group.first = new Entity('div[class^="Participant"]:nth-child(1)');
CommonObjects.participantsList.group.first.nameElement = new Entity('div[class^="Participant__name"]');

CommonObjects.participantsGrid = new Entity('div[class^="container"] div[class^="root"] div[class^="container"]');

CommonObjects.presenterContainer = new Entity('div[class^="presenterContainer"]');

CommonObjects.psHeader = new Entity('.PSHeader');
CommonObjects.psHeader.login = new Entity('[class*=LoginButton]');
CommonObjects.psHeader.more = new Entity('.PSHeaderIcon-Image_More');
CommonObjects.psHeader.center = new Entity('.PSHeader-Center');
CommonObjects.psHeader.user = new Entity('.PSHeader-User');
CommonObjects.psHeader.user.unreadTicker = new Entity('.user-account__ticker');
CommonObjects.psHeader.services = new Entity('a.PSHeaderService');
CommonObjects.psHeaderMorePopup = new Entity('.PSHeader-MorePopup');
CommonObjects.psHeaderMorePopup.calendarIcon = new Entity('.PSHeaderIcon_Calendar');

CommonObjects.waitingToContinue = new Entity('[class*=waitContinueContainer]');
CommonObjects.waitingToContinue.button = new Entity('button');

CommonObjects.participant = new Entity('[class*=Participant]');
CommonObjects.participant.videoContainer = new Entity('[class*=videoContainer]');
CommonObjects.participant.input = new Entity('[class*=Textinput-Control]');
CommonObjects.participant.connectButton = new Entity('[class*=joinMeetingButton]');
CommonObjects.participant.placeholderImage = new Entity('[class*=PlaceholderImage]');
CommonObjects.participant.audioButtonOff = new Entity('button[title="Выключить микрофон"]');
CommonObjects.participant.audioButtonOn = new Entity('button[title="Включить микрофон"]');
CommonObjects.participant.videoButtonOff = new Entity('button[title="Выключить камеру"]');
CommonObjects.participant.videoButtonOn = new Entity('button[title="Включить камеру"]');

CommonObjects.overlay = new Entity('[class*=overlayContainer]');
CommonObjects.overlay.nameInput = new Entity('input');
CommonObjects.overlay.closeButton = new Entity('button[title="На главный экран"]');
CommonObjects.footer = new Entity('[class*=Footer]');
CommonObjects.footer.settings = new Entity('button[class*=ButtonSettings]');

// plan-call
CommonObjects.connectScreenInputWrapper = new Entity('[class*="meetingIdInputWrapper"]');
CommonObjects.connectScreenInputWrapperError = new Entity('[class*=hasError]');
CommonObjects.connectScreenInput = new Entity('[class*="meetingIdInputWrapper"] .Textinput-Control');
CommonObjects.connectScreenButton = new Entity('[class*="connectButton"]');
CommonObjects.closePopupButton = new Entity('[class*="popup"] [class*="closeButton"]');
CommonObjects.planningIframe = new Entity('iframe');

// moderation
CommonObjects.moderationControlsXpath = new Entity(
    '//div[contains(@class, "Participant__inner") and contains(., ":userName")]'
);

CommonObjects.moderationTurnOffAudioButton = new Entity(
    '//div[contains(@class, "Participant__inner") and contains(., ":userName")]'
);

CommonObjects.moderationControlsMuteAudioXpath = new Entity(
    [
        '//span[contains(@class, "Participant_textName") and contains(text(), ":userName")]/../',
        'button[@data-test-id="mute-audio"]'
    ].join('')
);

CommonObjects.moderationControlsMuteVideo = new Entity('[class*=ModerationPopup] [title="Выключить камеру"]');

CommonObjects.moderationControlsMakeAnModerator = new Entity(
    '[class*=ModerationPopup] [title="Назначить соорганизатором"]'
);

CommonObjects.moderationControlsShowPopupButtonXpath = new Entity(
    [
        '//span[contains(@class, "Participant_textName") and contains(text(), ":userName")]/../',
        'button[@data-test-id="show-moderation-popup"]'
    ].join('')
);
CommonObjects.participantInUserList = new Entity('.Popup2 [class*=Participant]');

CommonObjects.moderationControlsShowPopupButtonInUserList = new Entity(
    '.Popup2 [class*=ModerationItems] [title="Ещё"]'
);

CommonObjects.avatarInMessageBox = new Entity('[class*=MessageBox] [class*=MessageBox-Content] [class*=avatar] ');

CommonObjects.avatarsInUsersList = new Entity('[class*=Popup2] [class*=ParticipantData] [class*=avatar] ');

CommonObjects.linkInUsersList = new Entity('[class*=Popup2] [class*=CopyLinkActions] .Textinput-Control ');

module.exports = {
    common: bemPageObject.create(CommonObjects)
};
