import {prepareSuite, makeSuite, mergeSuites} from 'ginny';
import {
    mergeState,
    createFilter,
    createFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';
// suites
import ProductFiltersEmbeddedColorSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductFiltersEmbedded/color';
import ProductFiltersEmbeddedSkuSuite from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductFiltersEmbedded/sku';
import QuickFiltersWithSkuOffers from
    '@self/platform/spec/hermione/test-suites/blocks/widgets/content/ProductFiltersEmbedded/withoutSkuWithSkuOffers';
// page-objects
import VisualEnumFilter from '@self/platform/containers/VisualEnumFilter/_pageObject/';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';
import FilterCompound from '@self/platform/components/FilterCompound/__pageObject';
import Filters from '@self/platform/components/Filters/__pageObject';
import ProductOffersSnippet from '@self/platform/spec/page-objects/components/ProductOffersSnippet';
import BreadcrumbsUnified from '@self/platform/spec/page-objects/BreadcrumbsUnified';

import {phoneProductRoute, productWithPicture} from '@self/platform/spec/hermione/fixtures/product';


import {
    buildProductOffersResultsState,
    buildColorFilter,
    createFilterIds,
    COLOR_FILTER_ID,
    ENUM_FILTER_ID,
    enumFilterValuesMock,
    enumFilterMock,
} from './fixtures/productWithVisualFilters';

export default makeSuite('Взаимодействие с фильтрами.', {
    environment: 'kadavr',
    feature: 'Фильтры',
    story: mergeSuites(
        prepareSuite(ProductFiltersEmbeddedColorSuite, {
            params: {
                filterId: COLOR_FILTER_ID,
            },

            hooks: {
                async beforeEach() {
                    const filterValueIds = createFilterIds(15);
                    const colorFilter = buildColorFilter(filterValueIds);

                    this.params.applyFilterValueId = filterValueIds[0];

                    const state = mergeState([
                        buildProductOffersResultsState(),
                        colorFilter,
                    ]);

                    await this.browser.setState('report', state);

                    return this.browser.yaOpenPage('touch:product', phoneProductRoute);
                },
            },
            pageObjects: {
                visualFilter() {
                    return this.createPageObject(VisualEnumFilter);
                },
                filterPopup() {
                    return this.createPageObject(FilterPopup);
                },
                selectFilter() {
                    return this.createPageObject(SelectFilter);
                },
            },
        }),
        prepareSuite(ProductFiltersEmbeddedSkuSuite, {
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
                selectFilter() {
                    return this.createPageObject(SelectFilter);
                },
            },
        }),
        prepareSuite(QuickFiltersWithSkuOffers, {
            params: {},
            hooks: {
                async beforeEach() {
                    const filterValueIds = createFilterIds(15);
                    this.params.applyFilterValueIds = [
                        String(filterValueIds[0]),
                        enumFilterValuesMock[1].value,
                    ];
                    const colorFilter = buildColorFilter(filterValueIds, id =>
                        (id < 3 ? {marketSku: String(id)} : {})
                    );

                    const product = productWithPicture;
                    this.params.initialPageUrl = phoneProductRoute;

                    const enumFilterValues = enumFilterValuesMock.map((value, i) => {
                        const id = String(i + 1);
                        return createFilterValue({...value, marketSku: id}, ENUM_FILTER_ID, id);
                    });

                    const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));
                    const state = mergeState([
                        buildProductOffersResultsState(),
                        colorFilter,
                        enumFilter,
                        ...enumFilterValues,
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
                    return this.createPageObject(VisualEnumFilter, {
                        root: `[data-autotest-filter-id="${COLOR_FILTER_ID}"]`,
                    });
                },
                typeEnumFilter() {
                    return this.createPageObject(VisualEnumFilter, {
                        root: `[data-autotest-filter-id="${ENUM_FILTER_ID}"]`,
                    });
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
                breadcrumbsUnified() {
                    return this.createPageObject(BreadcrumbsUnified);
                },
                offerSnippet() {
                    return this.createPageObject(ProductOffersSnippet);
                },
            },
        })
    ),
});
