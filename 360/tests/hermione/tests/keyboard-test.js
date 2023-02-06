const { consts } = require('../config');
const clientContentListing = require('../page-objects/client-content-listing');
const { photoItemByName } = require('../page-objects/client');
const slider = require('../page-objects/slider').common;
const { assert } = require('chai');
const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');

hermione.only.in(clientDesktopBrowsersList);
describe('Управление клавиатурой', () => {
    beforeEach(async function() {
        await this.browser.yaClientLoginFast('yndx-ufo-test-544');
    });

    it('diskclient-6317: Открытие папки по Enter', async function() {
        const bro = this.browser;
        await bro.url(consts.NAVIGATION.disk.url);

        await bro.yaSelectResource('Folder2');

        await bro.keys('Enter');

        await bro.yaAssertFolderOpened('Folder2');
        await bro.yaAssertListingHas('Горы.jpg');
    });

    it('diskclient-6318: Выбор ресурса по Tab', async function() {
        const bro = this.browser;
        await bro.url(consts.NAVIGATION.disk.url);

        await bro.yaSelectResource('Folder');

        await bro.keys('Tab');

        await bro.yaAssertView(
            'diskclient-6318',
            clientContentListing.common.listingBodyItemsXpath().replace(/:titleText/g, 'Folder2')
        );
    });

    it('diskclient-6319: Открытие слайдера по Enter в альбоме', async function() {
        const bro = this.browser;

        await bro.url('/client/albums/5f955d0d519db90a4ccf526d');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.yaFocus(photoItemByName().replace(':title', '36-42.jpg'));

        await bro.keys('Enter');

        await bro.yaWaitForVisible(slider.contentSlider.previewImage());

        assert.equal(await bro.yaGetActiveSliderImageName(), '36-42.jpg');
    });

    it('diskclient-6320: Выбор ресурса по tab в альбоме', async function() {
        const bro = this.browser;

        await bro.url('/client/albums/5f955d0d519db90a4ccf526d');
        await bro.yaWaitAlbumItemsInViewportLoad();

        await bro.yaSelectPhotoItem(photoItemByName().replace(':title', '36-42.jpg'), true);

        await bro.keys('Tab');

        await bro.pause(300);
        await bro.yaAssertView(
            'diskclient-6320',
            photoItemByName().replace(':title', '91-3.jpg')
        );
    });

    it('diskclient-6321: Открытие слайдера по Enter в фотосрезе', async function() {
        const bro = this.browser;

        await bro.url(consts.NAVIGATION.photo.url);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaFocus(photoItemByName().replace(':title', '2019-04-20 14-40-35.JPG'));

        await bro.keys('Enter');

        await bro.waitUntil(async () => {
            const previewImages = await bro.$$(slider.contentSlider.previewImage());
            const visiblePreviewImages = await Promise.all(previewImages.map((image) => image.isDisplayed()));

            return visiblePreviewImages.some(Boolean);
        });

        assert.equal(await bro.yaGetActiveSliderImageName(), '2019-04-20 14-40-35.JPG');
    });

    it('diskclient-6322: Выбор ресурса по tab в фотосрезе', async function() {
        const bro = this.browser;

        await bro.url(consts.NAVIGATION.photo.url);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectPhotoItem(photoItemByName().replace(':title', '2019-04-20 14-40-35.JPG'), true);

        await bro.keys('Tab');

        await bro.pause(300);
        await bro.yaAssertView(
            'diskclient-6322',
            photoItemByName().replace(':title', '2019-04-20 14-06-22.JPG')
        );
    });
});

hermione.only.in(clientDesktopBrowsersList);
describe('Управление клавиатурой', () => {
    afterEach(async function() {
        const bro = this.browser;
        await bro.url('/client/disk');
        const { tempFolderName } = this.currentTest.ctx;
        if (tempFolderName) {
            await bro.yaDeleteCompletely(tempFolderName, { safe: true, fast: true });
        }
    });

    it('diskclient-listing-delete-by-del: Удаление ресурса по del', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-545');

        await bro.url(consts.NAVIGATION.disk.url);
        const tempFolderName = 'tmp-' + Date.now();
        await bro.yaCreateFolder(tempFolderName);

        this.currentTest.ctx.tempFolderName = tempFolderName;

        await bro.yaOpenListingElement(tempFolderName);

        const resources = await bro.yaUploadFiles([
            'test-image1.jpg',
            'test-image2.jpg',
            'test-image3.jpg'
        ], { uniq: true });

        await bro.yaSelectResources(resources);

        await bro.keys('Delete');

        await bro.yaAssertProgressBarAppeared();
        await bro.yaAssertProgressBarDisappeared();

        for (const resource of resources) {
            await bro.yaAssertListingHasNot(resource);
        }
    });
});
