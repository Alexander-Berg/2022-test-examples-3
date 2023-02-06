import {prepareSuite, makeSuite, mergeSuites} from 'ginny';

// testSuites
import ProductCompareListsSuite from '@self/platform/spec/hermione/test-suites/blocks/ProductCompareLists';

// pageObjects
import RegionPopup from '@self/platform/spec/page-objects/widgets/parts/RegionPopup';
import ProductCompareLists from '@self/platform/spec/page-objects/ProductCompareLists';
import CompareDegradationSuite from '@self/project/src/spec/hermione/test-suites/blocks/Compare/degradation';
import ComparePage from '@self/platform/widgets/pages/CompareListsPage/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Cравнение товаров.', {
    environment: 'testing',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.setPageObjects({
                    regionPopup() {
                        return this.createPageObject(RegionPopup);
                    },
                });
                return this.browser
                    .yaOpenPage('touch:compare-lists')
                    .then(() => this.browser.yaClosePopup(this.regionPopup));
            },
        },
        makeSuite('Незалогиннный пользователь.', {
            story: mergeSuites(
                prepareSuite(ProductCompareListsSuite, {
                    pageObjects: {
                        productCompareLists() {
                            return this.createPageObject(ProductCompareLists);
                        },
                    },
                    hooks: {
                        beforeEach() {
                            return this.browser
                                .yaOpenPage('touch:compare-lists')
                                .then(() => this.browser.yaClosePopup(this.regionPopup));
                        },
                    },
                })

            ),
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
});
