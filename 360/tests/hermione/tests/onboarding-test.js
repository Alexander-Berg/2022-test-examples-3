const clientCommon = require('../page-objects/client-common');
const contentListing = require('../page-objects/client-content-listing');
const clientNavigation = require('../page-objects/client-navigation');
const { consts } = require('../config');

const TEST_ID = '?test-id=375300';

describe('Онбординг ->', () => {
    hermione.auth.createAndLogin();
    hermione.skip.notIn('', 'надо придумать как организовать пользователя – https://st.yandex-team.ru/CHEMODAN-79676');
    it('diskclient-6312: Отображение онбординга совместного редактирования документов', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6312-touch' : 'diskclient-6312';

        await bro.url(consts.NAVIGATION.recent.url);
        await bro.yaSkipWelcomePopup();
        await bro.yaClosePromoNotification();

        await bro.url(consts.NAVIGATION.disk.url + TEST_ID);
        await bro.refresh();

        await bro.yaWaitForVisible(clientCommon.common.shareEditOnboarding());
        await bro.yaWaitPreviewsLoaded(clientCommon.common.shareEditOnboarding.image());

        await bro.yaAssertView(this.testpalmId, clientCommon.common.shareEditOnboarding(), {
            invisibleElements: isMobile ?
                [contentListing.common.listing.item.time(), contentListing.common.listing.item.date()] :
                [],
            ignoreElements: isMobile ?
                [] :
                [clientNavigation.desktop.infoSpaceButton()]
        });
    });
});
