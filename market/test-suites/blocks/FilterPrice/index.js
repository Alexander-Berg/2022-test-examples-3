import {get} from 'lodash';
import {mergeSuites, makeSuite, makeCase} from 'ginny';
import {
    mergeState,
    createOffer,
    createFilter,
    createFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';
import Filter from '@self/root/src/widgets/content/search/Filters/components/Filter/__pageObject';
import FilterPriceOld from '@self/platform/spec/page-objects/FilterPrice';
import {FILTERS} from '@self/root/src/entities/filterSearch/constants';

import {
    filterPrice as filterPriceMock,
    filterPriceValues,
    dataMixin,
} from '@self/platform/spec/hermione/fixtures/priceFilter/filterPrice';
import {offerMock} from '@self/platform/spec/hermione/fixtures/priceFilter/offer';

const getCheckedState = ({min, max}) => {
    const FILTER_ID = 'glprice';

    const filterValuesChecked = [{
        min: min || '',
        max: max || '',
        id: 'chosen',
        checked: true,
    }, ...filterPriceValues];
    const filterValues =
        filterValuesChecked.map(value => {
            const filter = createFilter({...filterPriceMock, values: filterValuesChecked}, FILTER_ID);
            const filterValue = createFilterValue(value, FILTER_ID, value.id);
            return {
                ...filterValue,
                collections: {
                    ...filterValue.collections.filterValue,
                    filter: {
                        glprice: filter,
                    },
                },
            };
        });

    const offer = createOffer(offerMock);

    return mergeState([
        offer,
        dataMixin,
        ...filterValues,
    ]);
};

/**
 * Тесты на фильтр «Цена».
 * @param {PageObject} filtersAside
 * @param {PageObject.FilterPrice} filterPrice
 * @param {PageObject.SnippetList} snippetList
 */
export default makeSuite('Фильтр «Цена».', {
    feature: 'Фильтр «Цена»',
    story: mergeSuites(
        {
            beforeEach() {
                this.setPageObjects({
                    filter: () => this.createPageObject(Filter, {
                        parent: this.filtersAside,
                    }),
                    filterOld: () => this.createPageObject(FilterPriceOld, {
                        parent: this.filtersAside,
                    }),
                });
            },
            'по умолчанию': {
                'должен быть первым среди других фильтров': makeCase({
                    id: 'marketfront-620',
                    issue: 'MARKETVERSTKA-24666',
                    async test() {
                        const firstFilter = await this.filtersAside.firstFilter;
                        const filterPrice = await (
                            this.params && this.params.isTestOfOldPriceFilter
                                ? this.filterOld.root
                                : this.filter.getItemByFilterId(FILTERS.GLPRICE)
                        );

                        return this.browser.allure.runStep('На втором месте стоит фильтр «Цена»', () => {
                            const firstFilterElementHash = get(firstFilter, ['ELEMENT']);
                            const filterPriceElementHash = get(filterPrice, ['value', 'ELEMENT']);

                            return firstFilterElementHash.should.to.be.equal(
                                filterPriceElementHash,
                                'На втором месте стоит другой фильтр'
                            );
                        });
                    },
                }),
            },
            'При изменении значения диапазона': {
                'изменяются параметры в урле': makeCase({
                    id: 'marketfront-620',
                    issue: 'MARKETVERSTKA-24666',
                    test() {
                        const PARAMETERS = {
                            PRICE_VALUE: {
                                FROM: '1',
                                TO: '500000',
                            },
                            PRICE_URL_NAME: {
                                FROM: 'pricefrom',
                                TO: 'priceto',
                            },
                        };

                        const checkForQueryParamAbsence = paramName =>
                            this.browser
                                .yaParseUrl()
                                .then(({query}) =>
                                    this.expect(query, `Нет параметра ${paramName}`)
                                        .to.not.have.property(paramName)
                                );

                        const checkForQueryParamPresence = (paramName, paramValue) =>
                            this.browser
                                .yaParseUrl()
                                .then(({query}) =>
                                    this.expect(query)
                                        .to.have.property(
                                            paramName,
                                            paramValue,
                                            `Добавился параметр ${paramName} со значением ${paramValue}`
                                        )
                                );

                        return checkForQueryParamAbsence(PARAMETERS.PRICE_URL_NAME.FROM)
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                () => this.filterOld.setValue('from', PARAMETERS.PRICE_VALUE.FROM)
                                    .then(() => {
                                        const state = getCheckedState({
                                            min: PARAMETERS.PRICE_VALUE.FROM,
                                        });
                                        return this.browser.setState('report', state);
                                    }),
                                this.snippetList
                            ))
                            .then(() => checkForQueryParamPresence(
                                PARAMETERS.PRICE_URL_NAME.FROM,
                                PARAMETERS.PRICE_VALUE.FROM
                            ))
                            .then(() => checkForQueryParamAbsence(PARAMETERS.PRICE_URL_NAME.TO))
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                () => this.filterOld.setValue('to', PARAMETERS.PRICE_VALUE.TO)
                                    .then(() => {
                                        const state = getCheckedState({
                                            min: PARAMETERS.PRICE_VALUE.FROM,
                                            max: PARAMETERS.PRICE_VALUE.TO,
                                        });
                                        return this.browser.setState('report', state);
                                    }),
                                this.snippetList
                            ))
                            .then(() => checkForQueryParamPresence(
                                PARAMETERS.PRICE_URL_NAME.TO,
                                PARAMETERS.PRICE_VALUE.TO
                            ))
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                () => this.filterOld.clearInput('from'),
                                this.snippetList
                            ))
                            .then(() => checkForQueryParamAbsence(PARAMETERS.PRICE_URL_NAME.FROM));
                    },
                }),
            },
        }
    ),
});
