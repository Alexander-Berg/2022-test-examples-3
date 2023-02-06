import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import UnitsCalcSuite from '@self/root/src/spec/hermione/test-suites/blocks/unitsCalc';
import UnitsCalc from '@self/root/src/components/UnitsCalc/__pageObject';

import {UNITINFO_EXPECTED_DOUBLE_TEXT, UNITINFO_EXPECTED_TEXT} from './constants';

export default makeSuite('Апсейл попап с калькулятором упаковок.', {
    story: mergeSuites(
        prepareSuite(UnitsCalcSuite, {
            pageObjects: {
                unitsCalc() {
                    return this.createPageObject(UnitsCalc, {
                        parent: this.productDefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: UNITINFO_EXPECTED_TEXT,
            },
            meta: {
                id: 'marketfront-5765',
                issue: 'MARKETFRONT-79800',
            },
        }),
        prepareSuite(UnitsCalcSuite, {
            suiteName: 'Увеличение элементов в корзине.',
            hooks: {
                async beforeEach() {
                    await this.counterCartButton.increase.click();
                    await this.counterCartButton.waitUntilCounterChanged(1, 2);
                },
            },
            pageObjects: {
                unitsCalc() {
                    return this.createPageObject(UnitsCalc, {
                        parent: this.productDefaultOffer.root,
                    });
                },
            },
            params: {
                expectedText: UNITINFO_EXPECTED_DOUBLE_TEXT,
            },
            meta: {
                id: 'marketfront-5765',
                issue: 'MARKETFRONT-79800',
            },
        })
    ),
});
