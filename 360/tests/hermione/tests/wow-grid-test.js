const getUser = require('@ps-int/ufo-hermione/helpers/getUser')(require('../config/index').login);
const { folder, album, consts } = require('../config');
const PageObjects = require('../page-objects/public');
const { wowGridItem } = require('../helpers/selectors');
const { assert } = require('chai');

describe('Паблик папки / альбома -> ', () => {
    /**
     * Проверить вау-сетку по урлу
     *
     * @param {string} url
     */
    async function assertWowGridWithAd(url) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(url);
        await bro.yaScrollToBeginGrid(isMobile);
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    }

    it('diskpublic-2834, diskpublic-2890: Отображение вау-сетки для папки с фото и видео пользователь с рекламой', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2890' : 'diskpublic-2834';

        await assertWowGridWithAd.call(this, folder.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
    });

    it('diskpublic-2849, diskpublic-2851: Отображение вау-сетки в публичном альбоме юзером с рекламой', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2851' : 'diskpublic-2849';

        await assertWowGridWithAd.call(this, album.PUBLIC_ALBUM_WITH_10_PHOTOS_URL);
    });

    /**
     * Проверить вау-сетку по урлу платным пользователем
     *
     * @param {string} url
     */
    async function assertWowGridWithoutAd(url) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();

        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.loginButton());
        await bro.yaClick(PageObjects.loginButton());
        await bro.login(getUser('yndx-ufo-test-oligarh'));

        // после авторизации платным пользователем снова зайдем на страницу
        await bro.url(url);
        await bro.yaWaitForVisible(PageObjects.wowGrid(), consts.WAITING_AUTH_TIMEOUT);
        await bro.yaScrollToBeginGrid(isMobile, true);
        await bro.pause(500);
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    }

    it('diskpublic-2835, diskpublic-2839: Отображение вау-сетки для папки с фото и видео у пользователя без рекламы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2835' : 'diskpublic-2839';

        await assertWowGridWithoutAd.call(this, folder.PUBLIC_FOLDER_WITH_10_PHOTOS_URL);
    });

    it('diskpublic-2848, diskpublic-2850: Отображение вау-сетки в публичном альбоме юзером без рекламы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2850' : 'diskpublic-2848';

        await assertWowGridWithoutAd.call(this, album.PUBLIC_ALBUM_WITH_10_PHOTOS_URL);
    });

    /**
     * Проверить отображение пустой папки / альбома
     *
     * @param {string} url
     */
    async function checkEmptyPublicResource(url) {
        const bro = this.browser;

        await bro.url(url);
        await bro.yaWaitForHidden(PageObjects.wowGrid());
        await bro.yaWaitForHidden(PageObjects.listing());
        await bro.yaWaitForVisible(PageObjects.content.empty());
    }

    it('diskpublic-2880, diskpublic-2882: Отображение пустой публичной папки', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2882' : 'diskpublic-2880';

        await checkEmptyPublicResource.call(this, folder.PUBLIC_FOLDER_EMPTY_URL);
    });

    it('diskpublic-2881, diskpublic-2883: Отображение пустого публичного альбома', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2883' : 'diskpublic-2881';

        await checkEmptyPublicResource.call(this, album.PUBLIC_ALBUM_EMPTY_URL);
    });

    /**
     * Проверить отображение вау-сетки после ресайза браузера
     *
     * @param {string} url
     */
    async function assertWowGridResized(url) {
        const bro = this.browser;

        await bro.url(url);
        const { height, width } = await bro.getWindowSize();
        await bro.setWindowSize(width - 400, height - 100);
        await bro.yaScrollToBeginGrid();
        await bro.yaAssertView(this.testpalmId, PageObjects.wowGrid());
    }

    hermione.only.in('chrome-desktop');
    it('diskpublic-2858: Отображение вау-сетки для папки после ресайза окна браузера', async function() {
        this.testpalmId = 'diskpublic-2858';
        await assertWowGridResized.call(this, folder.PUBLIC_FOLDER_WITH_MANY_PHOTOS_URL);
    });

    hermione.only.in('chrome-desktop');
    it('diskpublic-2859: Отображение вау-сетки публичного альбома после ресайза браузера', async function() {
        this.testpalmId = 'diskpublic-2859';
        await assertWowGridResized.call(this, album.PUBLIC_ALBUM_WITH_MANY_PHOTOS_URL);
    });

    /**
     * Подгрузка порций в режиме вау-сетки
     *
     * @param {string} url
     */
    async function checkWowLoadPortions(url) {
        const bro = this.browser;

        await bro.url(url);
        await bro.yaWaitPreviewsLoaded(PageObjects.wowGrid.item.preview(), true);
        const loadedItems = await bro.yaScrollAndGetItems(PageObjects.wowGrid.item(), PageObjects.wowGrid.spinner());
        await assert.equal(121, loadedItems.length, 'Количество файлов отличается от ожидаемого');
    }

    it('diskpublic-2837, diskpublic-2838: Подгрузка порций в режиме вау-сетки для папки с фото и видео', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2838' : 'diskpublic-2837';

        await checkWowLoadPortions.call(this, folder.PUBLIC_FOLDER_WITH_MANY_PHOTOS_URL);
    });

    it('diskpublic-2852, diskpublic-2853: Отображение вау-сетки при подгрузки порций в публичном альбоме', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskpublic-2853' : 'diskpublic-2852';

        await checkWowLoadPortions.call(this, album.PUBLIC_ALBUM_WITH_MANY_PHOTOS_URL);
    });

    it('wow-grid-slider-portions-load: подгрузка порций вау-сетки в слайдере', async function() {
        const bro = this.browser;
        await bro.url(folder.PUBLIC_FOLDER_WITH_MANY_PHOTOS_URL);
        await bro.yaWaitForVisible(PageObjects.wowGrid());
        await bro.yaClick(wowGridItem(1));
        await bro.yaWaitForVisible(PageObjects.slider.items(), 'Слайдер не открылся');
        await bro.yaChangeSliderActiveImage(81);
        assert.equal(await bro.yaGetActiveSliderImageName(), '2010-03-25 22-42-26.JPG');
    });
});
