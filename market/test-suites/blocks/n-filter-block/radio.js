import {makeCase, makeSuite} from 'ginny';
import {
    createFilter,
    createFilterValue,
    createProduct,
    mergeState,
    priceSort,
} from '@yandex-market/kadavr/mocks/Report/helpers';
import {makeFilterParamValue} from '@self/platform/spec/hermione/helpers/filters';
import {
    booleanFilterValuesChecked,
    createRadioFilter,
} from '@self/project/src/spec/hermione/fixtures/filters/all-filters';
import {guruMock} from '@self/platform/spec/hermione/fixtures/priceFilter/product';
import AllFilters from '@self/platform/widgets/content/AllFilters/__pageObject';
import CommonSearchFilter from '@self/platform/widgets/content/AllFilters/CommonSearchFilter/__pageObject';
import Accordion from '@self/platform/components/Accordion/__pageObject';
import FilterValuesRadioList from '@self/platform/components/FilterValuesRadioList/__pageObject';
import Filter from '@self/root/src/widgets/content/search/Filters/components/Filter/__pageObject';

/**
 * Тесты на блок радио фильтра
 * @property {PageObject.FilterBlock} this.filterBlock
 * @property {PageObject.FilterPanelExtend} this.filterPanel
 */

export default makeSuite('Радио фильтр.', {
    feature: 'Фильтр типа «RADIO»',
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
                filterRadio: () => this.createPageObject(FilterValuesRadioList, {
                    parent: this.accordion,
                }),
            });
        },
        'При применении': {
            'переходим на выдачу, отфильтрованную согласно выбранному фильтру': makeCase({
                environment: 'kadavr',
                params: {
                    queryParamName: 'Query-параметр для данного фильтра',
                    filterId: 'id фильтра',
                    filterKind: 'Род фильтра',
                },
                test() {
                    const {queryParamName, filterId, filterKind} = this.params;

                    const clickOnFirstItem = () => this.filterRadio.clickItemByIndex(1);
                    const getQueryParamValueForFirstItem = () => this.filterRadio.getFilterPairIdByIndex(1)
                        .then(([id, valueId]) =>
                            makeFilterParamValue({
                                id,
                                value: valueId,
                                name: queryParamName,
                            })
                        );

                    const checkForQueryParamPresence = (queryParamValue = true) =>
                        this.browser
                            .yaCheckUrlParams({[queryParamName]: queryParamValue})
                            .should.eventually.to.be.equal(true, 'Параметр присутствует');

                    const checkNecessaryFilterVisible = () =>
                        this.browser.allure.runStep(
                            'Проверяем, что фильтр находится на странице',
                            () => this.browser
                                .element(Filter.selectByFilterId(filterId))
                                .isVisible()
                        );

                    const checkNecessaryFilterSelected = itemId =>
                        this.browser.allure.runStep(
                            'Проверяем, что фильтр выбран',
                            () => this.browser
                                .element(`${Filter.selectByFilterId(filterId)} input[value="${itemId}"]`)
                                .isSelected()
                        );

                    const checkFilterIsOpen = () => this.accordion.isOpen();
                    const clickApply = () => this.filterPanel.clickApplyButton();

                    return checkFilterIsOpen()
                        .then(isOpen => {
                            if (!isOpen) {
                                return this.accordion.toggle();
                            }

                            return undefined;
                        })
                        .then(() => clickOnFirstItem())
                        .then(() => {
                            const radioFilter = createRadioFilter(filterId, filterKind);
                            const filterValues = booleanFilterValuesChecked
                                .map(filterValue => createFilterValue(filterValue, filterId, filterValue.id));
                            const filter = createFilter(radioFilter, filterId);
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
                        .then(() => getQueryParamValueForFirstItem())
                        .then(queryParam => clickApply()
                            .then(() => checkForQueryParamPresence(queryParam))
                            .then(() => checkNecessaryFilterVisible())
                            .then(() => checkNecessaryFilterSelected(queryParam.split(':').join('_')))
                        );
                },
            }),
        },
    },
});
