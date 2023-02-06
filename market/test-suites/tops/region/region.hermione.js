import {prepareSuite, makeSuite} from '@yandex-market/ginny';

import RegionSelectorSuite from '@self/platform/spec/hermione2/test-suites/blocks/RegionSelector';
import RegionSelector from '@self/platform/spec/page-objects/widgets/content/RegionSelector';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница смены региона.', {
    environment: 'testing',
    story: prepareSuite(RegionSelectorSuite, {
        pageObjects: {
            regionSelector() {
                return this.browser.createPageObject(RegionSelector);
            },
        },
    }),
});
