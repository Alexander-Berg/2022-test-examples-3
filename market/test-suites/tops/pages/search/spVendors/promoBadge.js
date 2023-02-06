import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';

// suites
// import PromoBadgePopupSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoBadge/popup';
import PromoBadgeSuite from '@self/platform/spec/hermione/test-suites/blocks/PromoBadge/badge';

// page-objects
import SnippetTag from '@self/project/src/components/SnippetTags/SnippetTag/__pageObject';

import spVendorsMock from '@self/platform/spec/hermione/fixtures/sp-vendors/promoBadge';

import {
    S3_API_VERSION,
} from '@self/root/src/resources/s3mds/fetchSpecVendorsConfig/constants';

export default makeSuite('Бейдж.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState('S3Mds.files', {
                    [`/sp-vendor/${S3_API_VERSION}/public/touch/promo_badge.json`]: spVendorsMock.instrument.promo_badge,
                });
                await this.browser.setState('report', spVendorsMock.report);

                await this.browser.yaOpenPage('touch:list', routes.catalog.phones);
            },
        },

        prepareSuite(PromoBadgeSuite, {
            pageObjects: {
                vendorTagBadge() {
                    return this.createPageObject(SnippetTag);
                },
            },
            params: {
                badgeText: spVendorsMock.commonParams.text,
            },
        })

        // TODO Клик по бейджу не работает
        // prepareSuite(PromoBadgePopupSuite, {
        //     pageObjects: {
        //         vendorTagBadge() {
        //             return this.createPageObject(SnippetTag);
        //         },
        //     },
        //     params: {
        //         link: spVendorsMock.commonParams.link,
        //     },
        // })
    ),
});
