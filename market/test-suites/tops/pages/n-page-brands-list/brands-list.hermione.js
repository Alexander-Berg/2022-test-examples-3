import {makeSuite, prepareSuite, mergeSuites} from 'ginny';

// suites
import AlphabetsByDefaultSuite from '@self/platform/spec/hermione/test-suites/blocks/n-alphabets/byDefault';
import AlphabetsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-alphabets';
import AlphabetsNumSuite from '@self/platform/spec/hermione/test-suites/blocks/n-alphabets/__num';
import PopularBrandsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-popular-brands';
import AllBrandsSuite from '@self/platform/spec/hermione/test-suites/blocks/n-w-all-brands';
// page-objects
import PopularBrandsReact from '@self/platform/spec/page-objects/components/PopularBrands';
import BrandsGlossary from '@self/platform/spec/page-objects/components/BrandsGlossary';
import BrandsLettersFilter from '@self/platform/spec/page-objects/components/BrandsLettersFilter';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница списка брендов.', {
    environment: 'testing',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    alphabets: () => this.createPageObject(BrandsLettersFilter),
                });

                return this.browser.yaOpenPage('market:brands-list');
            },
        },

        prepareSuite(AlphabetsByDefaultSuite),
        prepareSuite(AlphabetsSuite),
        prepareSuite(AlphabetsNumSuite),


        prepareSuite(PopularBrandsSuite, {
            pageObjects: {
                popularBrands() {
                    return this.createPageObject(PopularBrandsReact);
                },
            },
        }),

        prepareSuite(AllBrandsSuite, {
            pageObjects: {
                allBrands() {
                    return this.createPageObject(BrandsGlossary);
                },
            },
        })
    ),
});
