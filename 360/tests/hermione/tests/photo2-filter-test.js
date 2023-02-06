const { NAVIGATION } = require('../config').consts;
const photo = require('../page-objects/client-photo2-page').common;
const { consts } = require('../config');
const { albums } = require('../page-objects/client-albums-page');
const popups = require('../page-objects/client-popups');

const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { assert } = require('chai');

const FILTER_SELECTORS = {
    all: {
        touch: photo.photoHeaderMenu.filterAllRadio(),
        desktop: photo.filterSelect.popup.all()
    },
    unlim: {
        touch: photo.photoHeaderMenu.filterUnlimRadio(),
        desktop: photo.filterSelect.popup.unlim()
    },
    folders: {
        touch: photo.photoHeaderMenu.filterFoldersRadio(),
        desktop: photo.filterSelect.popup.folders()
    }
};

/**
 * @param {Object} bro
 */
async function waitActionBarDisplayed(bro) {
    await bro.waitUntil(async () => {
        const actionBars = await bro.$$(popups.common.actionBar());
        const displayedActionBars = (await Promise.all(actionBars.map((bar) => bar.getAttribute('class'))))
            .filter((className) => className.includes('resources-action-bar_visible'));

        return Boolean(displayedActionBars.length);
    });
}

/**
 * @param {'all'|'unlim'|'folders'} filter
 */
async function selectFilter(filter) {
    const bro = this.browser;
    const isMobile = await bro.yaIsMobile();
    const filterSelector = FILTER_SELECTORS[filter][isMobile ? 'touch' : 'desktop'];
    if (isMobile) {
        await bro.click(photo.photoHeader.menuButton());
        await bro.yaWaitForVisible(photo.photoHeaderMenu());
        await bro.pause(500);
        await bro.click(filterSelector);
        await bro.yaWaitForHidden(photo.photoHeaderMenu());
    } else {
        await bro.click(photo.filterSelect());
        await bro.yaWaitForVisible(photo.filterSelect.popup());
        await bro.click(filterSelector);
        await bro.yaWaitForHidden(photo.filterSelect.popup());
    }
}

describe('Фильтрация безлимитных фото в фотосрезе 2 -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-27');
        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-4295: Отображение фильтра', async function() {
        const bro = this.browser;

        const isMobile = await bro.yaIsMobile();

        const controlSelector = isMobile ? photo.photoHeader.menuButton() : photo.filterSelect();
        const menuSelector = isMobile ? photo.photoHeaderMenu() : photo.filterSelect.popup();

        await bro.yaWaitForVisible(controlSelector);
        await bro.click(controlSelector);
        await bro.yaWaitForVisible(menuSelector);
        await bro.pause(500); // because of `animation-duration: .2s`
        await bro.assertView('diskclient-4295', menuSelector);
    });
});

describe('Фильтрация безлимитных фото в фотосрезе 3 -> ', () => {
    /**
     * @param {string} login
     * @returns {Promise<void>}
     */
    async function loginAngGoToPhotoslice(login) {
        const bro = this.browser;
        await bro.yaClientLoginFast(login);
        await bro.url(NAVIGATION.photo.url);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();
    }

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5259, diskclient-5506: Фильтр "Безлимит" в Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5259' : 'diskclient-5506';
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');

        await selectFilter.call(this, 'unlim');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaAssertView(this.testpalmId, photo.photo());
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5260, diskclient-5507: Фильтр "Из папок" в Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5260' : 'diskclient-5507';
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');

        await selectFilter.call(this, 'folders');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaAssertView(this.testpalmId, photo.photo());
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5261, diskclient-5496: Фильтр "Безлимит" при отсутствии безлимитных файлов в Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5261' : 'diskclient-5496';
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-170');

        await selectFilter.call(this, 'unlim');
        await bro.yaWaitForVisible(photo.photo.filterStub());

        await bro.yaAssertView(this.testpalmId, photo.photo());
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5262, diskclient-5497: Фильтр "Из папок" при отсутствии обычных файлов в Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5262' : 'diskclient-5497';
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-229');

        await selectFilter.call(this, 'folders');
        await bro.yaWaitForVisible(photo.photo.filterStub());

        await bro.yaAssertView(this.testpalmId, photo.photo());
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5263, diskclient-5498: Фильтр "Все" в Фото', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5263' : 'diskclient-5498';

        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');
        await selectFilter.call(this, 'folders');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await selectFilter.call(this, 'all');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaAssertView(this.testpalmId, photo.photo());
    });

    it('diskclient-5264, diskclient-5499: Повторное переключение фильтров в Фото', async function() {
        const bro = this.browser;
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');

        await selectFilter.call(this, 'folders');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await selectFilter.call(this, 'unlim');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaWaitForVisible(photo.photo.itemByName().replace(':title', '2019-01-29 16-18-06.JPG'));
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5265, diskclient-5500: Развыделение файлов после переключения фильтра в Фото', async function() {
        const bro = this.browser;
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');

        await bro.yaSelectPhotoItemByName('2019-01-29 16-18-06.JPG', true);
        await bro.yaSelectPhotoItemByName('2019-01-29 16-17-37.JPG', false, false, consts.KEY_SHIFT);

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['2019-01-29 16-18-06.JPG', '2019-01-29 16-18-06.JPG', '2019-01-29 16-17-49.JPG', '2019-01-29 16-17-37.JPG']
        );

        await selectFilter.call(this, 'unlim');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            []
        );
    });

    it('diskclient-5266, diskclient-5501: Выделение кластера в Фото с фильтром "Безлимит"', async function() {
        const bro = this.browser;
        await loginAngGoToPhotoslice.call(this, 'yndx-ufo-test-27');

        await bro.yaSelectCluster(photo.photo.titleLabel());
        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            [
                '2019-01-29 16-18-06.JPG', '2019-01-29 16-18-06.JPG', '2019-01-29 16-17-49.JPG',
                '2019-01-29 16-17-37.JPG', '2019-01-29 16-17-37.JPG'
            ]
        );

        await selectFilter.call(this, 'unlim');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectCluster(photo.photo.titleLabel());
        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['2019-01-29 16-18-06.JPG', '2019-01-29 16-17-49.JPG', '2019-01-29 16-17-37.JPG']
        );

        await selectFilter.call(this, 'folders');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaSelectCluster(photo.photo.titleLabel());
        assert.deepEqual(
            await bro.yaGetSelectedPhotoItemsNames(),
            ['2019-01-29 16-18-06.JPG', '2019-01-29 16-17-37.JPG']
        );
    });

    it('diskclient-6151, 6150: Смена фильтра при созданиии альбома', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-27');
        await bro.url('/client/albums');

        await bro.yaWaitForVisible(albums.newAlbumButton());
        await bro.click(albums.newAlbumButton());
        await bro.yaWaitForVisible(popups.common.albumTitleDialog());
        await bro.click(popups.common.albumTitleDialog.submitButton());
        await bro.yaWaitForHidden(popups.common.albumTitleDialog());

        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await waitActionBarDisplayed(bro);

        await selectFilter.call(this, 'folders');

        await waitActionBarDisplayed(bro);
    });
});
