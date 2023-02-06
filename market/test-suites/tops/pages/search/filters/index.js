import {makeSuite, mergeSuites, prepareSuite} from 'ginny';
import QuickCpaFilter from '@self/platform/widgets/content/QuickFilters/__pageObject/cpaFilter';
import SearchOptions from '@self/platform/spec/page-objects/SearchOptions';
import SearchSnippet from '@self/platform/spec/page-objects/containers/SearchSnippet';
import CpaFilterTumbler from '@self/platform/components/FilterTumbler/__pageObject/cpaFilter';
import QuickFilters from '@self/platform/widgets/content/QuickFilters/__pageObject';

import QuickCpaFilterSuite from './quickCpaFilter';

export default makeSuite('Взаимодействие с фильтрами', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: {
        'Быстрые фильтры': mergeSuites(
            prepareSuite(QuickCpaFilterSuite, {
                pageObjects: {
                    quickCpaFilter() {
                        return this.createPageObject(QuickCpaFilter);
                    },
                    cpaFilter() {
                        return this.createPageObject(CpaFilterTumbler);
                    },
                    searchOptions() {
                        return this.createPageObject(SearchOptions);
                    },
                    snippet() {
                        return this.createPageObject(SearchSnippet, {
                            parent: this.searchResults,
                        });
                    },
                    quickFilters() {
                        return this.createPageObject(QuickFilters);
                    },
                },
                params: {},
            })
        ),
    },
});
