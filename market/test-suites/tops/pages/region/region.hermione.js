import {prepareSuite, makeSuite} from 'ginny';

import RegionSelectorSuite from '@self/platform/spec/hermione/test-suites/blocks/RegionSelector';
import RegionSelector from '@self/platform/spec/page-objects/widgets/content/RegionSelector';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница смены региона.', {
    environment: 'testing',
    story: prepareSuite(RegionSelectorSuite, {
        pageObjects: {
            regionSelector() {
                return this.createPageObject(RegionSelector);
            },
        },
    }),
});
