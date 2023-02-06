import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from '@yandex-market/ginny';

import {PLACEHOLDER} from '@self/root/src/constants/placeholder';

import {
    HeaderSearchSuite,
} from '@self/platform/spec/hermione2/test-suites/blocks/HeaderSearch';

import HeaderSearchPO from '@self/platform/widgets/content/HeaderSearch/__pageObject';

export default makeSuite('Детали', {
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.yaSetValue(this.headerSearch.input, 'iphone');
                await this.headerSearch.submitButton.click();
            },
        },
        prepareSuite(HeaderSearchSuite, {
            pageObjects: {
                headerSearch() {
                    return this.browser.createPageObject(HeaderSearchPO);
                },
            },
            params: {
                expectedWithChip: true,
                expectedChipText: 'Express',
                expectedPlaceholder: PLACEHOLDER.EXPRESS,
            },
        })
    ),
});
