const { listing, listingBody } = require('../page-objects/client-content-listing').common;
const { albums2 } = require('../page-objects/client-albums-page');

describe('Редирект в последний посещённый раздел', () => {
    it('diskclient-5277, diskclient-5492: Редирект в последний посещенный раздел "Последние"', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-235');
        await bro.yaSaveSettings('lastContext', '/recent');
        await bro.url('/');

        await bro.yaWaitForVisible(listingBody.items());
        await bro.yaWaitForVisible(listing.head.header() + '[title="Последние файлы"]');
    });

    it('diskclient-5278, diskclient-5493: Редирект в последний посещенный раздел "Фото"', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-236');
        await bro.yaSaveSettings('lastContext', '/photo');
        await bro.url('/');

        await bro.yaWaitPhotoSliceItemsInViewportLoad();
    });

    it('diskclient-5279, diskclient-5533: Редирект в последний посещенный раздел "Альбомы"', async function() {
        const bro = this.browser;

        await bro.yaClientLoginFast('yndx-ufo-test-237');
        await bro.yaSaveSettings('lastContext', '/albums');
        await bro.url('/');

        await bro.yaWaitForVisible(albums2.personal());
    });
});
