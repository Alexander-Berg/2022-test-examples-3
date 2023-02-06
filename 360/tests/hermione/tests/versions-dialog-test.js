/* eslint-disable no-unused-vars,indent,prefer-const */
const popups = require('../page-objects/client-popups');
const versions = require('../page-objects/versions-dialog-modal');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

const testFilePdf = '45039903.pdf';
const testFileJpg = '1.jpg';
const testFileTxt = 'test-file.txt';
const historyOfChange = [
    {
        fileName: testFilePdf,
        touchId: '3456',
        desktopId: '3455'
    },
    {
        fileName: testFileJpg,
        touchId: '3459',
        desktopId: '3460'
    }
];

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

/**
 * @param {Browser} bro
 * @returns {Promise<Browser>}
 */
const waitForDialogVisible = async(bro) => {
    await bro.yaWaitForVisible(versions.common.versionsDialog());
    await bro.yaWaitForVisible(versions.common.versionsDialog.versionItem());
    if (!bro.yaIsMobile()) {
        await bro.yaSetModalDisplay('.versions-dialog');
    }
};

const doTest = ({ fileName, touchId, desktopId }) => {
    describe('Версионирование -> ', () => {
        it(`diskclient-${touchId}, ${desktopId}: assertView: Просмотр формы "История изменений" для файла ${fileName} без предыдущих версий`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            const testpalmId = await isMobile ? touchId : desktopId;
            this.testpalmId = testpalmId;

            await bro.yaClientLoginFast('yndx-ufo-test-36');
            await bro.yaSelectResource(fileName);
            await bro.yaWaitActionBarDisplayed();
            await bro.click(popups.common.actionBar.moreButton());
            await bro.pause(300);
            await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
            await bro.click(popups.common.actionBarMorePopup.versionsButton());
            await waitForDialogVisible(bro);
            if (isMobile) {
                await bro.pause(500);
            }
            await bro.assertView(testpalmId, versions.common.versionsDialog());
        });
    });
};

historyOfChange.forEach(doTest);
describe('Версионирование -> ', () => {
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-3462, 3461: Закрытие формы "История изменений" по крестику', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-3461';

        await bro.yaClientLoginFast('yndx-ufo-test-36');
        await bro.yaSelectResource(testFilePdf);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
        await bro.click(popups.common.actionBarMorePopup.versionsButton());
        await waitForDialogVisible(bro);
        await bro.click(versions.common.versionsDialog.buttonX());
        await bro.yaWaitForHidden(versions.common.versionsDialog());
    });

    it('diskclient-1212, 1062: assertView: Проверка различия между платными и бесплатными юзерами в форме "История изменений"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = await isMobile ? 'diskclient-1212' : 'diskclient-1062';
        this.testpalmId = testpalmId;

        await bro.yaClientLoginFast('yndx-ufo-test-oligarh');
        await bro.yaSelectResource(testFilePdf);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.pause(300);
        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
        await bro.click(popups.common.actionBarMorePopup.versionsButton());
        await waitForDialogVisible(bro);
        await bro.pause(300);
        await bro.assertView(testpalmId, versions.common.versionsDialog());
    });

    it('diskclient-1213: 1140: Отмена восстановления версии документа', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1213' : 'diskclient-1140';

        await bro.yaClientLoginFast('yndx-ufo-test-83');
        if (!isMobile) {
            await bro.yaUploadFiles(testFileTxt, { replace: true }); //в тачах нет возможности загрузки с заменой
        }
        await bro.yaSelectResource(testFileTxt);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
        await bro.pause(1000);
        await bro.click(popups.common.actionBarMorePopup.versionsButton());
        await waitForDialogVisible(bro);
        if (isMobile) {
            await bro.click(versions.touch.moreButton());
            await bro.yaWaitForVisible(versions.touch.restoreButton());
            await bro.click(versions.touch.restoreButton());
        } else {
            await bro.yaWaitForVisible(versions.common.versionsDialog.versionExpand());
            await bro.moveToObject(versions.common.versionsDialog.versionExpand());
            await bro.yaWaitForVisible(versions.common.versionsDialog.restoreButton());
            await bro.click(versions.common.versionsDialog.restoreButton());
        }
        await bro.yaWaitForVisible(versions.common.versionsRestoreDialog.buttonX());
        await bro.click(versions.common.versionsRestoreDialog.buttonX());
        await bro.yaWaitForHidden(versions.common.versionsRestoreDialog());
    });

    it('diskclient-1211: diskclient-987: Открытие документа в DV из формы "История изменений"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1211' : 'diskclient-987';

        await bro.yaClientLoginFast('yndx-ufo-test-36');
        if (!isMobile) {
            await bro.yaUploadFiles(testFileTxt, { replace: true }); //в тачах нет возможности загрузки с заменой
        }
        await bro.yaSelectResource(testFileTxt);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
        if (!isMobile) {
            await bro.pause(500); // Drawer animation
        }
        await bro.click(popups.common.actionBarMorePopup.versionsButton());
        await waitForDialogVisible(bro);
        if (isMobile) {
            await bro.yaWaitForVisible(versions.touch.moreButton());
            await bro.click(versions.touch.moreButton());
            await bro.yaWaitForVisible(versions.touch.openButton());
        } else {
            await bro.yaWaitForVisible(versions.common.versionsDialog.versionExpand());
            await bro.moveToObject(versions.common.versionsDialog.versionExpand());
            await bro.yaWaitForVisible(versions.common.versionsDialog.openButton());
        }
        await bro.yaClickAndAssertNewTabUrl(
            isMobile ? versions.touch.openButton() : versions.common.versionsDialog.openButton(),
            { linkShouldContain: 'docviewer' }
        );
    });

    it('diskclient-3495, 1000: Проверка схлопа/расхлопа цепочки версии документа', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3495' : 'diskclient-1000';

        await bro.yaClientLoginFast('yndx-ufo-test-36');
        await bro.yaSelectResource(testFileTxt);
        await bro.yaWaitActionBarDisplayed();
        await bro.click(popups.common.actionBar.moreButton());
        await bro.yaWaitForVisible(popups.common.actionBarMorePopup.versionsButton());
        await bro.click(popups.common.actionBarMorePopup.versionsButton());
        await waitForDialogVisible(bro);
        await bro.yaWaitForVisible(versions.common.versionsDialog.versionExpand());
        await bro.click(versions.common.versionsDialog.versionExpand());
        await bro.yaWaitForVisible(versions.common.versionsDialog.versionSublist());
        await bro.click(versions.common.versionsDialog.versionExpand());
        await bro.yaWaitForHidden(versions.common.versionsDialog.versionSublist());
    });

    it('diskclient-6683, diskclient-6684: Диалог подтверждения восстановления версии', async function () {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.yaClientLoginFast('yndx-ufo-test-586');

        await bro.yaUploadFiles(testFileTxt, { replace: true });
        await bro.yaSelectResource(testFileTxt);
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
        await bro.yaWaitForVisible(versions.common.versionsRestoreDialog());

        // mock date
        await bro.execute((dateSelector, timeSelector) => {
            document.querySelector(dateSelector).innerHTML = '3 июня 2021';
            document.querySelector(timeSelector).innerHTML = '18:41';
        }, versions.common.versionsRestoreDialog.content.date(), versions.common.versionsRestoreDialog.content.time());

        await bro.pause(500);
        await bro.yaAssertView(
            isMobile ? 'diskclient-6684' : 'diskclient-6683',
            versions.common.versionsRestoreDialog.modalContent(),
            { hideElements: [versions.common.versionsDialog()] }
        );
    });
});
