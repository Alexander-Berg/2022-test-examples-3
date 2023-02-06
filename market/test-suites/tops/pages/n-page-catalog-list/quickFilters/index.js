import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

// page-objects
import QuickFilters from '@self/root/src/widgets/content/search/QuickFilters/__pageObject';

// suites
import QuickFiltersBoolean from '@self/platform/spec/hermione/test-suites/blocks/QuickFilters/Boolean';
import QuickFilterBoolean from '@self/root/src/components/QuickFilters/Boolean/__pageObject';
import QuickFilterButton from '@self/root/src/components/QuickFilters/QuickFilterButton/__pageObject';


export default makeSuite('Быстрофильтры.', {
    environment: 'kadavr',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    quickFilters: () => this.createPageObject(QuickFilters),
                });
            },
        },
        prepareSuite(QuickFiltersBoolean, {
            pageObjects: {
                quickFilterBoolean() {
                    return this.createPageObject(QuickFilterBoolean);
                },
                quickFilterButton() {
                    return this.createPageObject(QuickFilterButton, {
                        parent: this.quickFilterBoolean,
                    });
                },
            },
        })
    ),
});
