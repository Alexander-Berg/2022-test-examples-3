import {makeCase, makeSuite} from 'ginny';


import {
    createFilter,
    createFilterValue,
    createProduct,
    mergeState,
    priceSort,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    createRangeFilter,
    rangeFilterValuesChecked,
} from '@self/project/src/spec/hermione/fixtures/filters/all-filters';
import {guruMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';

import AllFilters from '@self/platform/widgets/content/AllFilters/__pageObject';
import CommonSearchFilter from '@self/platform/widgets/content/AllFilters/CommonSearchFilter/__pageObject';
import Accordion from '@self/platform/components/Accordion/__pageObject';
import FilterRange from '@self/platform/components/FilterRange/__pageObject';
import Filter from '@self/root/src/widgets/content/search/Filters/components/Filter/__pageObject';

/**
 * Тесты на блок диапазонного фильтра
 */

export default makeSuite('Диапазонный фильтр.', {
    feature: 'Фильтр типа «RANGE»',
    environment: 'kadavr',
    story: {
        before() {
            this.setPageObjects({
                filterPanel: () => this.createPageObject(AllFilters),
                filterContainer: () => this.createPageObject(CommonSearchFilter, {
                    parent: this.filterPanel,
                }),
                accordion: () => this.createPageObject(Accordion, {
                    parent: this.filterContainer,
                }),
                filterRange: () => this.createPageObject(FilterRange, {
                    parent: this.accordion,
                }),
            });
        },
        'При применении': {
            'переходим на выдачу, отфильтрованную согласно выбранному фильтру': makeCase({
                environment: 'kadavr',
                params: {
                    queryParamName: 'Query-параметр для данного фильтра',
                    filterId: 'Id фильтра',
                    fromValue: 'Значение «От»',
                    filterKind: 'Род фильтра',
                },
                async test() {
                    const {queryParamName, filterId, fromValue, filterKind} = this.params;

                    const openFilterBlock = () =>
                        this.browser.allure.runStep('Открываем фильтр', () =>
                            this.accordion
                                .isOpen()
                                .then(isOpen => {
                                    if (!isOpen) {
                                        return this.accordion.toggle();
                                    }

                                    return undefined;
                                })
                        );

                    const setFromValue = value => this.filterRange.setValue('from', String(value));

                    const checkForQueryParamPresence = () =>
                        this.browser
                            .yaCheckUrlParams({[queryParamName]: `${filterId}:${fromValue}~`})
                            .should.eventually.to.be.equal(
                                true,
                                `Параметр ${queryParamName} присутствует с нужным значением`
                            );

                    const waitUntilQueryParamsAppear = () => this.browser.waitUntil(
                        checkForQueryParamPresence,
                        10000,
                        'Не дождались добавление query-параметров в url страницы',
                        1000
                    );

                    const clickApply = () => this.filterPanel.clickApplyButton();

                    const waitUntilPageLoaded = () => this.browser.yaWaitForPageReady();

                    const checkNecessaryFilterVisible = () =>
                        this.browser.allure.runStep(
                            'Проверяем, что фильтр отображается на странице',
                            () => this.browser
                                .element(Filter.selectByFilterId(filterId))
                                .isVisible()
                        );

                    const checkNecessaryFilterSelected = filterId =>
                        this.browser.allure.runStep(
                            'Проверяем, что фильтр выбран',
                            () => this.browser
                                .element(`${Filter.selectByFilterId(filterId)} [data-auto="filter-range-min"] input`)
                                .getValue()
                                .should.eventually.to.be.equal(
                                    fromValue,
                                    'Значение отличается от введённого на прошлой странице'
                                )
                        );

                    await openFilterBlock();
                    await setFromValue(fromValue);
                    await waitUntilQueryParamsAppear();

                    const rangeFilter = createRangeFilter(filterId, filterKind);
                    const filterValues = rangeFilterValuesChecked
                        .map(filterValue => createFilterValue(filterValue, filterId, filterValue.id));
                    const filter = createFilter(rangeFilter, filterId);
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

                    await this.browser.setState('report', state);
                    await clickApply();

                    await waitUntilPageLoaded();
                    await checkForQueryParamPresence();
                    await checkNecessaryFilterVisible();
                    return checkNecessaryFilterSelected(filterId);
                },
            }),
        },
    },
});
