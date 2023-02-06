import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from 'ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds';
import {commonParams} from '@self/root/src/spec/hermione/configs/params';

import degradationCommon from '@self/platform/spec/hermione/test-suites/blocks/degradation/common';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Список акций', {
    params: {
        ...commonParams.description,
    },
    defaultParams: {
        ...commonParams.value,
    },
    story: mergeSuites(
        prepareSuite(degradationCommon, {
            suiteName: 'Авторизованный пользователь. Деградация.',
            params: {
                isAuthWithPlugin: true,
            },
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.DEALS);
                },
            },
        }),
        prepareSuite(degradationCommon, {
            suiteName: 'Неавторизованный пользователь. Деградация.',
            hooks: {
                beforeEach() {
                    return this.browser.yaOpenPage(PAGE_IDS_COMMON.DEALS);
                },
            },
        })
    ),
});
