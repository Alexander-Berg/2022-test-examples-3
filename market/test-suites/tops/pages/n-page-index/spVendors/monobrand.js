import {prepareSuite, makeSuite} from 'ginny';

// suites
import ScrollBoxWidgetTitleTextSuite from '@self/platform/spec/hermione/test-suites/blocks/ScrollBoxWidget/title-text';

// page-objects
import ScrollBoxWidget from '@self/platform/spec/page-objects/ScrollBoxWidget';

import {
    S3_API_VERSION,
} from '@self/root/src/resources/s3mds/fetchSpecVendorsConfig/constants';

// mocks
import spVendorsMock from '@self/platform/spec/hermione/fixtures/sp-vendors/monobrand';
import indexPageMock from '../fixtures/index-page';


export default makeSuite('Монобренд.', {
    environment: 'kadavr',
    story: prepareSuite(ScrollBoxWidgetTitleTextSuite, {
        pageObjects: {
            scrollBoxWidget() {
                return this.createPageObject(
                    ScrollBoxWidget,
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

                await this.browser.yaOpenPage('market:index');
            },
        },
        params: {
            titleText: spVendorsMock.commonParams.titleText,
        },
    }),
});
