const { NAVIGATION } = require('../config').consts;
const albums = require('../page-objects/client-albums-page');

describe('Альбомы ->', () => {
    beforeEach(async function() {
        await this.browser.yaClientLoginFast('yndx-ufo-test-228');
    });

    it('diskclient-5511, diskclient-5219: Список альбомов в разделе Альбомы', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5511' : 'diskclient-5219';
        await bro.url(NAVIGATION.albums.url);

        await bro.yaWaitForHidden(albums.albums2.shimmer());
        await bro.yaWaitForVisible(albums.albums2.personal.album());
        await bro.yaWaitPreviewsLoaded(albums.albums2.item.preview());

        await bro.yaAssertView(this.testpalmId, isMobile ? albums.albums2RootContent() : albums.albums2());
    });
});
