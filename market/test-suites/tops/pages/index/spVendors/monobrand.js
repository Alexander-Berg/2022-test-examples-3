import {makeSuite, prepareSuite} from 'ginny';

import ScrollBoxSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox';
// page-objects
import ScrollBox from '@self/platform/spec/page-objects/ScrollBox';
// mocks
import indexPageMock from '@self/platform/spec/hermione/test-suites/blocks/ScrollBox/fixtures/index-page';
import spVendorsMock from '@self/platform/spec/hermione/fixtures/sp-vendors/monobrand';

import {
    S3_API_VERSION,
} from '@self/root/src/resources/s3mds/fetchSpecVendorsConfig/constants';

export default makeSuite('Монобренд.', {
    environment: 'kadavr',
    story: prepareSuite(ScrollBoxSuite, {
        pageObjects: {
            ScrollBox() {
                return this.createPageObject(
                    ScrollBox,
                    {
                        root: `[data-zone-data*="${spVendorsMock.commonParams.titleText}"]`,
                    }
                );
            },
        },
        hooks: {
            async beforeEach() {
                await this.browser.setState('Tarantino.data.result', [indexPageMock]);
                await this.browser.setState('S3Mds.files', {
                    [`/sp-vendor/${S3_API_VERSION}/public/desktop/monobrand.json`]: spVendorsMock.instrument.monobrand,
                });
                await this.browser.setState('report', spVendorsMock.report);

                await this.browser.yaOpenPage('touch:index');
            },
        },
        params: {
            titleText: spVendorsMock.commonParams.titleText,
        },
    }),
});
