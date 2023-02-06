const popups = require('../page-objects/client-popups');
const consts = require('../config').consts;
const testFile = 'test-file.txt';
const filesForUploadTest = require('../config').filesForUploadTest;

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

describe('Загрузка файла -> ', () => {
    /**
     * @param {string} touchUser
     * @param {string} desktopUser
     */
    async function loginAndUploadFiles(touchUser, desktopUser) {
        const bro = this.browser;
        const user = await bro.yaIsMobile() ? touchUser : desktopUser;

        await bro.yaClientLoginFast(user);
        await bro.yaOpenSection('disk');

        const workFolderName = `tmp-${Date.now()}-work-folder`;
        this.currentTest.ctx.items = [workFolderName];

        await bro.yaCreateFolder(workFolderName);
        await bro.yaGoToFolderAndWaitForListingSpinnerHide(workFolderName);
        await bro.yaUploadFiles(testFile, { disableHashes: true });
    }

    afterEach(async function() {
        const items = this.currentTest.ctx.items || [];
        if (items.length) {
            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
        }
        await this.browser.yaCleanTrash();
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-80810');
    it('diskclient-5077, 4711: Загрузка файлов PDD юзеру', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5077' : 'diskclient-4711';

        await loginAndUploadFiles.call(this, 'pdd00', 'pdd01');
        await bro.yaAssertListingHas(testFile);
    });
    it('diskclient-5076, 4712: Загрузка файлов B2B юзеру', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5076' : 'diskclient-4712';

        await loginAndUploadFiles.call(this, 'b2b00', 'b2b01');
        await bro.yaAssertListingHas(testFile);
    });

    hermione.skip.notIn('', 'Сильно мигающий тест /https://st.yandex-team.ru/CHEMODAN-62218');
    it('diskclient-4767, 4713: Загрузка переполненному юзеру', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4767' : 'diskclient-4713';

        await bro.yaClientLoginFast('yndx-ufo-test-26');
        await bro.yaUploadFiles(testFile)
            .then(() => {
                throw new Error('Удалось загрузить файл переполненному юзеру');
            })
            .catch(async() => {
                await bro.yaWaitNotificationForResource(testFile, consts.TEXT_NOTIFICATION_UPLOAD_ERROR);
            });
    });

    it('diskclient-4787, 4704 Загрузка файла с неуникальным именем', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-4787' : 'diskclient-4704';

        await bro.yaClientLoginFast('yndx-ufo-test-76');
        await bro.yaAssertListingHas(testFile);
        await bro.yaUploadFiles(testFile)
            .then(() => {
                throw new Error('Удалось загрузить неуникальный файл');
            })
            .catch(async() => {
                const title = await bro.getText(popups.common.uploader.errorText());
                return title === consts.TEXT_UPLOAD_ERROR_FILE_EXIST;
            });
    });
});

const doTest = ({ type, name, touchId, desktopId, touchUser, desktopUser }) => {
    describe('Загрузка файла ->', () => {
        beforeEach(async function() {
            await this.browser.yaCleanTrash();
        });
        it(`diskclient-${touchId}, ${desktopId}: Загрузка файла ${type} `, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? touchId : desktopId;
            const user = await bro.yaIsMobile() ? touchUser : desktopUser;
            await bro.yaClientLoginFast(user);
            await bro.yaOpenSection('disk');
            await bro.yaDeleteAllResources();
            await bro.yaUploadFiles(name, { disableHashes: true });
        });
    });
};

filesForUploadTest.forEach(doTest);
