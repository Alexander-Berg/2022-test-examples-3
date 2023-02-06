const clientPopups = require('../page-objects/client-popups');

const consts = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

hermione.only.in(clientDesktopBrowsersList);
describe('Действия с файлами из загрузчика', () => {
    afterEach(async function() {
        const items = this.currentTest.ctx.items || [];
        if (items.length) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely(items, { safe: true, fast: true });
        }
    });

    /**
     * В контекст выносятся следующие переменные:
     * 1. this.currentTest.ctx.workFolderName - папка, с которой будет работать тест
     * 2. this.currentTest.ctx.items - список временных файлов для удаления
     *
     * @param {string} user
     * @param {string} action
     * @returns {Promise<void>}
     */
    const uploadFileAndCallAction = async function(user, action) {
        const bro = this.browser;
        this.currentTest.ctx.workFolderName = `tmp-${Date.now()}`;
        this.currentTest.ctx.items = [this.currentTest.ctx.workFolderName];

        await bro.yaClientLoginFast(user);

        await bro.yaCreateFolder(this.currentTest.ctx.workFolderName);
        await bro.yaCloseActionBar();

        this.currentTest.ctx.testFileName = await bro.yaUploadFiles(
            'test-file.txt',
            {
                uniq: true,
                closeUploader: false
            });
        this.currentTest.ctx.items.push(this.currentTest.ctx.testFileName);

        await bro.rightClick(clientPopups.common.uploader.listingItem());
        await bro.yaWaitForVisible(clientPopups.common.actionPopup[`${action}Button`]());
        await bro.click(clientPopups.common.actionPopup[`${action}Button`]());
    };

    it('diskclient-4744: Перемещение ресурса из загрузчика из контекстного меню', async function() {
        const bro = this.browser;

        await uploadFileAndCallAction.call(this, 'yndx-ufo-test-192', 'move');

        await bro.yaSelectFolderInDialogAndApply(this.currentTest.ctx.workFolderName);

        await bro.yaAssertProgressBarAppeared();
        await bro.yaWaitNotificationForResource(
            {
                name: this.currentTest.ctx.testFileName,
                folder: this.currentTest.ctx.workFolderName
            },
            consts.TEXT_NOTIFICATION_FILE_MOVED_TO_FOLDER
        );

        await bro.yaWaitForVisible(clientPopups.common.uploader(), 'Загрузчик не отображается.');

        await bro.yaGoToFolderAndWaitForListingSpinnerHide(this.currentTest.ctx.workFolderName);
        await bro.yaAssertListingHas(this.currentTest.ctx.testFileName);
    });

    it('diskclient-4745: Копирование ресурса из загрузчика из контекстного меню', async function() {
        const bro = this.browser;

        await uploadFileAndCallAction.call(this, 'yndx-ufo-test-278', 'copy');

        await bro.yaSelectFolderInDialogAndApply(this.currentTest.ctx.workFolderName);

        await bro.yaAssertProgressBarAppeared();
        await bro.yaWaitNotificationForResource(
            {
                name: this.currentTest.ctx.testFileName,
                folder: this.currentTest.ctx.workFolderName
            },
            consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
        );
        await bro.yaAssertProgressBarDisappeared();

        await bro.click(clientPopups.common.uploader.closeButton());
        await bro.yaWaitForHidden(clientPopups.common.uploader());

        await bro.yaAssertListingHas(this.currentTest.ctx.testFileName);
        await bro.yaGoToFolderAndWaitForListingSpinnerHide(this.currentTest.ctx.workFolderName);
        await bro.yaAssertListingHas(this.currentTest.ctx.testFileName);
    });
});
