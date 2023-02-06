const { NAVIGATION } = require('../config').consts;

describe('Фотосрез 2 -> ', () => {
    beforeEach(async function() {
        const bro = this.browser;
        await bro.yaClientLoginFast('yndx-ufo-test-44');
    });

    hermione.only.in('search-app');
    it('diskclient-4325: ПП. Ссылка на диск', async function() {
        const bro = this.browser;
        this.testpalmId = 'diskclient-4325';

        await bro.url(NAVIGATION.photo.searchAppUrl);
        await bro.yaSetPhotoSliceListingType('tile');
        await bro.yaWaitPhotoSliceItemsInViewportLoad();
        await bro.yaAssertView(this.testpalmId, 'body');
    });
});
