const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const search = require('../page-objects/client-search-form').common;
const listing = require('../page-objects/client-content-listing').common;
const slider = require('../page-objects/slider').common;
const clientPageObjects = require('../page-objects/client');

const { consts } = require('../config');
const { retriable } = require('@ps-int/ufo-hermione/helpers/retries');
const { assert } = require('chai');

/**
 * @param {string} filename - имя файла для поиска в диске
 */
async function LoginSearchAndOpenFile(filename) {
    const bro = this.browser;
    await bro.yaClientLoginFast('yndx-ufo-test-251');
    const isMobile = await bro.yaIsMobile();
    const inputSelector = isMobile ? search.searchForm.input() : clientPageObjects.psHeader.suggest.input();
    await bro.click(inputSelector);
    await bro.addValue(inputSelector, filename);
    await bro.keys('Enter');
    await bro.yaWaitForVisible(listing.listing.firstFile());
    await bro.yaOpenListingElement(filename);
}

describe('Поиск -> ', () => {
    it('diskclient-1992, 1972, Просмотреть изображение из поисковой выдачи', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1992' : 'diskclient-1972';
        await LoginSearchAndOpenFile.call(this, 'Горы.jpg');
        await bro.waitForVisible(slider.contentSlider.previewImage());
    });
    it('diskclient-5184, 5183, Просмотреть безлимитное изображение из поисковой выдачи', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5183' : 'diskclient-5184';
        await LoginSearchAndOpenFile.call(this, 'Утка.HEIC');
        await bro.waitForVisible(slider.contentSlider.previewImage());
    });
    it('diskclient-1991, 1971, Воспроизвести видео из поисковой выдачи', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1991' : 'diskclient-1971';
        await LoginSearchAndOpenFile.call(this, 'Хлебные крошки.mp4');
        await bro.pause(1000);
        await bro.yaAssertVideoIsPlaying();
    });
    it('diskclient-5186, 5185, Воспроизвести безлимитное видео из поисковой выдачи', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5186' : 'diskclient-5185';
        await LoginSearchAndOpenFile.call(this, 'БезлимитныйВидос.MP4');
        await bro.pause(1000);
        await bro.yaAssertVideoIsPlaying();
    });
    it('diskclient-1990, 1970, Воспроизвести аудио из поисковой выдачи', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-1990' : 'diskclient-1970';
        const fileName = 'New York City.mp3';
        await LoginSearchAndOpenFile.call(this, fileName);

        await bro.yaWaitForVisible(slider.contentSlider.audioPlayer.playPauseButton());
        await bro.click(slider.contentSlider.audioPlayer.playPauseButton());
        await bro.yaWaitForVisible(slider.contentSlider.audioPlayerPlay());

        await bro.yaAssertAudioIsPlaying(fileName);
    });

    describe('Действия с файлами из поисковой выдачи', () => {
        afterEach(async function() {
            const items = this.currentTest.ctx.items || [];
            if (items.length) {
                await this.browser.url(consts.NAVIGATION.disk.url);
                await this.browser.yaDeleteCompletely([].concat(items), { safe: true, fast: true });
            }
        });

        /**
         * Общая функция для действий с файлами из загрузчика.
         * В this.currentTest.ctx заносится поле testFileName, содержащее имя загружаемого файла.
         *
         * @param {Object} opts
         * @param {string} opts.user
         * @param {string} opts.action
         * @param {string} [opts.createdFolderName]
         * @param {*} [opts.args]
         * @returns {Promise<void>}
         */
        const fileActionsAtSearchTest = async function(opts) {
            const bro = this.browser;

            await bro.yaClientLoginFast(opts.user);

            this.currentTest.ctx.testFileName = await bro.yaUploadFiles('test-file.txt', { uniq: true });
            this.currentTest.ctx.items = [this.currentTest.ctx.testFileName];
            if (opts.createdFolderName) {
                await bro.yaCreateFolder(opts.createdFolderName);
                await bro.yaCloseActionBar();
                this.currentTest.ctx.items.push(opts.createdFolderName);
            }

            await retriable(async() => {
                await bro.yaSearch(this.currentTest.ctx.testFileName);
            }, 5, 4000);

            await bro.yaWaitForVisible(listing.listing.firstFile(), consts.FILE_OPERATIONS_TIMEOUT);
            await bro.yaSelectResource(this.currentTest.ctx.testFileName);
            await bro[`ya${opts.action}Selected`](opts.args);
        };

        it('diskclient-1994, 1975: Удалить файл из поисковой выдачи', async function() {
            await fileActionsAtSearchTest.call(this, { user: 'yndx-ufo-test-273', action: 'Delete' });

            await this.browser.yaAssertProgressBarAppeared();
            await this.browser.yaWaitNotificationForResource(
                this.currentTest.ctx.testFileName,
                consts.TEXT_NOTIFICATION_FILE_MOVED_TO_TRASH
            );

            await this.browser.url(consts.NAVIGATION.disk.url);
            await this.browser.yaAssertListingHasNot(this.currentTest.ctx.testFileName);
        });

        it('diskclient-1995, 1976: Переименовать файл из поисковой выдачи', async function() {
            const newTestFileName = `tmp-${Date.now()}.txt`;
            await fileActionsAtSearchTest.call(this, {
                user: 'yndx-ufo-test-244',
                action: 'Rename',
                args: newTestFileName
            });
            this.currentTest.ctx.items.push(newTestFileName);

            await this.browser.yaAssertProgressBarAppeared();
            await this.browser.yaAssertProgressBarDisappeared();

            await this.browser.yaAssertListingHasNot(this.currentTest.ctx.testFileName);
            await this.browser.yaAssertListingHas(newTestFileName);

            await this.browser.yaOpenSection('disk');
            await this.browser.yaWaitForHidden(listing.listingSpinner());

            await this.browser.yaAssertListingHasNot(this.currentTest.ctx.testFileName);
            await this.browser.yaAssertListingHas(newTestFileName);
        });

        it('diskclient-1993, 1974: Скопировать файл из поисковой выдачи', async function() {
            const bro = this.browser;

            const workFolderName = `tmp-${Date.now()}work-folder`;

            await fileActionsAtSearchTest.call(this, {
                user: 'yndx-ufo-test-406',
                action: 'Copy',
                createdFolderName: workFolderName,
                args: workFolderName
            });

            await bro.yaAssertProgressBarAppeared();
            await bro.yaWaitNotificationForResource(
                { name: this.currentTest.ctx.testFileName, folder: workFolderName },
                consts.TEXT_NOTIFICATION_FILE_COPIED_TO_FOLDER
            );
            await bro.yaAssertProgressBarDisappeared();

            await bro.yaAssertListingHas(this.currentTest.ctx.testFileName);
            await bro.url(consts.NAVIGATION.folder(workFolderName).url);
            await bro.yaWaitForHidden(listing.listingSpinner());
            await bro.yaAssertListingHas(this.currentTest.ctx.testFileName);
        });
    });
});

describe('Поиск из единой шапки персональных сервисов -> ', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-19');
        await bro.url(consts.NAVIGATION.disk.url);
    });

    const querySearch = '%2018&scopeSearch=%2Fdisk';

    /**
     *
     */
    async function assertSearchResults() {
        const bro = this.browser;
        await bro.yaWaitForVisible(listing.listing.headSearch());
        await bro.yaWaitForVisible(listing.listing.firstFile());

        assert.sameMembers(
            [
                '%2018&scopeSearch=%2Fdisk%20%20.pptx',
                '%2018&scopeSearch=%2Fdisk.MOV',
                '%2018&scopeSearch=%2Fdisk.PNG',
                '%2018&scopeSearch=%2Fdisk.HEIC',
                '%2018&scopeSearch=%2Fdisk.xlsx'
            ],
            await bro.yaGetListingElementsTitles()
        );
    }

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-1475: [Enter]Поиск по Диску', async function () {
        const bro = this.browser;
        this.testpalmId = 'diskclient-ps-header-search-by-enter';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearch);
        await bro.keys('Enter');

        await assertSearchResults.call(this);
    });

    hermione.only.in(clientDesktopBrowsersList, 'Актуально только для десктопной версии');
    it('diskclient-1476: [Найти]Поиск по Диску', async function () {
        const bro = this.browser;
        this.testpalmId = 'diskclient-ps-header-search-by-button';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearch);
        await bro.click(clientPageObjects.psHeader.suggest.submitButton());

        await assertSearchResults.call(this);
    });
});
