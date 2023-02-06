import {makeSuite, makeCase} from 'ginny';
import {makeFilterParamValue} from '@self/platform/spec/hermione/helpers/filters';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';

const QUERY_PARAM_NAME = 'glfilter';

/**
 * Тесты на компонент FilterCompound
 * @property {PageObject.FilterCompound} filterCompound
 */
export default makeSuite('Элемент списка фильтров.', {
    story: {
        beforeEach() {
            this.setPageObjects({
                filterPopup: () => this.createPageObject(FilterPopup),
                selectFilter: () => this.createPageObject(SelectFilter),
            });
        },
        'При открытии фильтра и выборе значения': {
            'должен происходить переход в каталог с выбранным фильтром': makeCase({
                id: 'm-touch-2103',
                issue: 'MOBMARKET-8064',
                async test() {
                    const filterId = await this.filterCompound.getFilterId();

                    await this.filterCompound.clickOnCard();

                    const filterValueId = await this.selectFilter.getValueIdByIndex(1);

                    const filterParamValue = makeFilterParamValue({
                        id: filterId,
                        value: filterValueId,
                        name: QUERY_PARAM_NAME,
                    });

                    await this.selectFilter.valueItems.click();

                    return this.browser
                        .yaParseUrl()
                        .should.eventually.be.link({
                            pathname: '\\/catalog--[\\w-]+\\/[0-9]+\\/list',
                            query: {
                                [QUERY_PARAM_NAME]: filterParamValue,
                            },
                        }, {
                            mode: 'match',
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    },
});
