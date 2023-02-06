import {makeCase, makeSuite, mergeSuites} from 'ginny';

// configs
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {makeCatalogerTree} from '@self/project/src/spec/hermione/helpers/metakadavr';
import {waitForSuccessfulSnippetListUpdate} from '@self/platform/spec/hermione/helpers/filters';
import {searchResultFashion, searchResultTrying} from '@self/platform/spec/hermione/fixtures/quickFilters/boolean';

/**
 * Тест на Фильтр "C примеркой".
 * @param {PageObject.QuickFilterBoolean} quickFilterBoolean
 * @param {PageObject.QuickFilters} quickFilters
 */
export default makeSuite('Фильтр "С примеркой"', {
    feature: 'Примерка в Маркете',
    issue: 'MARKETFRONT-85532',
    story: mergeSuites(
        makeSuite('Фешен выдача c оффером с возможностью примерки', {
            story: {
                async beforeEach() {
                    const pageParams = routes.catalog.fashion;
                    const treeParams = [
                        'Одежда, обувь и аксессуары',
                        pageParams.hid,
                        pageParams.nid,
                        {viewType: 'list', tags: ['fashion']},
                    ];

                    await this.browser.setState('Cataloger.tree', makeCatalogerTree(...treeParams));
                    await this.browser.setState('report', searchResultFashion);

                    await this.browser.yaOpenPage('market:list', pageParams);
                    await this.quickFilters.waitForVisible()
                        .should.eventually.to.be.equal(true, 'Виден быстрофильтр');
                },
                'При открытии страницы': {
                    'виден быстрофильтр "С примеркой" с правильным текстом': makeCase({
                        async test() {
                            const filterName = 'С примеркой';

                            await this.quickFilterBoolean.waitForVisible()
                                .should.eventually.to.be.equal(true, 'Виден быстрофильтр "С примеркой"');

                            return this.quickFilterBoolean.getFilterText()
                                .should.eventually.to.be.equal(filterName, `Текст фильтра должен быть ${filterName}`);
                        },
                    }),
                },
                'При нажатии/отжатии чекбокса': {
                    'изменяются параметры в урле и обновляется выдача': makeCase({
                        async test() {
                            const queryParamName = 'trying-available';
                            const queryParamValue = '1';

                            const clickOnCheckbox = () => this.quickFilterButton.click();

                            const checkForQueryParamAbsence = () => this.browser
                                .yaParseUrl()
                                .then(({query}) =>
                                    this.expect(query, `Нет параметра ${queryParamName}`)
                                        .to.not.have.property(queryParamName)
                                );
                            const checkForQueryParamPresence = () => this.browser
                                .yaParseUrl()
                                .then(({query}) =>
                                    this.expect(query)
                                        .to.have.property(
                                            queryParamName,
                                            queryParamValue,
                                            `Добавился параметр ${queryParamName} со значением ${queryParamValue}`
                                        )
                                );

                            await checkForQueryParamAbsence();

                            await this.browser.setState('report', searchResultTrying);
                            await waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                clickOnCheckbox,
                                this.snippetList
                            );


                            await checkForQueryParamPresence();

                            await this.browser.setState('report', searchResultFashion);
                            await waitForSuccessfulSnippetListUpdate(
                                this.browser,
                                clickOnCheckbox,
                                this.snippetList
                            );


                            return checkForQueryParamAbsence();
                        },
                    }),
                },
            },
        })
    ),
});

