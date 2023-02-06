import {makeSuite, makeCase} from 'ginny';

import {
    mergeState,
    createOffer,
    createFilter,
    createProduct,
    createFilterValue,
} from '@yandex-market/kadavr/mocks/Report/helpers';

import {makeFilterParamValue, waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';

/**
 * Тесты на блок FilterCheckbox.
 * @param {PageObject.FilterCheckbox} filterCheckbox
 * @param {PageObject.SnippetList} snippetList
 */
export default makeSuite('Булевый фильтр', {
    feature: 'Фильтр типа «BOOLEAN»',
    environment: 'kadavr',
    params: {
        queryParamName: 'Query-параметр для данного фильтра',
        queryParamValue: 'Значение query-параметра, когда фильтр применен',
    },
    story: {
        'При нажатии/отжатии чекбокса': {
            'изменяются параметры в урле и обновляется выдача': makeCase({
                id: 'marketfront-599',
                issue: 'MARKETVERSTKA-24646',
                test() {
                    const {queryParamName, queryParamValue} = this.params;

                    const setReportState = () => {
                        const filterValue = {
                            'initialFound': 1,
                            'found': 100,
                            'value': '1',
                        };
                        const negativeFilterValue = {
                            'initialFound': 1,
                            'found': 100,
                            'value': '0',
                        };
                        const filterMock = {
                            'id': 'manufacturer_warranty',
                            'type': 'boolean',
                            'name': 'Гарантия производителя',
                            'kind': 2,
                            'values': [negativeFilterValue],
                        };

                        const product = createProduct({slug: 'product'});
                        const offer = createOffer({
                            entity: 'offer',
                            manufacturer: {warranty: true},
                            urls: {
                                encrypted: '/redir/encrypted',
                                decrypted: '/redir/decrypted',
                                offercard: '/redir/offercard',
                                geo: '/redir/geo',
                            },
                        });
                        const filter = createFilter(filterMock, 'manufacturer_warranty');
                        const dataMixin = {
                            data: {
                                search: {
                                    total: 1,
                                },
                            },
                        };

                        const state = mergeState([
                            product,
                            offer,
                            filter,
                            createFilterValue(filterValue, 'manufacturer_warranty', '1'),
                            createFilterValue(negativeFilterValue, 'manufacturer_warranty', '1'),
                            dataMixin,
                        ]);

                        return this.browser.setState('report', state);
                    };

                    const clickOnCheckbox = () => this.filterCheckbox.click();
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


                    return this.filterCheckbox.getFilterId()
                        .then(filterId => this.browser
                            .then(() => checkForQueryParamAbsence())
                            .then(() => setReportState())
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                clickOnCheckbox,
                                this.snippetList
                            ))
                            .then(() => checkForQueryParamPresence(
                                makeFilterParamValue({
                                    id: filterId,
                                    value: queryParamValue,
                                    name: queryParamName,
                                })
                            ))
                            .then(() => waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                clickOnCheckbox,
                                this.snippetList
                            ))
                            .then(() => checkForQueryParamAbsence())
                        );
                },
            }),
        },
    },
});
