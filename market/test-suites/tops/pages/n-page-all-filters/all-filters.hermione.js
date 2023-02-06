import {makeSuite, mergeSuites, prepareSuite} from 'ginny';

import {createFilter, createFilterValue, createProduct, mergeState} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    booleanFilterValues,
    createBooleanFilter,
    createEnumFilter,
    createRadioFilter,
    createRangeFilter,
    enumFilterValues,
    rangeFilterValues,
} from '@self/project/src/spec/hermione/fixtures/filters/all-filters';
import {guruMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';

import {routes} from '@self/platform/spec/hermione/configs/routes';
// suites
import FilterBooleanSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/boolean';
import FilterEnumSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/enum';
import FilterRadioSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/radio';
import FilterWithEnumSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/withFilterEnum';
import FilterRangeSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/range';
import FilterDefaultOpenedSuite from '@self/platform/spec/hermione/test-suites/blocks/n-filter-block/defaultOpened';
// page-objects
import FilterBlock from '@self/platform/spec/page-objects/n-filter-block';
import FilterColors from '@self/platform/spec/page-objects/FilterColors';
import SearchFiltersAside from '@self/platform/spec/page-objects/SearchFiltersAside';
import FilterPanelExtend from '@self/platform/spec/page-objects/n-filter-panel-extend';
import AllFilters from '@self/platform/widgets/content/AllFilters/__pageObject';

// eslint-disable-next-line import/no-commonjs
module.exports = makeSuite('Страница каталога, раздел всех фильтров.', {
    story: mergeSuites(
        makeSuite('Фильтры.', {
            environment: 'kadavr',
            story: mergeSuites(
                makeSuite('Булевый фильтр первого рода.', {
                    story: prepareSuite(FilterBooleanSuite, {
                        meta: {
                            id: 'marketfront-660',
                            issue: 'MARKETVERSTKA-24711',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            queryParamValue: '1',
                            filterKind: 1,
                            filterId: 16261440,
                        },
                        hooks: {
                            async beforeEach() {
                                const booleanFilter = createBooleanFilter('16261440', 1);
                                const filterValues = booleanFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '16261440', filterValue.id));
                                const filter = createFilter(booleanFilter, '16261440');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Булевый фильтр второго рода.', {
                    story: prepareSuite(FilterBooleanSuite, {
                        meta: {
                            id: 'marketfront-665',
                            issue: 'MARKETVERSTKA-24716',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            queryParamValue: '1',
                            filterKind: 2,
                            filterId: 13478167,
                        },
                        hooks: {
                            async beforeEach() {
                                const booleanFilter = createBooleanFilter('13478167', 2);
                                const filterValues = booleanFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '13478167', filterValue.id));
                                const filter = createFilter(booleanFilter, '13478167');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);

                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Булевый фильтр третьего рода.', {
                    story: prepareSuite(FilterBooleanSuite, {
                        params: {
                            queryParamName: 'free-delivery',
                            queryParamValue: '1',
                            filterKind: 2,
                            filterId: 'free-delivery',
                        },
                        hooks: {
                            async beforeEach() {
                                const booleanFilter = createBooleanFilter('free-delivery', 2);
                                const filterValues = booleanFilterValues.map(filterValue =>
                                    createFilterValue(filterValue, 'free-delivery', filterValue.id)
                                );
                                const filter = createFilter(booleanFilter, 'free-delivery');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Списковой фильтр первого рода.', {
                    story: prepareSuite(FilterEnumSuite, {
                        meta: {
                            id: 'marketfront-659',
                            issue: 'MARKETVERSTKA-24710',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '7893318',
                            filterKind: 1,
                        },
                        hooks: {
                            async beforeEach() {
                                const enumFilter = createEnumFilter('7893318', 1);
                                const filterValues = enumFilterValues.map(filterValue =>
                                    createFilterValue(filterValue, '7893318', filterValue.id)
                                );
                                const filter = createFilter(enumFilter, '7893318');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);

                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Списковой фильтр второго рода.', {
                    story: prepareSuite(FilterEnumSuite, {
                        meta: {
                            id: 'marketfront-663',
                            issue: 'MARKETVERSTKA-24714',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '13887626',
                            filterKind: 2,
                        },
                        hooks: {
                            async beforeEach() {
                                const enumFilter = createEnumFilter('13887626', 2);
                                const filterValues = enumFilterValues.map(filterValue =>
                                    createFilterValue(filterValue, '13887626', filterValue.id)
                                );
                                const filter = createFilter(enumFilter, '13887626');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Списковой фильтр третьего рода.', {
                    story: prepareSuite(FilterEnumSuite, {
                        meta: {
                            id: 'MARKETFRONT-17438',
                            issue: 'MARKETVERSTKA-24717',
                        },
                        params: {
                            queryParamName: 'fesh',
                            filterId: 'fesh',
                            filterKind: 2,
                        },
                        hooks: {
                            async beforeEach() {
                                const enumFilter = createEnumFilter('fesh', 2);
                                const filterValues = enumFilterValues.map(filterValue =>
                                    createFilterValue(filterValue, 'fesh', filterValue.id)
                                );
                                const filter = createFilter(enumFilter, 'fesh');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Радио фильтр первого рода.', {
                    story: prepareSuite(FilterRadioSuite, {
                        meta: {
                            id: 'marketfront-662',
                            issue: 'MARKETVERSTKA-24713',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '8555560',
                            filterKind: 1,
                        },
                        hooks: {
                            async beforeEach() {
                                const radioFilter = createRadioFilter('8555560', 1);
                                const filterValues = booleanFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '8555560', filterValue.id));
                                const filter = createFilter(radioFilter, '8555560');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Радио фильтр второго рода.', {
                    story: prepareSuite(FilterRadioSuite, {
                        meta: {
                            id: 'marketfront-672',
                            issue: 'MARKETVERSTKA-24722',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '13800894',
                            filterKind: 2,
                        },
                        hooks: {
                            async beforeEach() {
                                const radioFilter = createRadioFilter('13800894', 2);
                                const filterValues = booleanFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '13800894', filterValue.id));
                                const filter = createFilter(radioFilter, '13800894');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                prepareSuite(FilterWithEnumSuite, {
                    params: {
                        queryParamNameForFilterBlock: 'glfilter',
                        filterBlockId: '4925712',
                        queryParamNameForFilterList: 'glfilter',
                        filterListId: '13887626',
                    },
                    hooks: {
                        beforeEach() {
                            const params = routes.list.phones;
                            return this.browser.yaOpenPage('market:all-filters', params);
                        },
                    },
                    pageObjects: {
                        filterBlock() {
                            return this.createPageObject(
                                FilterBlock,
                                {
                                    root: `${FilterPanelExtend.root} [data-filter-id="4925712"]`,
                                }
                            );
                        },
                        filterList() {
                            return this.createPageObject(
                                FilterColors,
                                {
                                    parent: SearchFiltersAside.root,
                                }
                            );
                        },
                    },
                }),

                makeSuite('Диапазонный фильтр первого рода.', {
                    story: prepareSuite(FilterRangeSuite, {
                        meta: {
                            id: 'marketfront-661',
                            issue: 'MARKETVERSTKA-24712',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '15464317',
                            fromValue: '10',
                            filterKind: 1,
                        },
                        hooks: {
                            async beforeEach() {
                                const rangeFilter = createRangeFilter('15464317', 1);
                                const filterValues = rangeFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '15464317', filterValue.id));
                                const filter = createFilter(rangeFilter, '15464317');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);

                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Диапазонный фильтр второго рода.', {
                    story: prepareSuite(FilterRangeSuite, {
                        meta: {
                            id: 'marketfront-664',
                            issue: 'MARKETVERSTKA-24715',
                        },
                        params: {
                            queryParamName: 'glfilter',
                            filterId: '8257524',
                            fromValue: '10',
                            filterKind: 2,
                        },
                        hooks: {
                            async beforeEach() {
                                const rangeFilter = createRangeFilter('8257524', 2);
                                const filterValues = rangeFilterValues
                                    .map(filterValue => createFilterValue(filterValue, '8257524', filterValue.id));
                                const filter = createFilter(rangeFilter, '8257524');
                                const product = createProduct(guruMock.mock);

                                const dataMixin = {
                                    data: {
                                        search: {
                                            total: 1,
                                            totalOffers: 1,
                                        },
                                    },
                                };

                                const state = mergeState([
                                    product,
                                    filter,
                                    dataMixin,
                                    ...filterValues,
                                ]);
                                await this.browser.setState('report', state);
                                const params = routes.list.phones;
                                return this.browser.yaOpenPage('market:all-filters', params);
                            },
                        },
                    }),
                }),

                makeSuite('Состояние фильтров.', {
                    environment: 'testing',
                    story: prepareSuite(FilterDefaultOpenedSuite, {
                        meta: {
                            id: 'marketfront-1360',
                            issue: 'MARKETVERSTKA-26105',
                        },
                        params: {
                            checkedFilterId: 7893318,
                            checkedFilterValue: 153043,
                            // TODO: MARKETFRONT-11516 - раскомментировать id
                            openedFilterIds: ['glprice', 7893318, 13887626, /* 12782797, */ 'offer-shipping'],
                        },
                        hooks: {
                            beforeEach() {
                                return this.browser.yaOpenPage('market:all-filters', {
                                    slug: 'mobilnye-telefony',
                                    nid: 54726,
                                    glfilter: `${this.params.checkedFilterId}:${this.params.checkedFilterValue}`,
                                });
                            },
                        },
                        pageObjects: {
                            filterPanel() {
                                return this.createPageObject(AllFilters);
                            },
                        },
                    }),
                })
            ),
        })
    ),
});
