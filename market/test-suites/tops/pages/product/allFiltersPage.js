import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    mergeState,
    createFilter,
    createFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// suites
import ProductAllFiltersPageWithSkuSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductFiltersEmbedded/skuFilters';
import ProductOffersFiltersEmbeddedSkuSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductFiltersEmbedded/skuOffers';
import {createRadioFilter, booleanFilterValuesChecked} from '@self/project/src/spec/hermione/fixtures/filters/all-filters';
// page-objects
import VisualEnumFilter from '@self/platform/containers/VisualEnumFilter/_pageObject/';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';
import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';

import {phoneProductRoute, productWithPicture} from '@self/platform/spec/hermione/fixtures/product';


import {
    buildProductOffersResultsState,
    buildColorFilter,
    createFilterIds,
    COLOR_FILTER_ID,
} from './fixtures/productWithVisualFilters';

export default makeSuite('Взаимодействие с фильтрами.', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: mergeSuites(
        prepareSuite(ProductAllFiltersPageWithSkuSuite, {
            params: {},
            hooks: {
                async beforeEach() {
                    const filterValueIds = createFilterIds(15);
                    const colorFilter = buildColorFilter(filterValueIds, id => (
                        id < 3 ? {marketSku: id} : {}
                    ));
                    this.params.applyFilterValueIds = [
                        filterValueIds[0],
                        filterValueIds[1],
                        filterValueIds[5],
                    ];

                    const product = productWithPicture;
                    this.params.initialPageUrl = phoneProductRoute;

                    const state = mergeState([
                        buildProductOffersResultsState(),
                        colorFilter,
                        product,
                    ]);

                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                visualEnumFilter() {
                    return this.createPageObject(VisualEnumFilter);
                },
                filterPopup() {
                    return this.createPageObject(FilterPopup);
                },
                filters() {
                    return this.createPageObject(Filters);
                },
                selectFilter() {
                    return this.createPageObject(SelectFilter, {
                        parent: this.filterPopup,
                    });
                },
                filterCompound() {
                    return this.createPageObject(FilterCompound, {
                        root: `[data-autotest-id="${COLOR_FILTER_ID}"]`,
                    });
                },
            },
        }),
        prepareSuite(ProductOffersFiltersEmbeddedSkuSuite, {
            params: {},
            hooks: {
                async beforeEach() {
                    const filterValueIds = createFilterIds(2);
                    const selectedFilterId = filterValueIds[0];
                    const colorFilter = buildColorFilter(filterValueIds, id => {
                        if (id === selectedFilterId) {
                            return {marketSku: id, checked: true};
                        }
                        return {};
                    });
                    const product = productWithPicture;

                    this.params.initialPageUrl = phoneProductRoute;

                    const RADIO_FILTER_ID1 = '5085139';
                    const RADIO_FILTER_ID2 = '5085138';

                    const radioFilter = createRadioFilter(RADIO_FILTER_ID1);
                    const filterValues = booleanFilterValuesChecked
                        .map(filterValue => createFilterValue(filterValue, RADIO_FILTER_ID1, filterValue.id));

                    const filter = createFilter(radioFilter, RADIO_FILTER_ID1);

                    const radioFilter2 = createRadioFilter(RADIO_FILTER_ID2);

                    const filterValues2 = booleanFilterValuesChecked
                        .map(filterValue => createFilterValue(filterValue, RADIO_FILTER_ID2, filterValue.id));

                    const filter2 = createFilter(radioFilter2, RADIO_FILTER_ID2);

                    const state = mergeState([
                        buildProductOffersResultsState(),
                        colorFilter,
                        product,
                        filter,
                        filter2,
                        ...filterValues2,
                        ...filterValues,
                    ]);

                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('touch:product-offers', phoneProductRoute);
                },
                async afterEach() {
                    return this.browser.yaLogout();
                },
            },
            pageObjects: {
                visualEnumFilter() {
                    return this.createPageObject(VisualEnumFilter);
                },
                filterPopup() {
                    return this.createPageObject(FilterPopup);
                },
                filters() {
                    return this.createPageObject(Filters);
                },
                selectFilter() {
                    return this.createPageObject(SelectFilter, {
                        parent: this.filterPopup,
                    });
                },
                filterCompound() {
                    return this.createPageObject(FilterCompound, {
                        root: `[data-autotest-id="${COLOR_FILTER_ID}"]`,
                    });
                },
            },
        })
    ),
});
