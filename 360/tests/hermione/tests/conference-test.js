const DevTools = require('../helpers/devTools');
const clientObjects = require('../page-objects/client');
const ConfMembersManager = require('../helpers/confMembersManager');
const { openSettingsFromAnywhere } = require('../helpers/toolbarHelper');
const toolbarHelper = require('../helpers/toolbarHelper');
const { assertCountOfMembers } = require('../helpers/conferenceHelper');
const assert = require('chai').assert;

hermione.skip.in('firefox-desktop', 'Не адаптировано под Firefox');
describe('Конференции ->', () => {
    it('telemost-123: Два участника в конференции', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser('yndx-telemost-test-0');
        await confManager.addUserToConference('yndx-telemost-test-1');
        await confManager.switchUser('yndx-telemost-test-0');

        await assertCountOfMembers(bro, 2);
    });

    it('telemost-34: Подключение третьего человека', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);

        await confManager.createConferenceByUser('yndx-telemost-test-2');
        await confManager.addUserToConference('yndx-telemost-test-3');
        await confManager.addUserToConference('yndx-telemost-test-4');
        await confManager.switchUser('yndx-telemost-test-2');

        await assertCountOfMembers(bro, 3);
    });

    it('telemost-1: Создание конференции Яндексоидом', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-5');
    });

    it('telemost-3: Создание встречи для всех', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-6');
        await confManager.addUserToConference('yndx-telemost-test-7');
        await confManager.addUserToConference('yndx-telemost-test-10');

        await confManager.switchUser('yndx-telemost-test-6');
        await assertCountOfMembers(bro, 3);
    });

    it('telemost-4: Переход во встречу неавторизованным (конфа для всех)', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-8');
        await confManager.addAnonymousUserToConference('Anon1');
        await bro.yaWaitForVisible(clientObjects.common.xlButton());
        await bro.yaWaitForVisible(clientObjects.common.continueConnectButton());
        await bro.yaWaitForVisible('input[value="Гость"]');
        await bro.click(clientObjects.common.continueConnectButton());
        await assertCountOfMembers(bro, 2);
    });

    it('telemost-5: Оценка встречи', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-9');
        await bro.yaWaitForVisible(clientObjects.common.exitFromConferenceButton());
        await bro.click(clientObjects.common.exitFromConferenceButton());
        await bro.yaWaitForVisible(clientObjects.common.ratingModal());
        await bro.pause(1000);

        // Далаем скрин модалки без hover
        await bro.yaAssertView('telemost-5-without-hover', clientObjects.common.ratingModal(), {
            hideElements: [
                clientObjects.common.backgroundImage(),
                clientObjects.common.psHeader(),
                clientObjects.common.createConferenceButton()
            ],
            withHover: true
        });

        // Делаем скрин с hover над зведочкой
        await bro.moveToObject(clientObjects.common.ratingModal.ratingButton3());
        await bro.yaAssertView('telemost-5-hover', clientObjects.common.ratingModal(), {
            hideElements: [
                clientObjects.common.backgroundImage(),
                clientObjects.common.psHeader(),
                clientObjects.common.createConferenceButton()
            ],
            withHover: true
        });

        // Кликаем на звездочку и переходим к расширенной форме
        await bro.click(clientObjects.common.ratingModal.ratingButton3());

        await bro.yaAssertView('telemost-5', clientObjects.common.ratingModal(), {
            hideElements: [
                clientObjects.common.backgroundImage(),
                clientObjects.common.psHeader(),
                clientObjects.common.createConferenceButton()
            ],
            withHover: true
        });
        await bro.addValue(clientObjects.common.ratingModal.commentInput(), 'Autotests comment. Ignore.');
        await bro.click(clientObjects.common.ratingSendButton());
        await bro.yaWaitForHidden(clientObjects.common.ratingModal());

        await bro.yaAssertView('telemost-5-notificaton', clientObjects.common.messageBox(), {
            hideElements: [
                clientObjects.common.backgroundImage(),
                clientObjects.common.psHeader(),
                clientObjects.common.createConferenceButton()
            ],
            withHover: true
        });
    });

    it('telemost-10: Экран ожидания участников', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-11');
        await bro.yaWaitForVisible(
            clientObjects.common.createdConferenceMessageBox(),
            'Created Conference message box is not visible',
            5000
        );

        await confManager.addUserToConference('yndx-telemost-test-12');
        await confManager.switchUser('yndx-telemost-test-11');
        await bro.yaWaitForHidden(clientObjects.common.createdConferenceMessageBox());
    });

    it('telemost-14: Сброс звонка организатором', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-13');
        await confManager.addUserToConference('yndx-telemost-test-14');
        await confManager.switchUser('yndx-telemost-test-13');
        await bro.click(clientObjects.common.exitFromConferenceButton());
        await confManager.switchUser('yndx-telemost-test-14');
        await assertCountOfMembers(bro, 1);
    });

    it('telemost-15: Закрытие вкладки организатором', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-15');
        await confManager.addUserToConference('yndx-telemost-test-16');
        await confManager.switchUser('yndx-telemost-test-15');
        await bro.url('https://yandex.ru');
        await confManager.switchUser('yndx-telemost-test-16');
        await assertCountOfMembers(bro, 1);
    });

    it('telemost-18: Невалидная ссылка конфы', async function () {
        const bro = this.browser;
        await bro.yaLoginFast('yndx-telemost-test-17');
        await bro.setCookie({ name: 'background', value: '1' });
        await bro.url('/j/0000000000');
        await bro.yaWaitForVisible(clientObjects.common.xxlButton());
        await bro.yaAssertView('telemost-18-invalid-conf', 'body', {
            ignoreElements: [clientObjects.common.psHeader.user.unreadTicker()],
            withHover: true
        });

        await bro.click(clientObjects.common.xxlButton());
        await bro.yaWaitForVisible(clientObjects.common.createConferenceButton());
    });

    it('telemost-32: Настройки. Помощь', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-20');

        await openSettingsFromAnywhere(bro);

        await bro.click(clientObjects.common.settingsModalSupportTab());
        await bro.pause(1000);
        await bro.yaWaitForVisible(clientObjects.common.settingsModal.supportLink());
        await bro.yaClickAndAssertNewTabUrl(clientObjects.common.settingsModal.supportLink(), {
            linkShouldContain: 'https://yandex.ru/support/telemost/'
        });
    });

    // видео- и аудио-потоки не прерываются: при отключении сети через девтулзы
    // нотифайка "Нет интернета" будет, а видео/звук продолжат идти
    it('telemost-33: Потеря связи.', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-20');
        await confManager.addUserToConference('yndx-telemost-test-21');
        await confManager.switchUser('yndx-telemost-test-20');
        // Ждем, пока скроется нотифайка о создании конференции
        await bro.yaWaitForHidden(clientObjects.common.messageBox(), 10000);

        const devTools = await DevTools.create(bro);
        await devTools.disableNetworkOnPage(confManager.getCurrentPage());

        await bro.yaWaitForVisible(clientObjects.common.messageBox(), 20000);
        const messageText = await bro.getText(clientObjects.common.messageBox());
        assert(messageText === 'Нет интернета');
        await devTools.enableNetworkOnPage(confManager.getCurrentPage());
        await assertCountOfMembers(bro, 2);
    });

    it('telemost-35: Рефреш страницы конфы', async function () {
        const bro = this.browser;
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-22');
        await confManager.addUserToConference('yndx-telemost-test-23');

        await bro.refresh();
        await bro.yaWaitForVisible('video');
        await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
        await bro.click(clientObjects.common.enterConferenceButton());
        await assertCountOfMembers(bro, 2);
    });

    it('telemost-presenter-mode: Страница в режиме докладчика', async function () {
        const bro = this.browser;

        // Создаем конфу и выходим из нее сразу же
        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-11');
        await bro.deleteCookie();

        const params = 'camera=off&mic=off';

        const url = await bro.getUrl();
        await bro.url(`${url}?${params}`);
        const firstUserTab = await bro.getCurrentTabId();

        // Заходим в ту же конфу режимом гостя
        await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
        await bro.click(clientObjects.common.enterConferenceButton());
        await assertCountOfMembers(bro, 1);

        // Заходим другим гостем с длинным именем
        await confManager.addAnonymousUserToConference('test1', {
            displayName: 'Константин Константинович Константинопольский',
            urlParams: params
        });
        await bro.yaWaitForVisible(clientObjects.common.enterConferenceButton());
        await bro.click(clientObjects.common.enterConferenceButton());
        await assertCountOfMembers(bro, 2);

        // Возвращаемся к первому пользователю
        await bro.switchTab(firstUserTab);
        // Ждем, когда станет два участника
        await assertCountOfMembers(bro, 2);

        // Переходим в режим докладчика
        await bro.click(clientObjects.common.toolbar.moreButton());
        await bro.yaWaitForVisible(clientObjects.common.showMorePopup.presenterViewButton());
        await bro.click(clientObjects.common.showMorePopup.presenterViewButton());

        await bro.yaAssertView('telemost-presenter-mode', 'body', {
            ignoreElements: [clientObjects.common.participant.placeholderImage()]
        });
    });

    it('telemost-301, 303, 304: Отображение экрана встречи в режиме докладчика', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-22');
        await confManager.addAnonymousUserToConference('anon-telemost-301');

        const currentUrl = await bro.url();
        await bro.url(`${currentUrl.value}?test-id=374483`);

        await confManager.proceedToConference();
        await bro.yaResetPointerPosition();
        await bro.pause(1000); // for grid animation

        await bro.yaAssertView('telemost-301-list-grid', clientObjects.common.participantsGrid(), {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });

        await toolbarHelper.openShowMorePopup(bro);
        await toolbarHelper.setPresenterView(bro);

        await bro.yaAssertView('telemost-301-list-and-presenter', 'body', {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });
        await bro.moveToObject(clientObjects.common.participantsList.group.first());

        await bro.yaWaitUntil(
            'No participant name',
            async () => {
                const element = await bro.getCssProperty(
                    clientObjects.common.participantsList.group.first.nameElement(),
                    'opacity'
                );
                return element[0].value === 1;
            },
            5000,
            500
        );

        await bro.yaAssertView('telemost-301-list-hover', clientObjects.common.participantsList(), {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });
    });

    it('telemost-302: Отображение имен участников в активном шаринге', async function () {
        const bro = this.browser;

        const confManager = new ConfMembersManager(bro);
        await confManager.createConferenceByUser('yndx-telemost-test-22');
        await confManager.addAnonymousUserToConference('anon-telemost-302');

        const currentUrl = await bro.url();
        await bro.url(`${currentUrl.value}?test-id=374483`);

        await confManager.proceedToConference();
        await bro.yaResetPointerPosition();
        await bro.pause(1000); // for grid animation

        // во встрече находятся 2 и более участника
        // минимум у одного участника выбран тип отображения Вид галереи
        await bro.yaAssertView('telemost-302-list-grid', clientObjects.common.participantsGrid(), {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });

        // участник №1 шарит экран
        await confManager.switchUser('yndx-telemost-test-22');
        await toolbarHelper.startSharingFromToolbar(bro);
        await confManager.switchUser('anon-telemost-302');
        await bro.pause(1000);
        // в ленте участников сверху не отображаются имена и микрофоны участников
        await bro.yaAssertView('telemost-302-list-and-presenter', 'body', {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });
        await bro.moveToObject(clientObjects.common.participantsList.group.first());

        await bro.yaWaitUntil(
            'No participant name',
            async () => {
                const element = await bro.getCssProperty(
                    clientObjects.common.participantsList.group.first.nameElement(),
                    'opacity'
                );
                return element[0].value === 1;
            },
            5000,
            500
        );

        await bro.yaAssertView('telemost-302-list-hover', clientObjects.common.participantsList(), {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });

        // Участником №1 завершить шаринг экрана
        await confManager.switchUser('yndx-telemost-test-22');
        await toolbarHelper.cancelSharingFromToolbar(bro);
        await confManager.switchUser('anon-telemost-302');
        await bro.pause(1000); // for grid animation

        // всем пользователям вернулся тип отображения сетки, который был выбран у них ранее
        // для типа отображения Вид галереи - у всех пользователей есть имена;
        await bro.yaAssertView('telemost-302-list-grid-after', clientObjects.common.participantsGrid(), {
            hideElements: [
                clientObjects.common.videoOfParticipant.video(),
                clientObjects.common.videoOfParticipant.avatar()
            ],
            withHover: true
        });
    });
});
