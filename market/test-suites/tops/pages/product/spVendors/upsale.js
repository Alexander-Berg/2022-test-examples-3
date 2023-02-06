import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import BetterChoiceSpecsSuite from '@self/platform/spec/hermione/test-suites/blocks/BetterChoice/specs';
import BetterChoiceLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/BetterChoice/link';

// page-objects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import BetterChoice from '@self/platform/components/BetterChoice/__pageObject';

import spVendorsMock from '@self/platform/spec/hermione/fixtures/sp-vendors/upsale';


import {
    S3_API_VERSION,
} from '@self/root/src/resources/s3mds/fetchSpecVendorsConfig/constants';

export default makeSuite('Апсейл.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('S3Mds.files', {
                    [`/sp-vendor/${S3_API_VERSION}/public/touch/upsale.json`]: spVendorsMock.instrument.upsale,
                });
                await this.browser.setState('report', spVendorsMock.report);

                await this.browser.yaOpenPage('touch:product', spVendorsMock.route);
                await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
            },
        },

        prepareSuite(BetterChoiceSpecsSuite, {
            pageObjects: {
                betterChoice() {
                    return this.createPageObject(BetterChoice);
                },
            },
            params: {
                specs: spVendorsMock.commonParams.customSpec,
            },
        }),

        prepareSuite(BetterChoiceLinkSuite, {
            pageObjects: {
                betterChoice() {
                    return this.createPageObject(BetterChoice);
                },
            },
            params: {
                routeName: 'touch:product',
                routeParams: spVendorsMock.targetRoute,
            },
        })
    ),
});
