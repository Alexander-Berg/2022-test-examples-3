const { clientDesktopBrowsersList } = require('@ps-int/ufo-hermione/browsers');
const { photo, albumSliceDescription } = require('../page-objects/client-photo2-page').common;

describe('Альбомы-срезы -> ', () => {
    /**
     * @param {string} album
     * @param {string} testIdDesktop
     * @param {string} testIdTouch
     */
    async function assertViewAlbumSlice(album, testIdDesktop, testIdTouch) {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? testIdDesktop : testIdTouch;
        await bro.yaClientLoginFast('yndx-ufo-test-228');
        await bro.url(`/client/photo?filter=${album}`);

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, photo());
    }

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5428, diskclient-5538: Переход в автоальбом Красивые', async function() {
        await assertViewAlbumSlice.call(this, 'beautiful', 'diskclient-5428', 'diskclient-5538');
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5430, diskclient-5539: Переход в автоальбом Разобрать', async function() {
        await assertViewAlbumSlice.call(this, 'unbeautiful', 'diskclient-5430', 'diskclient-5539');
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5431, diskclient-5540: Переход в автоальбом Камера', async function() {
        await assertViewAlbumSlice.call(this, 'camera', 'diskclient-5431', 'diskclient-5540');
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5432, diskclient-5541: Переход в автоальбом Видео', async function() {
        await assertViewAlbumSlice.call(this, 'videos', 'diskclient-5432', 'diskclient-5541');
    });

    hermione.only.notIn('chrome-phone-6.0');
    it('diskclient-5433, diskclient-5537: Переход в автоальбом Скриншоты', async function() {
        await assertViewAlbumSlice.call(this, 'screenshots', 'diskclient-5433', 'diskclient-5537');
    });

    hermione.only.in(clientDesktopBrowsersList);
    it('diskclient-5440, diskclient-5536: Скролл при переходе из автоальбома в раздел Фото', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-228');

        // Открыть середину пофильтрованного фотосреза
        await bro.url('/client/photo?filter=beautiful&from=1496133357000');

        await bro.yaWaitForVisible(photo.itemByName().replace(/:title/, '2017-05-30 11-35-57.JPG'), 10000);
        await bro.yaWaitPhotoSliceItemsInViewportLoad();

        await bro.yaOpenSection('photo');

        // должен появиться ресурс не попадпющий под фильтр beautiful и находящийся в самом верху фотосреза
        await bro.yaWaitForVisible(photo.itemByName().replace(/:title/, 'attach.mov'));

        await bro.yaAssertScrollEquals(0);
    });

    it('diskclient-5248, diskclient-5529: Плашка дескрипшена отображается только первый раз', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-171');

        await bro.url('/client/photo?filter=beautiful');
        await bro.yaSaveSettings('beautifulAlbumDescriptionClosed', '0');
        await bro.yaWaitForVisible(albumSliceDescription());
        await bro.click(albumSliceDescription.closeButton());
        await bro.yaWaitForHidden(albumSliceDescription());
    });
});
