import {
    makeSuite,
    mergeSuites,
    prepareSuite,
} from '@yandex-market/ginny';

import {PAGE_IDS_COMMON} from '@self/root/src/constants/pageIds/index.market.desktop';
import {PLACEHOLDER} from '@self/root/src/constants/placeholder';
import {EXPRESS_ROOT_NAVNODE_ID} from '@self/root/src/constants/express';
import {rootExpress} from '@self/root/src/spec/hermione/kadavr-mock/cataloger/navigationPathExpress';

// Suites
import {
    HeaderSearchSuite,
    HeaderSearchExpressSuite,
} from '@self/platform/spec/hermione2/test-suites/blocks/HeaderSearch';

// PageObjects
import HeaderSearchPO from '@self/platform/widgets/content/HeaderSearch/__pageObject';

import {
    departmentPageMock,
} from './fixtures';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Экспресс', {
    environment: 'kadavr',
    feature: 'express',
    story: {
        'Поиск.': mergeSuites(
            prepareSuite(HeaderSearchSuite, {
                meta: {
                    id: 'marketfront-5080',
                    issue: 'MARKETFRONT-54423',
                },
                hooks: {
                    async beforeEach() {
                        await this.browser.setState('Cataloger.tree', rootExpress);
                        await this.browser.setState('Tarantino.data.result', [departmentPageMock]);

                        return this.browser.yaOpenPage(PAGE_IDS_COMMON.CATALOG, {
                            nid: EXPRESS_ROOT_NAVNODE_ID,
                            slug: 'express',
                        });
                    },
                },

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

        'На странице.': mergeSuites(
            prepareSuite(HeaderSearchExpressSuite, {
                meta: {
                    id: 'marketfront-5081',
                    issue: 'MARKETFRONT-54423',
                },
                hooks:
                    {
                        async beforeEach() {
                            await this.browser.setState('Cataloger.tree', rootExpress);
                            await this.browser.setState('Tarantino.data.result', [departmentPageMock]);

                            return this.browser.yaOpenPage(PAGE_IDS_COMMON.CATALOG, {
                                nid: EXPRESS_ROOT_NAVNODE_ID,
                                slug: 'express',
                            });
                        },
                    },
                pageObjects: {
                    headerSearch() {
                        return this.browser.createPageObject(HeaderSearchPO);
                    },
                },
            })
        ),
    },
});
