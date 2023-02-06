const { NAVIGATION } = require('../config').consts;
const { overdraftBlock, overdraftContent } = require('../page-objects/client');
const clientNavigation = require('../page-objects/client-navigation');
const fileListing = require('../page-objects/client-content-listing').common;
const popups = require('../page-objects/client-popups');
const albums = require('../page-objects/client-albums-page');
const slider = require('../page-objects/slider').common;
const consts = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const versions = require('../page-objects/versions-dialog-modal');

const TEST_FILE_NAME = 'TestFile.jpg';
const TEST_DOC_NAME = 'TestDocument.docx';
const TEST_AUDIO_NAME = 'TestAudio.mp3';
const TEST_VIDEO_NAME = 'TestVideo.mp4';
const PUBLIC_TEST_FILE_NAME = 'PublicTestFile.jpg';
const TEST_FOLDER_NAME = 'TestFolder';
const PUBLIC_TEST_FOLDER_NAME = 'PublicTestFolder';
const SHARED_TEST_FOLDER_NAME = 'SharedTestFolder';

/**
 * Скрытие овердрафт-экрана или овердрафт-диалога
 *
 */
async function closeOverdraftContent() {
    const bro = this.browser;
    await bro.click(overdraftContent.closeButton());
    await bro.yaWaitForHidden(overdraftContent());
    await bro.yaSkipWelcomePopup();
}

/**
 * Проверка блокировки действия - показ диалога-овердрафтника на десктопе или экрана на тачах
 *
 * @param {boolean} [isMobile]
 * @returns {Promise<void>}
 */
async function checkBlockAction(isMobile = false) {
    const bro = this.browser;
    await bro.yaWaitForVisible(isMobile ? overdraftContent() : popups.desktop.overdraftDialog());
}

/**
 * Открыть ресурс - даблкликом на десктопе, кликом на тачах
 *
 * @param {string} name
 * @param {boolean} isMobile
 * @returns {Promise<void>}
 */
async function openResourceByName(name, isMobile) {
    const bro = this.browser;
    const selector = fileListing.listingBodyItemsInfoXpath().replace(/:titleText/g, name);
    await bro.yaWaitForVisible(selector);

    if (isMobile) {
        await bro.click(selector);
    } else {
        await bro.doubleClick(selector);
    }
}

/**
 * Выделить ресурс и выполнить действие
 *
 * @param {string} resourceName
 * @param {string} actionName
 * @returns {Promise<void>}
 */
async function selectResourceAndDoAction(resourceName, actionName) {
    const bro = this.browser;
    await bro.yaSelectResource(resourceName);
    await bro.yaCallActionInActionBar(actionName);
}

/**
 * Действие с альбомом на странице альбома
 *
 * @param {string} actionName
 * @param {boolean} isInDropdown
 * @returns {Promise<void>}
 */
async function albumActionOnPageAlbum(actionName, isInDropdown = false) {
    const bro = this.browser;

    if (isInDropdown) {
        await bro.yaWaitForVisible(albums.album2.actionsMoreButton());
        await bro.click(albums.album2.actionsMoreButton());
        await bro.yaWaitForVisible(albums.album2ActionsDropdown());
        await bro.pause(500); // Drawer animation
        await bro.click(albums.album2ActionsDropdown[actionName + 'Button']());
    } else {
        const button = albums.album2[actionName + 'Button']();
        await bro.yaWaitForVisible(button);
        await bro.click(button);
    }
}

/**
 * Клик по кнопке публикации для ресурса
 *
 * @param {string} resourceName
 * @param {boolean} isMobile
 * @returns {Promise<void>}
 */
async function clickPublishResource(resourceName, isMobile) {
    const bro = this.browser;

    await bro.yaSelectResource(resourceName);
    await bro.yaWaitActionBarDisplayed();

    if (isMobile) {
        await bro.yaWaitForVisible(popups.touch.actionBar.publishButton());
        await bro.click(popups.touch.actionBar.publishButton());
    } else {
        await bro.yaWaitForVisible(popups.desktop.actionBar.publishButton());
        await bro.click(popups.desktop.actionBar.publishButton());
    }
}

/**
 * Клик по кнопке Загрузить
 *
 * @param {boolean} isMobile
 * @returns {Promise<void>}
 */
async function clickUploadButton(isMobile) {
    const bro = this.browser;

    if (isMobile) {
        await bro.yaWaitForVisible(clientNavigation.touch.touchListingSettings.plus());
        await bro.click(clientNavigation.touch.touchListingSettings.plus());
        await bro.yaWaitForVisible(popups.touch.createPopup.uploadFile());
        await bro.pause(500); // Drawer animation
        await bro.click(popups.touch.createPopup.uploadFile());
    } else {
        await bro.click(clientNavigation.desktop.sidebarButtons.upload());
    }
}

// Дата попадания в овердрафт для пользователей теста - обновляется в соответствующем gc-таске

describe('Овердрафт -> ', () => {
    describe('Новые овердрафтники -> ', () => {
        describe('Lite -> ', () => {
            beforeEach(async function() {
                const bro = this.browser;
                await bro.yaClientLoginFast('yndx-ufo-test-773');
                await bro.url(NAVIGATION.disk.url);
            });

            describe('Экран овердрафтника -> ', () => {
                it('diskclient-6819, diskclient-6931: Отображение заглушки лайт-овердрафта', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6931' : 'diskclient-6819';

                    await bro.yaAssertView(this.testpalmId, 'body', {
                        ignoreElements: [overdraftContent.text()]
                    });
                });

                it('diskclient-6834, diskclient-6942: Закрытие заглушки овердрафта по крестику', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6942' : 'diskclient-6834';

                    await closeOverdraftContent.call(this);
                });

                it('diskclient-6823, diskclient-6934: Переход в тюнинг из заглушки овердрафта', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6934' : 'diskclient-6823';

                    await bro.yaOpenLinkInNewTab(overdraftContent.addSpaceButton(), {
                        assertUrlHas: 'https://mail360.yandex.ru/premium-plans?from=disk_overdraft-screen'
                    });
                });

                it('diskclient-6825, diskclient-6935: Закрытие заглушки овердрафта по кнопке "Выбрать и удалить лишнее"', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6935' : 'diskclient-6825';

                    await bro.click(overdraftContent.cleanButton());
                    await bro.yaWaitForHidden(overdraftContent());
                });

                it('diskclient-6827, diskclient-6936: Переход в саппорт из заглушки овердрафта', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6936' : 'diskclient-6827';

                    await bro.yaOpenLinkInNewTab(overdraftContent.aboutLink(), {
                        assertUrlHas: 'support/disk/enlarge/disk-space.html'
                    });
                });

                it('diskclient-6830, diskclient-6939: Отображение заглушки лайт-овердрафта при перезагрузке страницы', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6939' : 'diskclient-6830';

                    await bro.refresh();
                    await bro.yaWaitForVisible(overdraftContent());
                });
            });

            hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
            describe('Плашка -> ', () => {
                beforeEach(async function() {
                    const bro = this.browser;
                    await closeOverdraftContent.call(this);
                    await bro.yaWaitForVisible(overdraftBlock());
                });

                it('diskclient-6821: Отображение плашки лайт-овердрафта', async function() {
                    const bro = this.browser;
                    this.testpalmId = 'diskclient-6821';

                    await bro.yaAssertView(this.testpalmId, 'body', {
                        ignoreElements: [
                            clientNavigation.desktop.spaceInfoSection(),
                            overdraftBlock.messageWrapper()
                        ]
                    });
                });

                it('diskclient-7022: Переход в тюнинг из плашки овердрафта', async function() {
                    const bro = this.browser;
                    this.testpalmId = 'diskclient-7022';

                    await bro.yaOpenLinkInNewTab(overdraftBlock.addSpaceButton(), {
                        assertUrlHas: 'https://mail360.yandex.ru/premium-plans?from=disk_overdraft_block'
                    });
                });

                it('diskclient-6835: Отображение плашки овердрафта при переходах в Диске', async function() {
                    const bro = this.browser;
                    this.testpalmId = 'diskclient-6835';

                    await bro.yaOpenSection('recent');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('photo');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('albums');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('shared');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('trash');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('journal');
                    await bro.yaWaitForVisible(overdraftBlock());

                    await bro.yaOpenSection('disk');
                    await bro.yaWaitForVisible(overdraftBlock());
                });
            });

            describe('Блокировка действий -> ', () => {
                beforeEach(async function() {
                    await closeOverdraftContent.call(this);
                });

                it('diskclient-6837, diskclient-6945: Отображение попапа овердрафта при загрузке файлов', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6945' : 'diskclient-6837';

                    await clickUploadButton.call(this, isMobile);
                    await checkBlockAction.call(this, isMobile);

                    // заскриним внешний вид попапа lite-овердрафтника
                    await bro.yaAssertView(this.testpalmId, 'body', {
                        invisibleElements: isMobile ? [
                            overdraftContent.text()
                        ] : [
                            clientNavigation.desktop.spaceInfoSection(),
                            overdraftContent.text(),
                            overdraftBlock()
                        ]
                    });
                });

                it('diskclient-6831, diskclient-6940: Отображение попапа овердрафта при поделении файлом', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6940' : 'diskclient-6831';

                    await clickPublishResource.call(this, TEST_FILE_NAME, isMobile);
                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-7025, diskclient-7028: Отображение заглушки овердрафта при копировании файла', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-7028' : 'diskclient-7025';

                    await selectResourceAndDoAction.call(this, TEST_FILE_NAME, 'copy');

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-7031, diskclient-7032: Отображение попапа овердрафта при публикации альбома', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ?
                        'diskclient-7032' :
                        'diskclient-7031';

                    await bro.url('/client/albums/61570f807e91b6eb8915b01c');
                    await closeOverdraftContent.call(this);

                    await albumActionOnPageAlbum.call(this, 'publish');
                    await checkBlockAction.call(this, isMobile);
                });

                hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
                describe('Создание общей папки -> ', () => {
                    it('diskclient-6954: Отображение попапа лайт-овердрафта при добавлении пользователя в общую папку', async function() {
                        const bro = this.browser;
                        this.testpalmId = 'diskclient-6954';

                        await bro.yaOpenSection('shared');
                        await bro.yaWaitForVisible(fileListing.listing.createSharedFolderButton());
                        await bro.click(fileListing.listing.createSharedFolderButton());

                        await checkBlockAction.call(this);
                    });

                    it('diskclient-6846: Отображение попапа лайт-овердрафта при добавлении пользователя в общую папку', async function() {
                        const bro = this.browser;
                        this.testpalmId = 'diskclient-6846';

                        await bro.yaSelectResource(SHARED_TEST_FOLDER_NAME);
                        await bro.yaWaitActionBarDisplayed();
                        await bro.click(popups.common.actionBar.moreButton());
                        await bro.yaWaitForVisible(popups.desktop.actionBarInviteButton());
                        await bro.click(popups.desktop.actionBarInviteButton());
                        await bro.yaWaitForVisible(popups.common.accessPopup.suggestInput());
                        await bro.yaSetValue(popups.common.accessPopup.suggestInput(), 'yndx-ufo-test-774@yandex.ru');
                        await bro.pause(500);
                        await bro.click(popups.common.accessPopup.formSend.sendButton());

                        await checkBlockAction.call(this);
                    });
                });
            });
        });

        describe('Hard -> ', () => {
            beforeEach(async function() {
                const bro = this.browser;
                await bro.yaClientLoginFast('yndx-ufo-test-776');
                await bro.url(NAVIGATION.disk.url);
            });

            describe('Блокировка действий -> ', () => {
                beforeEach(async function() {
                    await closeOverdraftContent.call(this);
                });

                it('diskclient-7034, diskclient-7035: Отображение попапа овердрафта при загрузке файлов', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ?
                        'diskclient-7035' :
                        'diskclient-7034';

                    await clickUploadButton.call(this, isMobile);
                    await checkBlockAction.call(this, isMobile);

                    await bro.pause(500);
                    // заскриним внешний вид попапа hard-овердрафтника
                    await bro.yaAssertView(this.testpalmId, 'body', {
                        ignoreElements: isMobile ? [
                            overdraftContent.title()
                        ] : [
                            clientNavigation.desktop.spaceInfoSection(),
                            overdraftContent.title(),
                            overdraftBlock()
                        ]
                    });
                });

                it('diskclient-7036, diskclient-7037: Отображение попапа овердрафта при поделении файлом', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ?
                        'diskclient-7037' :
                        'diskclient-7036';

                    await clickPublishResource.call(this, TEST_FILE_NAME, isMobile);
                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-6870, diskclient-6976: Отображение заглушки хард-овердрафта при копировании файла', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6976' : 'diskclient-6870';

                    await selectResourceAndDoAction.call(this, TEST_FILE_NAME, 'copy');

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-6849, diskclient-6956: Отображение попапа овердрафта при создании папки', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6956' : 'diskclient-6849';

                    if (isMobile) {
                        await bro.yaWaitForVisible(clientNavigation.touch.touchListingSettings.plus());
                        await bro.click(clientNavigation.touch.touchListingSettings.plus());
                        await bro.yaWaitForVisible(popups.touch.createPopup.createDirectory());
                        await bro.click(popups.touch.createPopup.createDirectory());
                    } else {
                        await bro.yaOpenCreatePopup();
                        await bro.click(popups.desktop.createPopup.createDirectory());
                    }

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-6853, diskclient-6960: Отображение попапа овердрафта при создании альбома', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6960' : 'diskclient-6853';

                    if (isMobile) {
                        await bro.yaWaitForVisible(clientNavigation.touch.touchListingSettings.plus());
                        await bro.click(clientNavigation.touch.touchListingSettings.plus());
                        await bro.yaWaitForVisible(popups.touch.createPopup.createAlbum());
                        await bro.click(popups.touch.createPopup.createAlbum());
                    } else {
                        await bro.yaOpenCreatePopup();
                        await bro.click(popups.desktop.createPopup.createAlbum());
                    }

                    await checkBlockAction.call(this, isMobile);
                });

                hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
                it('diskclient-6850: Отображение попапа овердрафта при создании документа', async function() {
                    const bro = this.browser;
                    this.testpalmId = 'diskclient-6850';

                    await bro.yaOpenCreatePopup();
                    await bro.click(popups.desktop.createPopup.createDocument());

                    await checkBlockAction.call(this);
                });

                it('diskclient-6861, diskclient-6968: Отображение попапа овердрафта при переименовании файла', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6968' : 'diskclient-6861';

                    await selectResourceAndDoAction.call(this, TEST_FILE_NAME, 'rename');

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-6864, diskclient-6970: Отображение заглушки хард-овердрафта при перемещении файлов', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6970' : 'diskclient-6864';

                    await selectResourceAndDoAction.call(this, TEST_FILE_NAME, 'move');

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-6882, diskclient-6985: Отображение попапа овердрафта при редактировании документа', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ? 'diskclient-6985' : 'diskclient-6882';

                    await selectResourceAndDoAction.call(this, TEST_DOC_NAME, 'edit');

                    await checkBlockAction.call(this, isMobile);
                });

                it('diskclient-7051, diskclient-7052: Отображение попапа овердрафта при открытии документа в редакторе', async function() {
                    const bro = this.browser;
                    const isMobile = await bro.yaIsMobile();
                    this.testpalmId = isMobile ?
                        'diskclient-7052' :
                        'diskclient-7051';

                    await openResourceByName.call(this, TEST_DOC_NAME, isMobile);

                    await checkBlockAction.call(this, isMobile);
                });

                describe('Диалог шаринга -> ', () => {
                    it('diskclient-6860, diskclient-6967: Отображение попапа поделения для публичного файла', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ? 'diskclient-6967' : 'diskclient-6860';

                        await clickPublishResource.call(this, PUBLIC_TEST_FILE_NAME, isMobile);

                        // заскриним внешний вид диалога шаринга для файла
                        await bro.pause(1000);
                        await bro.yaAssertView(
                            this.testpalmId, isMobile ? popups.touch.shareDialog() : popups.desktop.shareDialog(),
                            { ignoreElements: popups.common.shareDialog.textInput() }
                        );
                    });

                    it('diskclient-6915, diskclient-7005: Отображение попапа поделения для публичной папки', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ? 'diskclient-7005' : 'diskclient-6915';

                        await clickPublishResource.call(this, PUBLIC_TEST_FOLDER_NAME, isMobile);

                        // заскриним внешний вид диалога шаринга для папки
                        await bro.pause(500);
                        await bro.yaAssertView(
                            this.testpalmId, isMobile ? popups.touch.shareDialog() : popups.desktop.shareDialog(),
                            { ignoreElements: popups.common.shareDialog.textInput() }
                        );
                    });
                });

                describe('Альбомы -> ', () => {
                    beforeEach(async function() {
                        const bro = this.browser;
                        await bro.url('/client/albums/617fb404ba5446a23e04aad8');
                        await closeOverdraftContent.call(this);
                    });

                    it('diskclient-7038, diskclient-7039: Отображение попапа овердрафта при публикации альбома', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ?
                            'diskclient-7039' :
                            'diskclient-7038';

                        await albumActionOnPageAlbum.call(this, 'publish');
                        await checkBlockAction.call(this, isMobile);
                    });

                    it('diskclient-6879, diskclient-6982: Отображение попапа овердрафта при добавлении файла в личный альбом', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ? 'diskclient-6982' : 'diskclient-6879';

                        await albumActionOnPageAlbum.call(this, 'addToAlbum');
                        await checkBlockAction.call(this, isMobile);
                    });

                    it('diskclient-6863, diskclient-6969: Отображение попапа овердрафта при переименовании альбома', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ?
                            'diskclient-6969' :
                            'diskclient-6863';

                        await albumActionOnPageAlbum.call(this, 'rename', true);
                        await checkBlockAction.call(this, isMobile);
                    });
                });

                describe('Проигрывание -> ', () => {
                    beforeEach(async function() {
                        const bro = this.browser;
                        await bro.yaClientLoginFast('yndx-ufo-test-775');
                        await bro.url('/client/disk/' + TEST_FOLDER_NAME);
                        await closeOverdraftContent.call(this);
                    });

                    it('diskclient-6887, diskclient-6990: Отображение попапа овердрафта при воспроизведении аудио', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ? 'diskclient-6990' : 'diskclient-6887';

                        await openResourceByName.call(this, TEST_AUDIO_NAME, isMobile);

                        await bro.waitForVisible(slider.contentSlider.audioPlayer.playPauseButton());
                        await bro.click(slider.contentSlider.audioPlayer.playPauseButton());

                        await checkBlockAction.call(this, isMobile);
                    });

                    it('diskclient-6886, diskclient-6989: Отображение попапа овердрафта при воспроизведении видео', async function() {
                        const bro = this.browser;
                        const isMobile = await bro.yaIsMobile();
                        this.testpalmId = isMobile ? 'diskclient-6989' : 'diskclient-6886';

                        await openResourceByName.call(this, TEST_VIDEO_NAME, isMobile);

                        await bro.waitForVisible(slider.contentSlider.videoPlayer.overlayButton(), 1000);
                        await bro.click(slider.contentSlider.videoPlayer.overlayButton());

                        await checkBlockAction.call(this, isMobile);
                    });
                });
            });
        });
    });
});

describe('Овердрафт -> Новые овердрафтники -> Hard -> ', () => {
    const PATH = '/documents';
    hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-6881'] });
    it('diskclient-6881, diskclient-6984: Отображение попапа овердрафта при восстановлении версии документа', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6984' : 'diskclient-6881';

        await bro.url(NAVIGATION.disk.url + PATH);

        await bro.yaSkipWelcomePopup();
        await closeOverdraftContent.call(this);

        // выберем любой файл из папки
        const fileNames = await bro.yaGetListingElementsTitles();
        const testFileName = fileNames[Math.floor(Math.random() * fileNames.length)];

        // старые версии документов хронятся 14 дней, чтобы всегда были версии - нужно восстановить из Корзины документ
        await bro.yaDeleteResource(testFileName);
        await bro.yaOpenSection('trash');
        await bro.yaSelectResource(testFileName);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups[isMobile ? 'touch' : 'desktop'].actionBar.restoreFromTrashButton());
        await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_RESTORE);

        await bro.url(NAVIGATION.disk.url + PATH);
        await bro.yaSkipWelcomePopup();
        await closeOverdraftContent.call(this);

        await bro.yaSelectResource(testFileName);
        await bro.yaCallActionInActionBar('versions', true);
        await bro.yaWaitForVisible(versions.common.versionsDialog.versionItem());
        if (isMobile) {
            await bro.click(versions.touch.moreButton());
            await bro.yaWaitForVisible(versions.touch.restoreButton());
            await bro.click(versions.touch.restoreButton());
        } else {
            await bro.yaWaitForVisible(versions.common.versionsDialog.versionItemWithActions());
            await bro.moveToObject(versions.common.versionsDialog.versionItemWithActions());
            await bro.yaWaitForVisible(versions.common.versionsDialog.restoreButton());
            await bro.click(versions.common.versionsDialog.restoreButton());
        }

        await checkBlockAction.call(this, isMobile);
    });
});
