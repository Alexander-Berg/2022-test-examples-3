import {prepareSuite, mergeSuites, makeSuite} from 'ginny';

import HeadlineSuite from '@self/platform/spec/hermione/test-suites/blocks/widgets/parts/Headline';
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import Headline from '@self/platform/spec/page-objects/widgets/parts/Headline';

export default makeSuite('Франшиза', {
    story: mergeSuites(
        prepareSuite(HeadlineSuite, {
            hooks: {
                async beforeEach() {
                    await this.browser.yaOpenPage('touch:bvl-franchise', {
                        brandId: 15294598,
                        slug: 'temnaia-storona',
                    });
                    await this.browser.yaClosePopup(this.createPageObject(RegionPopup));
                },
            },
            pageObjects: {
                headline() {
                    return this.createPageObject(Headline);
                },
            },
        })
    ),
});
