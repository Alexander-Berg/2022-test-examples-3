const albums = require('../page-objects/client-albums-page');
const clientNavigation = require('../page-objects/client-navigation');

describe('Альбомы ->', () => {
    it('diskclient-5729, diskclient-5822: Отображение онбординга личных альбомов', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-5822' : 'diskclient-5729';

        await bro.yaClientLoginFast('yndx-ufo-test-396');
        await bro.url('/client/disk');
        await bro.executeAsync((done) => {
            ns.Model.get('settings')
                .save({ key: 'personalAlbumsOnboardingClosed', value: '0' })
                .then(done, done);
        });

        await bro.yaWaitForVisible(albums.onboarding());
        await bro.yaWaitPreviewsLoaded(albums.onboarding.image());

        await bro.yaAssertView(this.testpalmId, albums.onboarding(), {
            ignoreElements: [clientNavigation.desktop.spaceInfoSection.infoSpaceButton()]
        });
    });

    it('diskclient-6334, diskclient-6335: Онбординг альбома "Люди на фото"', async function() {
        const bro = this.browser;
        const isMobile = await bro.yaIsMobile();
        this.testpalmId = isMobile ? 'diskclient-6335' : 'diskclient-6334';

        await bro.yaClientLoginFast('yndx-ufo-test-764');
        await bro.url('/client/disk');
        await bro.executeAsync((done) => {
            ns.Model.get('settings')
                .save({ key: 'facesAlbumsOnboardingClosed', value: '0' })
                .then(done, done);
        });

        await bro.yaWaitForVisible(albums.onboardingFaces());
        await bro.yaWaitPreviewsLoaded(albums.onboardingFaces.image());

        await bro.yaAssertView(this.testpalmId, albums.onboardingFaces(), {
            ignoreElements: [clientNavigation.desktop.infoSpaceButton()]
        });
    });
});
