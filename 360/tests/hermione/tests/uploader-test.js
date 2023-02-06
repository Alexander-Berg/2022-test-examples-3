const { root } = require('../page-objects/client');
const clientPopups = require('../page-objects/client-popups');
const clientAlbums = require('../page-objects/client-albums-page');
const navigation = require('../page-objects/client-navigation');
const slider = require('../page-objects/slider');
const clientContentListing = require('../page-objects/client-content-listing');
const { infoSpaceButton, sidebarNavigation } = require('../page-objects/client-navigation').desktop;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const consts = require('../config').consts;
const { assert } = require('chai');
const fs = require('fs');
const path = require('path');

describe('Загрузчик ->', () => {
    afterEach(async function() {
        try {
            const { items } = this.currentTest.ctx;
            if (items) {
                const bro = this.browser;
                await bro.url(this.currentTest.ctx.cleanUrl);
                await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
                await bro.yaDeleteCompletely(items, { fast: true, safe: true });
            }
        } catch (error) {}
    });

    /**
     * @param {string} action
     * @param {string} link
     * @returns {Promise<void>}
     */
    async function callDocumentActionsFromUploader(action, link) {
        const bro = this.browser;
        const buttonName = `${action}Button`;

        await bro.yaClientLoginFast('yndx-ufo-test-280');
        const file = await bro.yaUploadFiles('test-file.docx', { uniq: true, closeUploader: false });

        this.currentTest.ctx.cleanUrl = '/client/disk';
        this.currentTest.ctx.items = [file];

        await bro.rightClick(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.actionPopup[buttonName]());
        await bro.click(clientPopups.common.actionPopup[buttonName]());

        await bro.pause(1000);
        const tabs = await bro.getTabIds();
        await bro.window(tabs[1]);
        await bro.yaAssertUrlInclude(link);
        await bro.window(tabs[0]);
        await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem());
    }

    /**
     * @param {Object} bro
     * @returns {Promise<void>}
     */
    async function isUploadFinish(bro) {
        await bro.yaWaitForVisible(
            clientPopups.common.uploader.uploadedItem(),
            'Файл не был загружен', 15000);
    }

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4751: Редактирование документа из загрузчика из контекстного меню', async function() {
        await callDocumentActionsFromUploader.call(this, 'edit', 'https://disk.yandex.ru/edit');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4750: Открытие в DV из загрузчика из контекстного меню', async function() {
        await callDocumentActionsFromUploader.call(this, 'view', 'https://docs.yandex.ru/docs/view?url=ya-disk');
    });

    it('diskclient-4786, 4715: Внешний вид загрузчика', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-430');
        await bro.url(consts.NAVIGATION.disk.url);
        const folderName = `tmp-${Date.now()}`;
        await bro.yaCreateFolder(folderName);
        this.currentTest.ctx.items = [folderName];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await bro.yaOpenListingElement(folderName);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.yaUploadFiles(['test-file.jpg', 'test-file.txt'], { uniq: false, closeUploader: false });
        await bro.yaWaitPreviewsLoaded(clientPopups.common.uploader.itemPreview(), true);

        await bro.pause(500); // анимация прогресс-бара

        await bro.yaAssertView('diskclient-4715', clientPopups.common.uploader(), { tolerance: 10 });
    });

    it('diskclient-4800, 4752: Переход к загруженному ресурсу из загрузчика (первая порция)', async function() {
        const bro = this.browser;
        const workFolderName = `tmp-${Date.now()}-work-folder`;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4800' : 'diskclient-4752';
        await bro.yaClientLoginFast('yndx-ufo-test-423');

        await bro.yaCreateFolder(workFolderName);
        this.currentTest.ctx.items = [workFolderName];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await bro.yaOpenListingElement(workFolderName);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true, closeUploader: false });

        await bro.click(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientContentListing.common.listingBodyItemsInfoXpath()
            .replace(/:titleText/g, testFileName));

        await bro.yaWaitActionBarDisplayed();
        assert.isTrue(await bro.yaIsResourceSelected(testFileName), 'Ресурс не выделен.');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4753: Переход к загруженному ресурсу из загрузчика (вторая+ порция) ', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4753';
        await bro.yaClientLoginFast('yndx-ufo-test-401');

        await bro.url(consts.NAVIGATION.disk.url + '/folder');
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url + '/folder';
        this.currentTest.ctx.items = [];
        const testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true, closeUploader: false });
        this.currentTest.ctx.items.push(testFileName);

        await bro.click(clientPopups.common.uploader.listingItem());

        await bro.yaWaitForVisible(clientContentListing.common.listingBodyItemsInfoXpath()
            .replace(/:titleText/g, testFileName));
        await bro.yaWaitActionBarDisplayed();

        assert.isTrue(await bro.yaIsResourceSelected(testFileName), 'Ресурс не выделен.');
    });

    /**
     * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
     * @param {Browser} bro
     * @param {string|string[]} testFiles
     * @param {string} testPalmId
     */
    async function doDragAndDropTest(bro, testFiles, testPalmId) {
        await bro.url('/client/disk');
        await tryToCleanup(bro);

        await bro.yaUploadFiles(testFiles, { dragAndDrop: true, closeUploader: false });

        // иногда прогресс-бар не успевает заполниться
        await bro.waitUntil(async() => {
            const width = (await bro.execute((selector) => {
                return document.querySelector(selector).style.width;
            }, clientPopups.common.uploader.progressBar()));
            return width === '100%';
        }, consts.FILE_OPERATIONS_TIMEOUT, 'прогресс-бар не заполнился');
        // завершение анимации прогресс-бара
        await bro.pause(200);

        await bro.yaAssertView(testPalmId, root.content(),
            {
                ignoreElements: [infoSpaceButton(), sidebarNavigation(),
                    clientContentListing.common.listingBody.items.trashIconFull(),
                    clientContentListing.common.listingBody.items.trashIcon()],
                tolerance: 5,
            }
        );
        if (Array.isArray(testFiles)) {
            await Promise.all(testFiles.map((file) => bro.yaAssertListingHas(file)));
        } else {
            await bro.yaAssertListingHas(testFiles);
        }
    }
    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: 'diskclient-4714', lock_duration: '60' });
    it('diskclient-4714: Загрузка файла в Корень через днд', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4714';
        this.currentTest.ctx.items = 'test-file.txt';
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());

        await doDragAndDropTest(bro, this.currentTest.ctx.items, this.testpalmId);
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-74392');
    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: 'diskclient-4695', lock_duration: '60' });
    it('diskclient-4695: Загрузка группы файлов через drag&drop', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4695';
        this.currentTest.ctx.items = ['test-file.txt', 'test-file.jpg'];
        await bro.yaClientLoginFast('yndx-ufo-test-400');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await doDragAndDropTest(bro, this.currentTest.ctx.items, this.testpalmId);
    });

    /**
     * @param {Browser} bro
     *
     * @returns {void}
     */
    async function tryToCleanup(bro) {
        if (await bro.yaListingNotEmpty()) {
            try {
                await bro.yaDeleteAllResources();
                await bro.yaCleanTrash();
            } finally {
                await bro.url('/client/disk');
                await bro.waitForVisible(clientContentListing.common.clientListing());
            }
        }
    }

    it('diskclient-4754, 4783: Закрытие загрузчика по крестику', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-410');

        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await tryToCleanup(bro);

        this.currentTest.ctx.items = await bro.yaUploadFiles('test-file.txt', { closeUploader: true, uniq: true });
        await bro.refresh();
        assert(!(await bro.isVisible(clientPopups.common.uploader())), 'После рефреша появился загрузчик');
    });

    hermione.auth.tus({ tags: 'diskclient-626-4788', lock_duration: 60 });
    it('diskclient-626, 4788: Отказ от замены неуникального файла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        try {
            await bro.yaAssertListingHas('test-file.txt');
        } catch (error) {
            await bro.yaUploadFiles('test-file.txt', { closeUploader: true });
        }

        if (isMobile) {
            await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await bro.click(navigation.touch.touchListingSettings.plus());
            await bro.yaWaitForVisible(clientPopups.touch.createPopup.uploadFile());
        }

        await bro.doUpload('test-file.txt',
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input()
        );
        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.doNotUploadButton());
        await bro.click(clientPopups.common.uploader.uploaderNotifications.doNotUploadButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: 'diskclient-626', lock_duration: 60 });
    it('diskclient-626: Отмена замены неуникального файла в диалоге загрузок', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-411');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        try {
            await bro.yaAssertListingHas('test-file.txt');
        } catch (error) {
            await bro.yaUploadFiles('test-file.txt', { closeUploader: true });
        }

        await bro.doUpload('test-file.txt',
            navigation.desktop.sidebarButtons.upload.input()
        );

        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.doNotUploadButton());
        // для установки :hover
        await bro.moveToObject(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem.cancelButton());
        await bro.click(clientPopups.common.uploader.listingItem.cancelButton());

        await bro.yaWaitForHidden(clientPopups.common.uploader());
    });

    /**
     * @param {Browser} bro
     */
    async function interruptXHR(bro) {
        await bro.execute(() => {
            window.XMLHttpRequest.prototype.clearOpen = window.XMLHttpRequest.prototype.open;
            const modifiedOpen = function(method, url, async, user, password) {
                if (!document.stopXHRInterruption) {
                    url = 'https://localhost/error';
                }
                this.clearOpen(method, url, async, user, password);
            };
            window.XMLHttpRequest.prototype.open = modifiedOpen ;

            window.clearFetch = window.fetch;
            window.fetch = function(url, ...args) {
                if (!document.stopXHRInterruption) {
                    url = 'https://localhost/error';
                }
                return window.clearFetch(url, ...args);
            };
        });
    }

    /**
     * @param {Browser} bro
     */
    async function stopXHRInterruption(bro) {
        await bro.execute(() => {
            document.stopXHRInterruption = true;
            window.XMLHttpRequest.prototype.open = window.XMLHttpRequest.prototype.clearOpen;
            window.fetch = window.clearFetch;
        });
    }

    hermione.only.in(clientDesktopBrowsersList);
    // на деле настоящего обрыва сети не происходит.
    // запросы перенаправляются на локалхост некоторое время, потом возвращается нормальный режим.
    it('diskclient-4718: Обрыв сети при загрузке файла', async function() {
        const bro = this.browser;
        this.currentTest.ctx.items = 'test-file.mov';
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast('yndx-ufo-test-412');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await tryToCleanup(bro);

        if (isMobile) {
            await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await bro.click(navigation.touch.touchListingSettings.plus());
            await bro.yaWaitForVisible(clientPopups.touch.createPopup.uploadFile());
        }

        await interruptXHR(bro);
        await bro.doUpload('test-file.MOV',
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input()
        );
        await bro.pause(5000);
        await stopXHRInterruption(bro);

        await bro.waitUntil(async() => {
            const title = await bro.getText(clientPopups.common.uploader.progressText());
            return title === consts.TEXT_UPLOAD_DIALOG_UPLOAD_COMPLETE;
        }, consts.FILE_OPERATIONS_TIMEOUT, 'Загрузка файла не завершилась');
        await bro.yaAssertListingHas('test-file.MOV');
    });

    /**
     * @param {number} size
     *
     * @returns {string}
     */
    function prepareBigTestFile(size) {
        const filename = `tmp-file-${size}mb.txt`;
        const exists = fs.existsSync(path.resolve(consts.TEST_FILES_PATH, filename));
        if (!exists) {
            const garbage = new Uint8Array(1024 * 1024 * size);
            fs.writeFileSync(path.resolve(consts.TEST_FILES_PATH, filename), garbage);
        }
        return filename;
    }

    /**
     * @param {Browser} bro
     *
     * @returns {Promise<void>}
     */
    async function cancelAllUploads(bro) {
        await bro.yaWaitForVisible(clientPopups.common.uploader.moreButton());
        await bro.click(clientPopups.common.uploader.moreButton());
        await bro.yaWaitForVisible(clientPopups.common.actionBarMorePopup.cancelAllButton());
        await bro.click(clientPopups.common.actionBarMorePopup.cancelAllButton());
        await bro.yaWaitForHidden(clientPopups.common.actionBarMorePopup());
        const cancelButton = clientPopups.common.uploaderUploadConfirmationCancelButtonXpath();
        await bro.yaWaitForVisible(cancelButton);
        await bro.click(cancelButton);
    }

    // В FF ошибка " write /tmp/53ad23c5-22dd-4f62-9990-89bff452f79f/tmp-file-110mb.txt: no space left on device"
    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-72347');
    it('diskclient-4697, 4790: Отмена всех загрузок', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-413' : 'yndx-ufo-test-414');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await tryToCleanup(bro);

        // если файл сильно больше 100 МБ, загрузка вебдрайвера упадет,
        // т.к. он засовывает в zip и передает base64-строкой
        const testFile = prepareBigTestFile(110);

        if (isMobile) {
            await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await bro.click(navigation.touch.touchListingSettings.plus());
            await bro.yaWaitForVisible(clientPopups.touch.createPopup.uploadFile());
        }

        await bro.doUpload(testFile,
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input()
        );

        await cancelAllUploads(bro);
        await bro.yaWaitForHidden(clientPopups.common.uploader());
        await bro.pause(2000);
        await bro.yaAssertListingHasNot(testFile);
    });

    // В FF ошибка " write /tmp/53ad23c5-22dd-4f62-9990-89bff452f79f/tmp-file-110mb.txt: no space left on device"
    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-72283');
    it('diskclient-4724, 4791: Отмена нескольких загрузок по кнопке "Отменить загрузки"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-415' : 'yndx-ufo-test-416');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await tryToCleanup(bro);

        if (isMobile) {
            await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await bro.click(navigation.touch.touchListingSettings.plus());
            await bro.yaWaitForVisible(clientPopups.touch.createPopup.uploadFile());
        }

        this.currentTest.ctx.items = 'test-file.txt';
        // если суммарный объем файлов сильно больше 100 МБ, вебдрайвер кинет эксепшен
        const testFiles = ['test-file.txt', prepareBigTestFile(110)];

        await bro.doUpload(testFiles,
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input()
        );
        await bro.yaWaitForVisible(clientPopups.common.uploader.uploadedItem(), 10000);
        await cancelAllUploads(bro);
        await bro.yaWaitForVisible(clientPopups.common.uploader.closeButton());
        await bro.click(clientPopups.common.uploader.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());
        await bro.pause(2000);
        await bro.yaAssertListingHas(testFiles[0]);
        await bro.yaAssertListingHasNot(testFiles[1]);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4717: Отмена загрузки одного файла', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-411');
        await bro.url('/client/disk');
        await bro.waitForVisible(clientContentListing.common.clientListing());
        await tryToCleanup(bro);

        // начинаем загрузку большого файла
        const testFiles = await bro.yaUploadHugeFilesViaDragAndDrop(500);

        await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem());
        // для установки :hover
        await bro.moveToObject(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem.cancelButton());
        await bro.click(clientPopups.common.uploader.listingItem.cancelButton());

        await bro.yaWaitForHidden(clientPopups.common.uploader());
        await bro.pause(500);
        await bro.yaAssertListingHasNot(testFiles[0]);
    });

    /**
     * @param {Browser} bro
     *
     * @returns {Promise<void>}
     */
    async function clickPlusIfMobile(bro) {
        if (await bro.yaIsMobile()) {
            await bro.yaWaitForVisible(navigation.touch.touchListingSettings.plus());
            await bro.click(navigation.touch.touchListingSettings.plus());
            await bro.yaWaitForVisible(clientPopups.touch.createPopup.uploadFile());
        }
    }

    hermione.skip.notIn('', 'Мигает скриншот из-за гонки загрузки файлов – https://st.yandex-team.ru/CHEMODAN-75313');
    it('diskclient-4735, 4789: Переход на страницу оплаты из загрузчика', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4789' : 'diskclient-4735';
        await bro.yaClientLoginFast('yndx-ufo-test-442');

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await clickPlusIfMobile(bro);
        await bro.doUpload(['test-file.pdf', 'test-file.exe'],
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input());

        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications());
        // для прогрузки статусов загрузки
        await bro.yaWaitForVisible(clientPopups.common.uploader.uploaderNotifications.fileIcon());

        if (!isMobile) {
            await bro.moveToObject(clientPopups.common.uploader.listingItem());

            await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem.hoverButton());
            await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem.cancelButton());
        }

        await bro.yaClickAndAssertNewTabUrl(clientPopups.common.uploader.uploaderNotifications.buyPlaceButton(), {
            linkShouldContain: '/tuning'
        }, 'Не открылась страница покупки места');

        const currentTab = await bro.getCurrentTabId();
        const openedTabs = await bro.getTabIds();
        const secondTab = openedTabs.find((id) => id !== currentTab);
        await bro.switchTab(secondTab);
        assert(await bro.isVisible(clientPopups.common.uploader()), 'Загрузчик закрылся');

        const ignoredElements = [
            clientPopups.common.uploader.uploaderNotifications.fileName(),
            clientPopups.common.uploader.uploaderNotifications.fileIcon()
        ];
        await bro.yaAssertView(this.testpalmId, clientPopups.common.uploader(),
            { ignoreElements: ignoredElements });
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4719: Загрузка файла через контекстное меню', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4719';
        await bro.yaClientLoginFast('yndx-ufo-test-730');

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await bro.rightClick(clientContentListing.common.listing());
        await bro.yaWaitForVisible(clientPopups.desktop.contextMenuCreatePopup.upload());

        const filename = await bro.doUpload('test-file.jpg',
            clientPopups.desktop.contextMenuCreatePopup.upload.uploadInput(), true);
        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await isUploadFinish(bro);

        await bro.yaOpenListingElement(filename);
        await bro.yaWaitForVisible(slider.common.contentSlider.previewImage());
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: 'diskclient-4747', lock_duration: '30' });
    it('diskclient-4747: Удаление одного ресурса из загрузчика из контекстного меню', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4747';

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const filename = await bro.doUpload('test-file.pdf',
            navigation.desktop.sidebarButtons.upload.input(), true);
        const relativeSelector = clientContentListing.common.listingBodyItemsInfoXpath()
            .replace(/:titleText/g, filename);

        await isUploadFinish(bro);

        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await bro.rightClick(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.actionPopup.deleteButton());
        await bro.click(clientPopups.common.actionPopup.deleteButton());
        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();
        await bro.yaWaitForHidden(relativeSelector);
        await bro.yaWaitForHidden(clientPopups.common.uploader());

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());
        await bro.yaAssertListingHasNot(filename);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4743: Переименование ресурса из загрузчика из контекстного меню', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-731');

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const filename = await bro.doUpload('test-file.zip',
            navigation.desktop.sidebarButtons.upload.input(), true);

        const newFilename = 'new-' + filename;
        await isUploadFinish(bro);

        this.currentTest.ctx.items = [newFilename, filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await bro.rightClick(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.actionPopup.renameButton());
        await bro.click(clientPopups.common.actionPopup.renameButton());
        await bro.yaWaitForVisible(clientPopups.common.renameDialog.nameInput());
        await bro.yaSetValue(clientPopups.common.renameDialog.nameInput(), newFilename);
        await bro.click(clientPopups.common.renameDialog.submitButton());
        await bro.yaWaitForHidden(clientPopups.common.renameDialog());

        await bro.yaAssertListingHas(newFilename);
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-4740: Создание альбома из загрузчика из контекстного меню', async function() {
        const bro = this.browser;
        const testdata = {
            user: 'yndx-ufo-test-730',
            albumTitle: 'Новый альбом',
            phots: 'test-image1.jpg'
        };

        await bro.yaClientLoginFast(testdata.user);

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const filename = await bro.doUpload(testdata.phots,
            navigation.desktop.sidebarButtons.upload.input(), true);
        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await isUploadFinish(bro);
        await bro.rightClick(clientPopups.common.uploader.listingItem());

        await bro.yaClick(clientPopups.common.actionPopup.addToAlbumButton());
        await bro.yaClick(clientPopups.common.selectAlbumDialog.createAlbum());
        await bro.yaClick(clientPopups.common.albumTitleDialog.submitButton());

        await bro.yaWaitNotificationForResource(
            testdata.albumTitle,
            consts.TEXT_NOTIFICATION_ALBUM_CREATED,
            { close: false }
        );
        await bro.yaClickNotificationForResource(testdata.albumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
        await bro.yaCloseNotificationForResource(testdata.albumTitle, consts.TEXT_NOTIFICATION_ALBUM_CREATED);
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.yaAssertPhotosInAlbum([filename]);
        assert.strictEqual(await bro.getText(clientAlbums.album2.title()), testdata.albumTitle);
    });

    hermione.only.in(clientDesktopBrowsersList);
    hermione.auth.tus({ tags: ['diskclient-4741'], lock_duration: 40 });
    it('diskclient-4741: Добавление в альбом из загрузчика из контекстного меню', async function() {
        const bro = this.browser;
        const testdata = {
            file: 'test-image2.jpg',
            albumName: 'не удалять'
        };

        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const filename = await bro.doUpload(
            testdata.file,
            navigation.desktop.sidebarButtons.upload.input(),
            true
        );
        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await isUploadFinish(bro);

        await bro.rightClick(clientPopups.common.uploader.listingItem());
        await bro.yaClick(clientPopups.common.actionPopup.addToAlbumButton());
        await bro.yaClick(clientPopups.common.selectAlbumDialog.albumByName().replace(':title', testdata.albumName));
        await bro.yaWaitForHidden(clientPopups.common.selectAlbumDialog());
        await bro.yaAssertInViewport(clientPopups.desktop.uploader());
        await bro.yaOpenSection('albums');
        await bro.yaClick(clientAlbums.albumByName().replace(':titleText', testdata.albumName));
        await bro.yaWaitForVisible(clientAlbums.album());
        await bro.yaWaitForVisible(clientAlbums.album2.itemByName().replace(':title', filename));
    });

    it('diskclient-4755, 4756, 4784: Сворачивание и разворачивание загрузчика', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast('yndx-ufo-test-729');
        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        await clickPlusIfMobile(bro);
        const filename = await bro.doUpload('test-file.MOV',
            isMobile ?
                clientPopups.touch.createPopup.uploadFile.input() :
                navigation.desktop.sidebarButtons.upload.input(),
            true);
        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await bro.yaWaitForVisible(clientPopups.common.uploader.progressBarContainer());
        await bro.click(isMobile ? clientPopups.common.uploader.progressBarContainer() :
            clientPopups.common.uploader.collapseState());
        if (!isMobile) {
            assert.equal(await bro.getText(clientPopups.common.uploader.collapseState()), 'Развернуть');
        }

        await bro.yaOpenSection('journal');
        await bro.yaWaitForVisible(clientPopups.common.uploader.progressBarContainer());
        await bro.click(clientPopups.common.uploader.progressBarContainer());
        await bro.yaWaitForVisible(clientPopups.common.uploader.listingItem());
        if (!isMobile) {
            assert.equal(await bro.getText(clientPopups.common.uploader.collapseState()), 'Свернуть');
        }
    });

    hermione.only.notIn('firefox-desktop', 'https://st.yandex-team.ru/CHEMODAN-84277');
    it('diskclient-5742, 4778: Загрузка уникального файла', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-731');
        const isMobile = await bro.yaIsMobile();
        await bro.url(consts.NAVIGATION.disk.url);
        await bro.yaWaitForHidden(clientContentListing.common.listingSpinner());

        const exists = fs.existsSync(path.resolve(consts.TEST_FILES_PATH, 'uniq-test-file.txt'));
        if (exists) {
            fs.unlinkSync(path.resolve(consts.TEST_FILES_PATH, 'uniq-test-file.txt'));
        }
        fs.writeFileSync(path.resolve(consts.TEST_FILES_PATH, 'uniq-test-file.txt'),
            Math.random().toString(36) + Date.now());

        const filename = await bro.yaUploadFiles('uniq-test-file.txt', { closeUploader: false, uniq: true });
        this.currentTest.ctx.items = [filename];
        this.currentTest.ctx.cleanUrl = consts.NAVIGATION.disk.url;

        await isUploadFinish(bro);
        await bro.click(clientPopups.common.uploader.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());

        const selector = clientContentListing.common.listingBodyItemsInfoXpath().replace(/:titleText/g, filename);
        await bro.yaClickAndAssertNewTabUrl(selector, { linkShouldContain: 'docviewer', doDoubleClick: !isMobile });
    });
});
