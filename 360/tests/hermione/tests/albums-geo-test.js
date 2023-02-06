const { NAVIGATION } = require('../config').consts;
const albums = require('../page-objects/client-albums-page');
const clientNavigation = require('../page-objects/client-navigation');
const slider = require('../page-objects/slider').common;
const listing = require('../page-objects/client-content-listing').common;
const { assert } = require('chai');

describe('Гео-альбомы ->', () => {
    hermione.skip.notIn('', 'https://st.yandex-team.ru/CHEMODAN-72160');
    it('diskclient-5632, 5631: онбординг', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-397');
        await bro.url(NAVIGATION.albums.url);

        await bro.execute(() => {
            ns.Model.get('settings').save({
                key: 'geoAlbumsOnboardingClosed',
                value: '0'
            });
        });

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.yaAssertView('diskclient-5631', '.albums2-onboarding-geo', {
            ignoreElements: [clientNavigation.desktop.infoSpaceButton()]
        });
    });

    it('diskclient-5745, 5744: отображение мета-альбома Места в списке альбомов', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        await bro.yaClientLoginFast('yndx-ufo-test-376');
        await bro.url(NAVIGATION.albums.url);

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.yaAssertView('diskclient-5744', isMobile ? albums.albums2RootContent() : albums.albums2());
    });

    it('diskclient-5636, 5635: отображение списка мест внутри мета-альбома Места', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-376');
        await bro.url(NAVIGATION.geoAlbums.url);

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.yaAssertView('diskclient-5635', albums.albums2());
    });

    it('diskclient-5749, 5748: возвращаемся в список из альбома по клику на кнопку `назад`', async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-376');
        await bro.url(NAVIGATION.albums.url);

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.click(albums.albums2.geo());

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.click(albums.albums2.item());
        await bro.yaWaitPreviewsLoaded(albums.album2.item.preview());

        await bro.back();

        await bro.yaWaitForVisible(albums.albums2.item.preview());

        const isGeoLinkVisible = await bro.isVisible(albums.albums2.geo());

        assert(!isGeoLinkVisible, 'Skipped list of places and went directly to personal albums');
    });

    it('diskclient-5766, diskclient-5768: Отображение альбома мест', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5768' : 'diskclient-5766';
        await bro.yaClientLoginFast('yndx-ufo-test-376');
        const geoAlbum = { id: '5e42945b52d01500550a8ca9', name: 'Сан-Франциско' };
        await bro.url(NAVIGATION.geoAlbums.url);

        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaClick(albums.albumByName().replace(':titleText', geoAlbum.name));

        await bro.yaAssertUrlInclude(geoAlbum.id);
        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, albums.album());
    });

    it('diskclient-6011, diskclient-6012: Скроллинг порций в альбоме мест', async function() {
        const bro = this.browser;
        const user = 'yndx-ufo-test-519';

        const album = {
            name: 'Санкт-Петербург',
            id: '5ea020c8737de300531d3691',
            type: 'геоальбом',
            length: 161,
            itemSelector: albums.album2.item(),
            lastItemName: '1998-07-03 14-56-45.JPG'
        };

        await bro.yaClientLoginFast(user);

        await bro.url(`${NAVIGATION.albums.url}/${album.id}`);
        await bro.yaAssertUrlInclude(album.id);

        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaWaitForHidden(albums.album2.itemByName().replace(':title', album.lastItemName));

        const loadedItems = await bro.yaScrollAndGetItems(album.itemSelector, listing.listingSpinner());
        await assert.equal(album.length, loadedItems.length, 'Количество файлов отличается от ожидаемого');

        await bro.yaWaitForVisible(albums.album2.itemByName().replace(':title', album.lastItemName));
    });

    it('diskclient-5771, diskclient-5772: Открытие изображения в слайдере в альбоме мест', async function() {
        const bro = this.browser;
        this.testpalmId = await bro.yaIsMobile() ? 'diskclient-5768' : 'diskclient-5766';
        await bro.yaClientLoginFast('yndx-ufo-test-376');

        const album = {
            id: '5e42945b52d01500550a8ca9',
            name: 'Сан-Франциско',
            type: 'геоальбом',
            itemName: '55fc617ba69da.jpg'
        };

        await bro.url(`${NAVIGATION.albums.url}/${album.id}`);
        await bro.yaAssertUrlInclude(album.id);

        await bro.yaWaitAlbumItemsInViewportLoad();
        await bro.yaClick(albums.album2.itemByName().replace(':title', album.itemName));

        await bro.yaWaitForVisible(slider.contentSlider());
        await bro.yaAssertView(this.testpalmId, slider.contentSlider());
    });
});
