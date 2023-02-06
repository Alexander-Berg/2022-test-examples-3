import {makeSuite, makeCase} from 'ginny';
import {
    waitForSuccessfulSnippetListUpdate,
    makeFilterParamValue,
    makeQueryParams,
} from '@self/platform/spec/hermione/helpers/filters';
import SnippetList from '@self/platform/spec/page-objects/n-snippet-list';
import FilterPanelExtend from '@self/platform/spec/page-objects/n-filter-panel-extend';

/**
 * Тесты на блок радио фильтра
 * @property {PageObject.FilterBlock} this.filterBlock
 * @property {PageObject.FilterPanelExtend} this.filterPanel
 */

export default makeSuite('Взаимодействие радио-фильтра и спиского на выдаче.', {
    feature: 'Фильтрация',
    environment: 'testing',
    story: {
        before() {
            this.setPageObjects({
                filterPanel: () => this.createPageObject(FilterPanelExtend),
                snippetList: () => this.createPageObject(SnippetList),
            });
        },
        'При применении радио фильтра': {
            'переходим на выдачу, отфильтрованную по нему и при выборе спиского параметр радио фильтра сохраняется':
                makeCase({
                    id: 'marketfront-668',
                    issue: 'MARKETVERSTKA-24719',
                    environment: 'testing',
                    params: {
                        queryParamNameForFilterBlock: 'Query-параметр для фильтра на странице всех фильтров',
                        filterBlockId: 'id фильтра',
                        queryParamNameForFilterList: 'Query-параметр для спискового фильтра',
                    },
                    async test() {
                        const {
                            queryParamNameForFilterBlock,
                            filterBlockId,
                            queryParamNameForFilterList,
                        } = this.params;

                        const checkForQueryParamPresence = params =>
                            this.browser
                                .yaCheckUrlParams(params)
                                .should.eventually.to.be.equal(true, 'Проверка параметров');

                        const clickOnFirstItem = () => this.filterBlock.clickItemByIndex(1);
                        const queryParamValueForFirstItemFilterRadio =
                            await this.filterBlock.getItemIdByIndex(1)
                                .then(itemId => {
                                    const [, filterId, filterValueId] = itemId.split(',');
                                    return makeFilterParamValue({
                                        id: filterId,
                                        value: filterValueId,
                                        name: queryParamNameForFilterBlock,
                                    });
                                });

                        const waitUntilQueryParamsAppear = () => this.browser.waitUntil(
                            () => checkForQueryParamPresence({
                                [queryParamNameForFilterBlock]: queryParamValueForFirstItemFilterRadio,
                            }),
                            10000,
                            'Не дождались добавление query-параметров в url страницы',
                            1000
                        );

                        const checkNecessaryFilterVisible = () =>
                            this.browser.allure.runStep(
                                'Проверяем, что фильтр находится на странице',
                                () => this.browser
                                    .element(`[data-autotest-id="${filterBlockId}"]`)
                                    .isVisible()
                            );

                        const checkNecessaryFilterSelected = itemId =>
                            this.browser.allure.runStep('Проверяем, что фильтр выбран', () => this.browser
                                .element(`[data-autotest-id="${filterBlockId}"] input[id$="${itemId}"]`)
                                .isSelected());

                        const openFilterRadio = () => this.filterBlock.isClosed()
                            .then(isClosed => {
                                if (isClosed) {
                                    return this.filterBlock.click();
                                }

                                return undefined;
                            });
                        const clickApply = () => this.filterPanel.clickApplyButton();

                        const clickOnFirstItemFilterList = () => this.filterList.clickItemByIndex(1);
                        const getQueryParamValueForFirstItemFilterList = async () =>
                            this.filterList.getItemIdByIndex(1)
                                .then(itemId => {
                                    const [filterId, filterValueId] = itemId.split('_');
                                    return makeFilterParamValue({
                                        id: filterId,
                                        value: filterValueId,
                                        name: queryParamNameForFilterList,
                                    });
                                });

                        await openFilterRadio();
                        await clickOnFirstItem();
                        await waitUntilQueryParamsAppear();
                        await clickApply();
                        await this.browser.yaWaitForPageLoaded();
                        await checkForQueryParamPresence({
                            [queryParamNameForFilterBlock]: queryParamValueForFirstItemFilterRadio,
                        });
                        await checkNecessaryFilterVisible();
                        await checkNecessaryFilterSelected(
                            queryParamValueForFirstItemFilterRadio.split(':').join('_')
                        );
                        await waitForSuccessfulSnippetListUpdate(
                            this.browser,
                            clickOnFirstItemFilterList,
                            this.snippetList
                        );

                        const queryParamValueForFirstItemFilterList = await getQueryParamValueForFirstItemFilterList();

                        return checkForQueryParamPresence(
                            makeQueryParams([
                                {[queryParamNameForFilterBlock]: queryParamValueForFirstItemFilterRadio},
                                {[queryParamNameForFilterList]: queryParamValueForFirstItemFilterList},
                            ])
                        );
                    },
                }),
        },
    },
});
