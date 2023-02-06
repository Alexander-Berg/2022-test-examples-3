import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// suites
import CompareHeadSuite from '@self/platform/spec/hermione/test-suites/blocks/n-compare-head';
import CompareShowControlsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-compare-show-controls';
import CompareOffersLinkSuite from '@self/platform/spec/hermione/test-suites/blocks/n-compare-offers-link';

// page-objects
import CompareHead from '@self/platform/widgets/content/compare/Content/CompareHead/__pageObject';
import CompareToolbar from '@self/platform/widgets/content/compare/Toolbar/__pageObject';
import CompareRow from '@self/platform/widgets/content/compare/Content/CompareRow/__pageObject';
import CompareContent from '@self/platform//widgets/content/compare/Content/__pageObject';
import CompareCell from '@self/platform/widgets/content/compare/Content/CompareCell/__pageObject';

import CompareDegradationSuite from '@self/project/src/spec/hermione/test-suites/blocks/Compare/degradation';
import ComparePage from '@self/platform/widgets/pages/ComparePage/__pageObject';

import {comparisonsMock, catalogerMock, reportMock} from './mock';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница сравнения товаров.', {
    story: mergeSuites(
        makeSuite('Блок сравнения товаров.', {
            story: mergeSuites(
                {
                    async beforeEach() {
                        await Promise.all([
                            this.browser.setState('Cataloger.tree', catalogerMock),
                            this.browser.setState('report', reportMock),
                            this.browser.setState(
                                'persComparison',
                                {data: {comparisons: comparisonsMock}}
                            ),
                        ]);
                        return this.browser.yaOpenPage('market:compare');
                    },
                },
                prepareSuite(CompareHeadSuite, {
                    pageObjects: {
                        compareHead() {
                            return this.createPageObject(CompareHead);
                        },
                        compareCell() {
                            return this.createPageObject(CompareCell, {
                                parent: this.compareHead,
                                root: `${CompareCell.root}:nth-child(1)`,
                            });
                        },
                    },
                }),
                prepareSuite(CompareShowControlsSuite, {
                    pageObjects: {
                        compareShowControls() {
                            return this.createPageObject(CompareToolbar);
                        },
                        compareContent() {
                            return this.createPageObject(CompareContent, {
                                parent: this.compareShowControls.parent,
                            });
                        },
                    },
                }),
                prepareSuite(CompareOffersLinkSuite, {
                    pageObjects: {
                        compareRow() {
                            return this.createPageObject(CompareRow, {
                                root: CompareContent.root,
                            });
                        },
                    },
                }),
                prepareSuite(CompareDegradationSuite, {
                    environment: 'kadavr',
                    pageObjects: {
                        comparePage() {
                            return this.createPageObject(ComparePage);
                        },
                    },
                })
            ),
        })
    ),
});
