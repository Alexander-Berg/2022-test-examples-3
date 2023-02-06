const popups = require('../page-objects/client-popups');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const page = require('../page-objects/client-content-listing').common;
const search = require('../page-objects/client-search-form');
const navigation = require('../page-objects/client-navigation');
const slider = require('../page-objects/slider').common;
const { psHeader } = require('../page-objects/client');
const { assert } = require('chai');

const listingItem = (n) => `${page.listingBody.items()}:nth-of-type(${n})`;
const singleFileName = 'Горы.jpg';
const severalFileNames = ['Горы.jpg', 'Москва.jpg', 'Зима.jpg'];
const folderDownload = 'forDownload';

/**
 *
 */
async function downloadFile() {
    const bro = this.browser;
    const isMobile = await bro.yaIsMobile();
    await bro.yaWaitActionBarDisplayed();

    this.url = await bro.yaGetDownloadUrlFromAction(async() => {
        await bro.click(isMobile ?
            popups.touch.actionBar.downloadButton() : popups.desktop.actionBar.downloadButton());
    });
}

describe('Скачивание одного файла в разных разделах -> ', () => {
    it('diskclient-1405, diskclient-1481: Скачивание файла в корне Диска', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1405' : 'diskclient-1481';

        await bro.yaClientLoginFast('yndx-ufo-test-524');
        await bro.yaOpenSection('disk');
        await bro.yaSelectResource(singleFileName);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%93%D0%BE%D1%80%D1%8B\.jpg/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-6132, diskclient-664: Скачивание файла в разделе Последние', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6132' : 'diskclient-664';

        await bro.yaClientLoginFast('yndx-ufo-test-525');
        await bro.yaOpenSection('recent');
        await bro.yaSelectResource(singleFileName);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%93%D0%BE%D1%80%D1%8B\.jpg/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-6134, diskclient-734: Скачивание файла из среза публичных ссылок', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6134' : 'diskclient-734';

        await bro.yaClientLoginFast('yndx-ufo-test-527');
        await bro.url('client/published');
        await bro.yaSelectResource(singleFileName);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%93%D0%BE%D1%80%D1%8B\.jpg/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Скачивание группы файлов в разных разделах -> ', () => {
    it('diskclient-1013, diskclient-644: Скачивание группы файлов в корне Диска', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1013' : 'diskclient-644';

        await bro.yaClientLoginFast('yndx-ufo-test-524');
        await bro.yaOpenSection('disk');
        await bro.yaSelectResources(severalFileNames);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /\/downloader\.disk\.yandex\.ru\/zip-files\/.+&filename=archive-(.*)\.zip.*/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-6133, diskclient-3442: Скачивание группы файлов в разделе Последние', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6133' : 'diskclient-3442';

        await bro.yaClientLoginFast('yndx-ufo-test-525');
        await bro.yaOpenSection('recent');
        await bro.yaSelectResources(severalFileNames);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /\/downloader\.disk\.yandex\.ru\/zip-files\/.+&filename=archive-(.*)\.zip.*/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-6136, diskclient-3242: Скачивание группы файлов из среза публичных ссылок', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6136' : 'diskclient-3242';

        await bro.yaClientLoginFast('yndx-ufo-test-527');
        await bro.url('client/published');
        await bro.yaSelectResources(severalFileNames);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /\/downloader\.disk\.yandex\.ru\/zip-files\/.+&filename=archive-(.*)\.zip.*/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Скачивание из слайдера в саджестах -> ', () => {
    it('diskclient-1865, diskclient-1855: Скачивание файла из слайдера в саджестах', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1865' : 'diskclient-1855';

        await bro.yaClientLoginFast('yndx-ufo-test-528');
        const inputSelector = isMobile ? search.common.searchForm.input() : psHeader.suggest.input();
        await bro.click(inputSelector);
        await bro.addValue(inputSelector, 'Горы');
        const itemFileSelector = isMobile ?
            search.common.searchResultItems.itemFile() :
            psHeader.suggest.items.fileItem();
        await bro.yaWaitForVisible(itemFileSelector);
        await bro.click(itemFileSelector);
        await bro.waitForVisible(slider.contentSlider.previewImage());
        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(slider.sliderButtons.downloadButton());
        });

        assert.match(
            url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%93%D0%BE%D1%80%D1%8B\.jpg/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Скачивание файла через контекстное меню -> ', () => {
    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-863: Скачивание файла через контекстное меню', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-524');

        await bro.rightClick(listingItem(1));
        await bro.yaWaitForVisible(popups.common.actionPopup(), 'контекстное меню не отобразилось');
        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(popups.common.actionPopup.downloadButton());
        });

        assert.match(
            url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=%D0%93%D0%BE%D1%80%D1%8B\.jpg/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Скачивание папки -> ', () => {
    it('diskclient-1017, diskclient-656: Скачивание папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1017' : 'diskclient-656';

        await bro.yaClientLoginFast('yndx-ufo-test-530');
        await bro.yaSelectResource(folderDownload);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /downloader\.disk\.yandex\.ru\/zip\/.+&filename=forDownload\.zip/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-1110, diskclient-6138: Скачивание общей папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-1110' : 'diskclient-6138';

        await bro.yaClientLoginFast('yndx-ufo-test-531');
        await bro.yaOpenSection('shared');
        await bro.yaSelectResource(folderDownload);
        await downloadFile.call(this);

        assert.match(
            this.url,
            /downloader\.disk\.yandex\.ru\/zip\/.+&filename=forDownload\.zip/,
            'Некорректный url для скачивания'
        );
    });
});

describe('Скачивание вирусного файла -> ', () => {
    it('diskclient-6144, diskclient-3258: Скачивание вирусного файла', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6144' : 'diskclient-3258';

        await bro.yaClientLoginFast('yndx-ufo-test-532');
        await bro.yaSelectResource('eicar.zip');
        if (isMobile) {
            await bro.yaExecuteClick(navigation.touch.modalCell());
            await bro.yaWaitForHidden(navigation.touch.modalCell());
        }
        await bro.yaWaitActionBarDisplayed();
        await bro.click(isMobile ?
            popups.touch.actionBar.downloadButton() : popups.desktop.actionBar.downloadButton());
        await bro.yaWaitForVisible(popups.common.confirmationDialog());

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(popups.common.confirmationDialog.submitButton());
        });

        assert.match(
            url,
            /downloader\.disk\.yandex\.ru\/disk\/.+&filename=eicar\.zip/,
            'Некорректный url для скачивания'
        );
    });

    it('diskclient-6145, diskclient-3259: Скачивание вирусного файла в составе группы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6145' : 'diskclient-3259';

        await bro.yaClientLoginFast('yndx-ufo-test-532');
        await bro.yaSelectResources(['Горы.jpg', 'Зима.jpg', 'eicar.zip']);
        await bro.yaWaitActionBarDisplayed();

        const url = await bro.yaGetDownloadUrlFromAction(async() => {
            await bro.click(isMobile ?
                popups.touch.actionBar.downloadButton() : popups.desktop.actionBar.downloadButton());
        });

        assert.match(
            url,
            /\/downloader\.disk\.yandex\.ru\/zip-files\/.+&filename=archive-(.*)\.zip.*/,
            'Некорректный url для скачивания'
        );
    });
});
