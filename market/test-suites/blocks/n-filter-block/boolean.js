import {makeCase, makeSuite} from 'ginny';
import {
    createFilter,
    createFilterValue,
    createProduct,
    mergeState,
    priceSort,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeFilterParamValue} from '@self/platform/spec/hermione/helpers/filters';
import FilterCheckbox from '@self/platform/components/FilterCheckbox/__pageObject';
import AllFilters from '@self/platform/widgets/content/AllFilters/__pageObject';

import {
    booleanFilterValuesChecked,
    createBooleanFilter,
} from '@self/project/src/spec/hermione/fixtures/filters/all-filters';
import {guruMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';

/**
 * Тесты на блок Boolean фильтра
 * @property {PageObject.FilterBlock} this.filterCheckbox
 * @property {PageObject.FilterPanelExtend} this.filterPanel
 */

export default makeSuite('Булевый фильтр.', {
    feature: 'Фильтр типа «BOOLEAN».',
    story: {
        before() {
            this.setPageObjects({
                filterPanel: () => this.createPageObject(AllFilters),
                filterCheckbox: () => this.createPageObject(FilterCheckbox, {
                    parent: this.filterPanel,
                }),
            });
        },
        'При применении': {
            'переходим на выдачу, отфильтрованную согласно выбранному фильтру': makeCase({
                id: 'marketfront-667',
                issue: 'MARKETVERSTKA-24718',
                environment: 'kadavr',
                params: {
                    queryParamName: 'Query-параметр для данного фильтра',
                    queryParamValue: 'Значение query-параметра, когда фильтр применен',
                    filterKind: 'Род фильтра',
                },
                test() {
                    const {queryParamName, queryParamValue, filterKind, filterId} = this.params;

                    const checkForQueryParamPresence = expectedFilterParamValue =>
                        this.browser
                            .yaParseUrl()
                            .then(({query}) =>
                                this.expect(query)
                                    .to.have.property(
                                        queryParamName,
                                        expectedFilterParamValue,
                                        `Присутствует параметр ${queryParamName} со значением ${expectedFilterParamValue}`
                                    )
                            );

                    return this.filterCheckbox.click()
                        .then(() => {
                            const booleanFilter = createBooleanFilter(filterId, filterKind);
                            const filterValues = booleanFilterValuesChecked
                                .map(filterValue => createFilterValue(filterValue, filterId, filterValue.id));
                            const filter = createFilter(booleanFilter, filterId);
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
                                priceSort,
                                dataMixin,
                                ...filterValues,
                            ]);

                            return this.browser.setState('report', state);
                        })
                        .then(() => this.filterPanel.clickApplyButton())
                        .then(() => checkForQueryParamPresence(
                            makeFilterParamValue({
                                id: filterId,
                                name: queryParamName,
                                value: queryParamValue,
                            }))
                        );
                },
            }),
        },
    },
});
