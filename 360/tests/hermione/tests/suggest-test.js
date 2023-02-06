const search = require('../page-objects/client-search-form');
const slider = require('../page-objects/slider').common;
const listing = require('../page-objects/client-content-listing').common;
const clientPageObjects = require('../page-objects/client');
const { LISTING, NAVIGATION } = require('../config').consts;
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

/**
 * @typedef { import('@ps-int/ufo-hermione/types').Browser } Browser
 */

describe('Саджесты -> ', () => {
    it('diskclient-1634, 1609: assertView: Подсказки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = await isMobile ? 'diskclient-1634' : 'diskclient-1609';
        await bro.yaClientLoginFast(isMobile ? 'yndx-ufo-test-134' : 'yndx-ufo-test-35');

        const inputSelector = isMobile ? search.common.searchForm.input() : clientPageObjects.psHeader.suggest.input();
        await bro.click(inputSelector);
        await bro.addValue(inputSelector, 'мо');
        await bro.yaWaitForVisible(isMobile ?
            search.common.searchResultItems.item() :
            clientPageObjects.psHeader.suggest.items.item()
        );
        await bro.pause(220);
        await bro.assertView(this.testpalmId, isMobile ?
            search.common.searchResultItems() :
            clientPageObjects.psHeader.suggest.items()
        );
    });

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1743: Закрытие по стрелке назад/крестику', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1743';
        await bro.yaClientLoginFast('yndx-ufo-test-82');

        await bro.click(search.common.searchForm.input());
        await bro.yaWaitForVisible(search.common.searchResult());
        await bro.click(search.touch.buttonX());
        await bro.yaWaitForHidden(search.common.searchResult());
    });

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1745: assertView: Игнор символов', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1745';

        await bro.yaClientLoginFast('yndx-ufo-test-82');
        await bro.click(search.common.searchForm.input());
        await bro.addValue(search.common.searchForm.input(), '!@#$ %^ &[{}]$^±§*()+_=,><&мор');
        await bro.yaWaitForVisible(search.common.searchResultItems.item());
        await bro.assertView(this.testpalmId, search.common.searchResultItems());
    });

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1843: Открытие Слайдера с изображением', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1843';

        await bro.yaClientLoginFast('yndx-ufo-test-82');
        await bro.click(search.common.searchForm.input());
        await bro.addValue(search.common.searchForm.input(), 'ujhs');
        await bro.yaWaitForVisible(search.common.searchResultItems.itemFile());
        await bro.click(search.common.searchResultItems.itemFile());
        await bro.waitForVisible(slider.contentSlider.previewImage());
        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());
        await bro.waitForVisible(search.common.searchResultItems.itemFile());
        await bro.hasFocus(search.common.searchForm.input());
    });

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1764: assertView: Саджесты. Открытие папки при клике по файлу', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1764';

        await bro.yaClientLoginFast('yndx-ufo-test-82');
        await bro.click(search.common.searchForm.input());
        await bro.addValue(search.common.searchForm.input(), 'group');
        await bro.yaWaitForVisible(search.common.searchResultItems.item());
        await bro.click(search.common.searchResultItems.item());
        await bro.yaWaitForHidden(search.common.searchResultItems());
        await bro.waitForVisible(listing.clientListing());
        await bro.waitForVisible(listing.listing.item());
        await bro.assertView(this.testpalmId, listing.clientListing());
    });

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1631: Саджесты. История просмотров', async function() {
        const testData = {
            user: 'yndx-ufo-test-540',
            searches: ['хлебные', 'mp4', 'jpg', 'крошки', 'Санкт-Петербург', 'Москва', 'Море', 'Мишки', 'Зима', 'Горы']
        };
        const bro = this.browser;
        this.testpalmId = 'diskclient-1631';

        await bro.yaClientLoginFast(testData.user);
        await bro.click(search.common.searchForm());
        await bro.yaWaitForVisible(search.common.searchResultItems());

        const searchItems = await bro.$$(search.common.searchResultItems.item());
        const actualSearches = await Promise.all(searchItems.map((item) => item.getAttribute('title')));

        assert.deepEqual(actualSearches, testData.searches);
    });
});

const querySearchFolder = 'folder_name';
const querySearchAudio = 'little-big-faradenza.mp3';
const querySearchVideo = 'greenvideofile.mp4';
const querySearchBook = 'book.fb2';
const querySearchManyFiles = '1K_file_';
const querySearchSeveralFilesLast = '1K_file_36.txt';

describe('Переходы из саджестов поиска -> ', () => {
    const user = 'yndx-ufo-test-258';

    /**
     * @param {Browser} bro
     */
    async function searchBook(bro) {
        await bro.yaClientLoginFast(user);
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchBook);
        await bro.yaWaitForVisible(search.common.searchResultItems.itemFile() + `[title="${querySearchBook}"]`);
    }

    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-4853: Саджесты. Открытие папки', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4853';
        await bro.yaClientLoginFast(user);
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchFolder);
        await bro.yaWaitForVisible(search.common.searchResultItems.item());
        await bro.click(search.common.searchResultItems.item() + `[title="${querySearchFolder}"]`);
        await bro.yaAssertUrlInclude(querySearchFolder);
    });
    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1742: Саджесты. Открытие Слайдера с аудио', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1742';
        await bro.yaClientLoginFast(user);
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchAudio);
        await bro.yaWaitForVisible(search.common.searchResultItems.item());
        await bro.click(search.common.searchResultItems.item() + `[title="${querySearchAudio}"]`);
        await bro.yaWaitForVisible(slider.contentSlider.audioPlayer());
    });
    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1842: Саджесты. Открытие Слайдера с видео файлом', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1842';
        await bro.yaClientLoginFast(user);
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchVideo);
        await bro.yaWaitForVisible(search.common.searchResultItems.item());
        await bro.click(search.common.searchResultItems.item() + `[title="${querySearchVideo}"]`);
        await bro.yaWaitForVisible(slider.contentSlider.videoPlayer());
    });
    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-1739: Саджесты. Открытие DV', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1739';
        await searchBook(bro);
        await bro.yaClickAndAssertNewTabUrl(search.common.searchResultItems.item() + `[title="${querySearchBook}"]`,
            { linkShouldContain: 'docviewer' });
    });
    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-4858: Саджесты. Саджесты не закрываются после перехода в DV', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4858';
        await bro.yaClientLoginFast(user);
        await searchBook(bro);
        await bro.click(search.common.searchResultItems.item() + `[title="${querySearchBook}"]`);
        await bro.pause(500);
        await bro.yaWaitForVisible(search.common.searchResultItems.item() + `[title="${querySearchBook}"]`);
    });
    hermione.only.notIn(clientDesktopBrowsersList); // десктоп ниже в блоке тестов про единую шапку
    it('diskclient-4851: Саджесты. Результаты поиска. Скролл', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4851';
        await bro.yaClientLoginFast(user);
        await bro.yaSetListingType(LISTING.tile);
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchManyFiles);
        await bro.click(search.common.searchForm.submitButton());
        await bro.yaWaitForVisible(listing.listing.inner());
        await bro.yaScrollToEnd();
        await bro.yaWaitForVisible(`span=${querySearchSeveralFilesLast}`);
    });

    hermione.skip.notIn('', 'Сломанный тест https://st.yandex-team.ru/CHEMODAN-66954');
    it('diskclient-4900, 1733: Саджесты. Тип листинга "Список" в Корзине результатах поиска', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        const testpalmId = isMobile ? 'diskclient-4900' : 'diskclient-1733';
        this.testpalmId = testpalmId;
        await bro.yaClientLoginFast(user);
        await bro.yaSetListingType(LISTING.tile);
        await bro.yaOpenSection('trash');
        await bro.click(search.common.searchForm());
        await bro.yaSetValue(search.common.searchForm.input(), querySearchManyFiles);
        await bro.keys('Enter');
        await bro.yaWaitForVisible(listing.listingThemeRow());
    });
});

hermione.only.in(clientDesktopBrowsersList); // Единая шапка ПС только на десктопах
describe('Саджесты в единой шапке персональных сервисов -> ', () => {
    beforeEach(async function () {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-767');
        await bro.url(NAVIGATION.disk.url);
    });

    it('diskclient-6191: Саджест в единой шапке персональных сервисов', async function() {
        const bro = this.browser;
        const testpalmId = 'diskclient-6191';

        await bro.yaWaitForVisible(clientPageObjects.psHeader());
        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), 'м');
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.item());
        await bro.assertView(testpalmId, [
            clientPageObjects.psHeader(),
            clientPageObjects.psHeaderSuggestPopup()
        ], {
            invisibleElements: clientPageObjects.psHeader.calendarDay(),
            hideElements: clientPageObjects.psHeader.legoUser.ticker()
        });
    });

    it('diskclient-1608: assertView: Подсказки-истории', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1608';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items());
        await bro.assertView(this.testpalmId, clientPageObjects.psHeader.suggest.items());
    });

    it('diskclient-1610: Закрытие по крестику', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1610';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items());
        await bro.click(clientPageObjects.psHeader.suggest.close());
        await bro.yaWaitForHidden(clientPageObjects.psHeader.suggest.items());
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.close())));
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.submitButton())));
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-3453: Закрытие кликом на подложку', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-3453';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items());
        await bro.click('body');
        await bro.yaWaitForHidden(clientPageObjects.psHeader.suggest.items());
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.close())));
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.submitButton())));
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-3454: Закрытие с помощью Esc', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-3454';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items());
        await bro.keys('\uE00C');
        await bro.yaWaitForHidden(clientPageObjects.psHeader.suggest.items());
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.close())));
        assert(!(await bro.isVisible(clientPageObjects.psHeader.suggest.submitButton())));
    });

    it('diskclient-1735: assertView: Игнор символов', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1735';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), '!@#$ %^ &[{}]$^±§*()+_=,><&мор');
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items());
        await bro.assertView(this.testpalmId, clientPageObjects.psHeader.suggest.items());
    });

    it('diskclient-1841: Открытие Слайдера с изображением', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1841';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), 'ujhs');
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.click(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.waitForVisible(slider.contentSlider.previewImage());
        await bro.click(slider.sliderButtons.closeButton());
        await bro.yaWaitForHidden(slider.contentSlider.previewImage());
        await bro.waitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.hasFocus(clientPageObjects.psHeader.suggest.input());
    });

    it('diskclient-1763: Переход к папке', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1763';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), 'Vjhcrfz');
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.folderItem());
        await bro.click(clientPageObjects.psHeader.suggest.items.folderItem());
        await bro.yaWaitForHidden(clientPageObjects.psHeader.suggest.items());

        await bro.waitForVisible(listing.clientListing());
        await bro.waitForVisible(listing.listing.head.header());

        assert.equal((await bro.getText(listing.listing.head.header())), 'Морская');

        const url = await bro.getUrl();
        assert.equal(url, `${bro.options.baseUrl}/client/disk/${encodeURIComponent('Морская')}`);
    });

    it('diskclient-1731: Саджесты. Открытие папки при клике по файлу', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1731';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), 'group');
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.click(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.yaWaitForHidden(clientPageObjects.psHeader.suggest.items());

        await bro.waitForVisible(listing.clientListing());
        await bro.waitForVisible(listing.listing.head.header());

        assert.equal((await bro.getText(listing.listing.head.header())), 'folder_name');

        assert(await bro.yaIsResourceSelected('group_errors.js'), 'Ресурс "group_errors.js" не выделен');
    });

    it('diskclient-4849: Саджесты. Открытие Слайдера с аудио', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4849';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearchAudio);
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.click(clientPageObjects.psHeader.suggest.items.fileItem() + `[title="${querySearchAudio}"]`);
        await bro.yaWaitForVisible(slider.contentSlider.audioPlayer());
    });

    it('diskclient-1840: Саджесты. Открытие Слайдера с видео файлом', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1840';

        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearchVideo);
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
        await bro.click(clientPageObjects.psHeader.suggest.items.fileItem() + `[title="${querySearchVideo}"]`);
        await bro.yaWaitForVisible(slider.contentSlider.videoPlayer());
    });

    /**
     * @param {Browser} bro
     */
    async function searchBook(bro) {
        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearchBook);
        await bro.yaWaitForVisible(clientPageObjects.psHeader.suggest.items.fileItem());
    }

    it('diskclient-4850: Саджесты. Открытие DV', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4850';

        await searchBook(bro);
        await bro.yaClickAndAssertNewTabUrl(
            clientPageObjects.psHeader.suggest.items.fileItem() + `[title="${querySearchBook}"]`,
            { linkShouldContain: 'https://docviewer' }
        );
    });

    it('diskclient-4857: Саджесты. Саджесты не закрываются после перехода в DV', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4857';

        await searchBook(bro);
        await bro.click(clientPageObjects.psHeader.suggest.items.fileItem() + `[title="${querySearchBook}"]`);
        await bro.pause(500);
        assert(
            await bro.isVisible(clientPageObjects.psHeader.suggest.items.fileItem() + `[title="${querySearchBook}"]`)
        );
    });

    it('diskclient-1734: Саджесты. Результаты поиска. Скролл', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-1734';
        await bro.yaSetListingType(LISTING.tile);
        await bro.click(clientPageObjects.psHeader.suggest());
        await bro.yaSetValue(clientPageObjects.psHeader.suggest.input(), querySearchManyFiles);
        await bro.click(clientPageObjects.psHeader.suggest.submitButton());
        await bro.yaWaitForVisible(listing.listing.inner());
        await bro.yaScrollToEnd();
        await bro.yaWaitForVisible(`span=${querySearchSeveralFilesLast}`);
    });
});
