import {makeSuite, makeCase} from 'ginny';

import {makeFilterParamValue, waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';

/**
 * Тест на блок FilterList.
 * @property {PageObject.FilterList} filterList
 * @property {PageObject.SearchResults} searchResults
 */
export default makeSuite('Списковой фильтр.', {
    feature: 'Фильтр типа «ENUM»',
    params: {
        queryParamName: 'Query-параметр для данного фильтра',
    },
    story: {
        'При выборе/отжатии значений списка': {
            'изменяются параметры в урле и обновляется выдача': makeCase({
                test() {
                    const {queryParamName, getReportState} = this.params;

                    const setReportState = step /* : 'first' | 'second' */ => {
                        const state = getReportState(step);

                        if (state) {
                            return this.browser.setState('report', state);
                        }

                        return Promise.resolve();
                    };

                    const clickOnFirstItem = () => this.filterList.clickItemByIndex(1);
                    const clickOnSecondItem = () => this.filterList.clickItemByIndex(2);

                    const getQueryParamValueForFirstItem = () => this.filterList.getItemIdByIndex(1)
                        .then(itemId => {
                            const [filterId, filterValueId] = itemId.split('_');
                            return makeFilterParamValue({
                                id: filterId,
                                value: filterValueId,
                                name: queryParamName,
                            });
                        });
                    const getQueryParamValueForFirstAndSecondItem = () =>
                        Promise.all([
                            this.filterList.getItemIdByIndex(1),
                            this.filterList.getItemIdByIndex(2),
                        ]).then(([item1Id, item2Id]) => {
                            const [filterId, filterValueId2] = item1Id.split('_');
                            const [, filterValueId3] = item2Id.split('_');
                            const joinedFilterValue = [filterValueId2, filterValueId3].join();
                            return makeFilterParamValue({
                                id: filterId,
                                value: joinedFilterValue,
                                name: queryParamName,
                            });
                        });
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

                    return checkForQueryParamAbsence()
                        .then(() => setReportState('first'))
                        .then(() => waitForSuccessfulSnippetListUpdate(
                            this.browser,
                            clickOnFirstItem,
                            this.searchResults
                        ))
                        .then(() => getQueryParamValueForFirstItem())
                        .then(queryParam => checkForQueryParamPresence(queryParam))
                        .then(() => setReportState('second'))
                        .then(() => waitForSuccessfulSnippetListUpdate(
                            this.browser,
                            clickOnSecondItem,
                            this.searchResults
                        ))
                        .then(() => getQueryParamValueForFirstAndSecondItem())
                        .then(queryParam => checkForQueryParamPresence(queryParam));
                },
            }),
        },
    },
});
