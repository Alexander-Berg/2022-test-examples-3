import {makeSuite, makeCase} from 'ginny';

import {makeFilterParamValue, waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';

/**
 * Тесты на блок FilterRadio.
 * @param {PageObject.FilterRadio} filterRadio
 * @param {PageObject.SearchResults} searchResults
 * @param {function} getReportState - коллбек на установку стейта репорта между переключением фильтров
 */
export default makeSuite('Радио (RADIO) фильтр', {
    feature: 'Фильтр типа «RADIO»',
    environment: 'kadavr',
    params: {
        queryParamName: 'Query-параметр для данного фильтра',
        queryParamValues: 'Массив значений, которые необходимо проверить',
        filterId: 'Идентификатор фильтра',
    },
    story: {
        'При изменении фильтра': {
            'изменяются параметры в урле и обновляется выдача': makeCase({
                id: 'marketfront-601',
                issue: 'MARKETVERSTKA-24650',
                async test() {
                    const {
                        queryParamName,
                        queryParamValues,
                        filterId,
                        getReportState,
                    } = this.params;

                    const setReportState = value => {
                        const state = getReportState(value);

                        if (state) {
                            return this.browser.setState('report', state);
                        }

                        return Promise.resolve();
                    };

                    const clickItem = (id, value) => this.filterRadio.clickLabel(id, value);
                    const deselect = id => this.filterRadio.clickLabel(id, '-1');

                    const checkForQueryParamAbsence = () => this.browser
                        .yaParseUrl()
                        .then(({query}) =>
                            this.expect(query, `Нет параметра ${queryParamName}`)
                                .to.not.have.property(queryParamName)
                        );
                    const checkForQueryParamPresence = expectedFilterParamValue => this.browser
                        .yaParseUrl()
                        .then(({query}) =>
                            this.expect(query)
                                .to.have.property(
                                    queryParamName,
                                    expectedFilterParamValue,
                                    `Добавился параметр ${queryParamName} со значением ${expectedFilterParamValue}`
                                )
                        );

                    const checkFilterRadio = queryParamValue =>
                        checkForQueryParamAbsence()
                            .then(() => setReportState(queryParamValue))
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                () => clickItem(filterId, queryParamValue),
                                this.searchResults
                            ))
                            .then(() => checkForQueryParamPresence(
                                makeFilterParamValue({
                                    id: filterId,
                                    value: queryParamValue,
                                    name: queryParamName,
                                })
                            ))
                            .then(() => setReportState())
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                () => deselect(filterId),
                                this.searchResults
                            ))
                            .then(() => checkForQueryParamAbsence());

                    async function checkRadioFilters(values) {
                        for (const val of values) {
                            // eslint-disable-next-line no-await-in-loop
                            await checkFilterRadio(val);
                        }
                    }

                    return checkRadioFilters(queryParamValues);
                },
            }),
        },
    },
});
