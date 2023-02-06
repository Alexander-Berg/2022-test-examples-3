import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {routes} from '@self/platform/spec/hermione/configs/routes';
import {searchResultsWithProductState} from '@self/platform/spec/hermione/fixtures/credit';
// suites
import FiltersCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/Filters/credit';
import WithCreditSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withCredit';
import WithCreditSearchParamSuite from '@self/platform/spec/hermione/test-suites/blocks/FiltersInteraction/withCreditSearchParam';
// page-objects
import FilterRadio from '@self/platform/spec/page-objects/FilterRadio';
import FilterCounter from '@self/platform/spec/page-objects/FilterCounter';
import SnippetCell2 from '@self/project/src/components/Search/Snippet/Cell/__pageObject';

export default makeSuite('Фильтр «Покупка в кредит» в фильтрах.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.setState(
                    'report',
                    searchResultsWithProductState
                );

                return this.browser.yaOpenPage('market:list', {viewtype: 'grid', ...routes.list.phones});
            },
        },
        prepareSuite(FiltersCreditSuite, {
            meta: {
                id: 'marketfront-3502',
                issue: 'MARKETVERSTKA-34622',
            },
            pageObjects: {
                filterList() {
                    return this.createPageObject(FilterRadio);
                },
            },
        }),

        prepareSuite(WithCreditSuite, {
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
        }),

        prepareSuite(WithCreditSearchParamSuite, {
            meta: {
                id: 'marketfront-3504',
                issue: 'MARKETVERSTKA-34621',
            },
            pageObjects: {
                filterList() {
                    return this.createPageObject(FilterRadio);
                },
                filterCounter() {
                    return this.createPageObject(FilterCounter);
                },
                snippet() {
                    return this.createPageObject(SnippetCell2);
                },
            },
        })
    ),
});

