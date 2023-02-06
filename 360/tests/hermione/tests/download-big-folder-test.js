const popups = require('../page-objects/client-popups');
const listing = require('../page-objects/client-content-listing').common;
const folderName = 'node_modules';

describe('Скачивание большой папки -> ', () => {
    hermione.only.notIn('firefox-desktop');
    it('diskclient-3539: Скачивание большой папки через ПО - ОС несовместима с ПО', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = 'diskclient-3539';
        await bro.yaClientLoginFast('yndx-ufo-test-351');

        await bro.yaOpenSection('disk');
        await bro.yaWaitForHidden(listing.listingSpinner());

        await bro.yaSelectResource(folderName);
        await bro.yaWaitActionBarDisplayed();
        if (isMobile) {
            await bro.click(popups.touch.actionBar.downloadButton());
        } else {
            await bro.click(popups.desktop.actionBar.downloadButton());
        }
        await bro.yaWaitForVisible(popups.common.downloadBigFolderDialog());
        await bro.pause(500);
        await bro.yaAssertView(this.testpalmId, popups.common.downloadBigFolderDialog.content());

        await bro.click(popups.common.downloadBigFolderDialog.close());
        await bro.yaWaitForHidden(popups.common.downloadBigFolderDialog());
    });
});
