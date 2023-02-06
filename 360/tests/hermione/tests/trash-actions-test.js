const { consts } = require('../config');
const testEmptyFile = 'test-file-empty.txt';

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

describe('Корзина -> ', () => {
    afterEach(async function() {
        const listingResources = this.currentTest.ctx.listingResources || [];

        if (listingResources.length) {
            const bro = this.browser;
            await bro.yaOpenSection('disk');
            await bro.yaDeleteCompletely(listingResources, { safe: true, fast: true });
        }
    });

    hermione.auth.tus({ tus_consumer: 'disk-front-client', tags: ['diskclient-1101', 'diskclient-617'] });
    it('diskclient-1101, 617: Корзина. Очистка корзины.', async function() {
        const bro = this.browser;
        const testFileName = await bro.yaUploadFiles('test-file.jpg', { uniq: true });
        this.currentTest.ctx.listingResources = [testFileName];

        await bro.yaSelectResource(testFileName);
        await bro.yaDeleteSelected();

        await bro.yaAssertProgressBarAppeared();
        await bro.yaWaitNotificationForResource(testFileName, consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH);
        await bro.yaAssertListingHasNot(testFileName);

        await bro.yaCleanTrash();
        await bro.yaAssertListingHasNot(testFileName);
    });

    it('diskclient-3784, 1509: Очистка корзины с пустой папкой', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3784' : 'diskclient-1509';
        await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-78' : 'yndx-ufo-test-31');

        const folderName = await bro.yaGetUniqResourceName();
        await bro.yaCreateFolder(folderName);
        this.currentTest.ctx.listingResources = [folderName];

        await bro.yaDeleteResource(folderName);
        await bro.yaOpenSection('trash');
        await bro.yaAssertListingHas(folderName);
        await bro.yaDeleteResource(folderName, { trash: true });
        this.currentTest.ctx.listingResources = [];
        await bro.yaAssertListingHasNot(folderName);
    });

    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-84275'); // не загружается файл
    it('diskclient-3783, 3782: Очистка Корзины с нулевым файлом', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-3783' : 'diskclient-3782';
        await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-79' : 'yndx-ufo-test-32');

        const fileName = await bro.yaUploadFiles(testEmptyFile, { closeUploader: true, uniq: true });
        this.currentTest.ctx.listingResources = [fileName];

        await bro.yaDeleteResource(fileName);
        await bro.yaOpenSection('trash');
        await bro.yaAssertListingHas(fileName);
        await bro.yaDeleteResource(fileName, { trash: true });
        this.currentTest.ctx.listingResources = [];
        await bro.yaAssertListingHasNot(fileName);
    });
});
