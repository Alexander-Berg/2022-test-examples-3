import {makeSuite, mergeSuites, prepareSuite} from '@yandex-market/ginny';
import SearchHeader from '@self/platform/widgets/content/SearchHeader/redesign/__pageObject';
import ClarifyingCategories from '@self/platform/spec/page-objects/ClarifyingCategories';
import ClarifyCategoryCard from '@self/platform/components/ClarifyCategoryCard/__pageObject';

import SearchEverywhereLinkSuite from '@self/platform/spec/hermione2/test-suites/blocks/filters/searchEverywhereLink';
import ResetManyQuickFiltersSuite from '@self/platform/spec/hermione2/test-suites/blocks/filters/resetManyQuickFilters';
import {CHILDREN_GOODS_CATEGORY} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/clarifyingCategories';
import ResetLink from '@self/platform/widgets/content/QuickFilters/components/ResetLink/__pageObject';
import ResetOneQuickFilterSuite from '@self/platform/spec/hermione2/test-suites/blocks/filters/resetOneQuickFilter';
import SelectedQuickFilters from '@self/platform/spec/hermione2/test-suites/blocks/filters/selectedQuickFilters';
import SearchSpellcheck from '@self/platform/widgets/content/SearchSpellcheck/__pageObject';
import QuickFilters from '@self/platform/widgets/content/QuickFilters/__pageObject';
import SpellCheckerSuite from '@self/platform/spec/hermione2/test-suites/blocks/filters/spellchecker';

const options = {
    pageObjects: {
        searchHeader() {
            return this.browser.createPageObject(SearchHeader);
        },
        сlarifyingCategories() {
            return this.browser.createPageObject(ClarifyingCategories);
        },
        firstSearchClarifyCategoryCard() { return this.browser.createPageObject(ClarifyCategoryCard); },
        resetLink() { return this.browser.createPageObject(ResetLink); },
        quickFilters() { return this.browser.createPageObject(QuickFilters); },
        searchSpellcheck() { return this.browser.createPageObject(SearchSpellcheck); },
    },
    params: {
        text: 'Мобильные телефоны',
        nid: CHILDREN_GOODS_CATEGORY.nid,
        slug: CHILDREN_GOODS_CATEGORY.slug,
    },
};

export default makeSuite('Взаимодействие с фильтрами', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: mergeSuites(
        prepareSuite(SearchEverywhereLinkSuite, options),
        prepareSuite(ResetManyQuickFiltersSuite, options),
        prepareSuite(ResetOneQuickFilterSuite, options),
        prepareSuite(SpellCheckerSuite, options),
        prepareSuite(SelectedQuickFilters, options)
    ),
});
