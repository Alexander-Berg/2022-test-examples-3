const clientPopups = require('../page-objects/client-popups');
const { NAVIGATION } = require('../config').consts;
const consts = require('../config').consts;
const assert = require('chai').assert;

const TEST_FILE_NAME = 'Горы.jpg';
const TEST_FILE_NAME2 = '19-2.jpg';

describe('Диалоги -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-402');
        await bro.yaOpenSection('disk');
    });

    it('diskclient-5609, 5608: Диалог выбора папки закрывается после перехода на предыдущую страницу', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5609' : 'diskclient-5608';

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('move');
        await bro.yaWaitForVisible(clientPopups.common.selectFolderDialog());

        await bro.back();
        await bro.yaWaitForHidden(clientPopups.common.selectFolderDialog());
    });

    it('diskclient-5605, 5604: Диалог переименования скрывается после перехода на предыдущую страницу', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5605' : 'diskclient-5604';

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('rename');
        await bro.yaWaitForVisible(clientPopups.common.createDialog());

        await bro.back();
        await bro.yaWaitForHidden(clientPopups.common.createDialog());
    });

    it('diskclient-5607, 5606: Диалог создания папки скрывается после перехода на предыдущую страницу', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5607' : 'diskclient-5606';

        await bro.yaOpenCreateDirectoryDialog();

        await bro.back();
        await bro.yaWaitForHidden(clientPopups.common.createDialog());
    });

    it('diskclient-5612, 5611: Форма выбора альбома закрывается после выхода из папки.', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5612' : 'diskclient-5611';

        await bro.yaSelectResource(TEST_FILE_NAME);
        await bro.yaCallActionInActionBar('addToAlbum', false);
        await bro.yaWaitForVisible(clientPopups.common.selectAlbumDialog());

        await bro.back();
        await bro.yaWaitForHidden(clientPopups.common.selectAlbumDialog());
    });

    it('diskclient-scroll-in-dialog-select-albums: Скроллинг в диалоге выбора альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'scroll-in-dialog-select-albums' : 'diskclient-scroll-in-dialog-select-albums';

        await bro.yaClientLoginFast('yndx-ufo-test-769');
        await bro.yaOpenSection('disk');

        await bro.yaSelectResource(TEST_FILE_NAME2);
        await bro.yaCallActionInActionBar('addToAlbum', false);
        await bro.yaWaitForVisible(clientPopups.common.selectAlbumDialog());
        await bro.yaWaitForVisible(clientPopups.common.selectAlbumDialog.createAlbum());

        const selectorLastAlbumInDialog = clientPopups.common.selectAlbumDialog.album() + ':last-child';

        bro.execute((selector) => {
            const element = document.querySelector(selector);
            element && element.scrollIntoView();
        }, selectorLastAlbumInDialog);

        await bro.yaWaitPreviewsLoaded(clientPopups.common.selectAlbumDialog.preview());
        await bro.yaAssertView(this.testpalmId, clientPopups.common.selectAlbumDialog.content());
    });
});

describe('Диалоги -> ', () => {
    afterEach(async function() {
        // если тест упал при открытом диалоге выбора папки,
        // то очистка не сработает, т.к. кнопки будут закрыты
        // диалогом; рефреш помогает
        // FIXME не срабатывает GC, проверить
        await this.browser.refresh();

        const items = this.currentTest.ctx.items;
        const folder = this.currentTest.ctx.folder;

        if (folder) {
            await this.browser.url(NAVIGATION.folder(folder).url);
        } else {
            await this.browser.yaOpenSection('disk');
        }

        if (items) {
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
    });

    /**
     * @param {Object} bro
     * @param {boolean} scrolled
     */
    async function checkScrollInSelectFolderDialog(bro, scrolled) {
        await bro.yaWaitForVisible(clientPopups.common.selectFolderDialog());

        const isScrollAvailable = (await bro.execute((dialog) => {
            return $(dialog)[0].scrollHeight > $(dialog)[0].clientHeight;
        }, clientPopups.common.selectFolderDialog.content()));

        assert.equal(isScrollAvailable, true, 'Нет скролла в диалоге выбора папки');

        const scrollPostion = (await bro.execute((dialog) => {
            return $(dialog)[0].scrollTop > 0;
        }, clientPopups.common.selectFolderDialog.content()));

        assert.equal(
            scrollPostion,
            scrolled,
            scrollPostion ? 'Не сработал скролл к ресурсу' : 'Скролл не сбрасывается в начало'
        );
    }

    /**
     * @param {Object} bro
     * @param {string} file
     * @param {string} action
     */
    async function openSelectFolderDialog(bro, file, action = 'move') {
        await bro.yaSelectResource(file);
        await bro.yaCallActionInActionBar(action);

        const tree = await bro.$(clientPopups.common.selectFolderDialog.treeContent());
        await tree.waitForDisplayed({ timeout: 2000, timeoutMsg: 'Не показался диалог перемещения' });
    }

    /**
     * @param {Object} bro
     * @param {string} fileName
     * @param {string} folderName
     */
    async function submitActionInSelectFolderDialog(bro, fileName, folderName) {
        await bro.waitForEnabled(clientPopups.common.selectFolderDialog.acceptButton());
        await bro.click(clientPopups.common.selectFolderDialog.acceptButton());
        await bro.yaWaitForHidden(clientPopups.common.selectFolderDialog());

        await bro.yaAssertProgressBarAppeared();
        await bro.yaWaitNotificationForResource(
            { name: fileName, folder: folderName },
            consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
        );
    }

    /**
     * @param {Object} bro
     * @param {string} fileName
     */
    async function selectResourceInSelectFolderDialog(bro, fileName) {
        await bro.click(clientPopups.common.selectFolderDialogItemsXpath().replace(':titleText', fileName));
    }

    it('diskclient-6152, diskclient-6153: Сброс скролла в диалоге выбора папки при переходе по папкам', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6153' : 'diskclient-6152';

        // FIXME удалить файл tmp-1603790477519-0-test-file.txt,
        // т.к. он может быть удален GC (видимо, из-за него GC и не работает)
        // новый файл — template.txt
        const testdata = {
            user: 'yndx-ufo-test-546',
            folderName: 'new_folder_9',
            fileName: 'template.txt'
        };

        await bro.yaClientLoginFast(testdata.user);

        testdata.moveFile = await bro.yaUploadFiles('test-file.txt', { uniq: true });
        this.currentTest.ctx.items = testdata.moveFile;

        await openSelectFolderDialog(bro, testdata.moveFile);
        await selectResourceInSelectFolderDialog(bro, testdata.folderName);
        await checkScrollInSelectFolderDialog(bro, true);
        await submitActionInSelectFolderDialog(bro, testdata.moveFile, testdata.folderName);

        this.currentTest.ctx.folder = testdata.folderName;

        await openSelectFolderDialog(bro, testdata.fileName);
        await checkScrollInSelectFolderDialog(bro, false);
    });
});
