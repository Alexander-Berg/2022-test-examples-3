import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {PRODUCT_ROUTE, productState} from '@self/platform/spec/hermione/fixtures/credit';
// suites
import FiltersCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/credit';
import FiltersInteractionWithCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withCredit';
// page-objects
import FilterRadio from '@self/platform/spec/page-objects/FilterRadio';
import FilterCounter from '@self/platform/spec/page-objects/FilterCounter';

export default makeSuite('Фильтр "Покупка в кредит" в фильтрах.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(FiltersCreditSuite, {
            meta: {
                id: 'marketfront-3498',
                issue: 'MARKETVERSTKA-34616',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        productState
                    );

                    return this.browser.yaOpenPage('market:product-offers', PRODUCT_ROUTE);
                },
            },
            pageObjects: {
                filterList() {
                    return this.createPageObject(FilterRadio);
                },
            },
        }),
        prepareSuite(FiltersInteractionWithCreditSuite, {
            meta: {
                id: 'marketfront-3499',
                issue: 'MARKETVERSTKA-34618',
            },
            params: {
                place: 'productoffers',
            },
            pageObjects: {
                filterList() {
                    return this.createPageObject(FilterRadio);
                },
                filterCounter() {
                    return this.createPageObject(FilterCounter);
                },
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        productState
                    );

                    return this.browser.yaOpenPage('market:product-offers', PRODUCT_ROUTE);
                },
            },
        })
    ),
});
