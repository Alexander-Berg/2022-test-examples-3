import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {searchResultsWithOfferState, searchResultsWithProductState} from '@self/platform/spec/hermione/fixtures/credit';
// suites
import FiltersCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/credit';
import FiltersInteractionWithCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withCredit';
// page-objects
import FilterRadio from '@self/platform/spec/page-objects/FilterRadio';
import FilterCounter from '@self/platform/spec/page-objects/FilterCounter';

export default makeSuite('Фильтр «Покупка в кредит» в фильтрах.', {
    environment: 'kadavr',
    story: mergeSuites(
        prepareSuite(FiltersCreditSuite, {
            meta: {
                id: 'marketfront-3500',
                issue: 'MARKETVERSTKA-34617',
            },
            hooks: {
                async beforeEach() {
                    await this.browser.setState(
                        'report',
                        searchResultsWithOfferState
                    );

                    return this.browser.yaOpenPage('market:search', routes.search.default);
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
                id: 'marketfront-3503',
                issue: 'MARKETVERSTKA-34620',
            },
            params: {
                place: 'blender',
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
                        searchResultsWithProductState
                    );

                    return this.browser.yaOpenPage('market:search', routes.search.default);
                },
            },
        })
    ),
});
