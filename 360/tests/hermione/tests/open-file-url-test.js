const filesForOpenUrlTest = require('../config').filesForOpenUrlTest;
const slider = require('../page-objects/slider').common;
const listing = require('../page-objects/client-content-listing').common;
const assert = require('chai').assert;
const url = require('url');

const doTest = ({ fileUrl, sectionUrl, sectionName, touchId, desktopId }) => {
    describe('Переход к конкретному файлу по прямому урлу  ->', () => {
        it(`diskclient-${touchId}, ${desktopId}: ${sectionName}`, async function() {
            const bro = this.browser;
            const isMobile = await bro.yaIsMobile();
            this.testpalmId = isMobile ? `diskclient-${touchId}` : `diskclient-${desktopId}`;

            await bro.yaClientLoginFast('yndx-ufo-test-69');
            const baseUrl = await bro.getUrl();
            await bro.url(fileUrl);
            await bro.waitForVisible(slider.contentSlider.previewImage());
            await bro.click(slider.sliderButtons.closeButton());
            await bro.yaWaitForHidden(slider.contentSlider.previewImage());
            await bro.waitForVisible(listing.listing());
            if (!isMobile) {
                await bro.yaWaitActionBarDisplayed();
            }
            const currentUrl = await bro.getUrl();
            assert.equal(decodeURIComponent(currentUrl), 'https://' + url.parse(baseUrl).hostname + sectionUrl);
        });
    });
};

filesForOpenUrlTest.forEach(doTest);
