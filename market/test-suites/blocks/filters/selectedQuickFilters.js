import {makeSuite, makeCase} from '@yandex-market/ginny';
import {
    mergeState,
    createFilter,
    createFilterValue,

} from '@yandex-market/kadavr/mocks/Report/helpers';

import {
    createBooleanFilter,
    booleanFilterValuesChecked,
    createRadioFilter,
    createRangeFilter,
    rangeFilterValuesChecked,
    enumFilterValuesChecked,
    createEnumFilter,
} from '@self/platform/spec/hermione2/fixtures/filters/all-filters';

import {
    reportState,
} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/productWithOffers';

import {routes} from '@self/platform/spec/hermione/configs/routes';

const BOOLEAN_FILTER_ID = '1231231';
const RADIO_FILTER_ID = '1231232';
const RANGE_FILTER_ID = '15464317';
const ENUM_FILTER_ID = '15464320';

export default makeSuite('Выбранные быстрые фильтры 4 этаж', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64247',
    story: {
        async beforeEach() {
            const enumFilterValues = enumFilterValuesChecked.map(enumFilterValue =>
                createFilterValue(enumFilterValue, ENUM_FILTER_ID, enumFilterValue.id)
            );

            const enumFilterMock = createEnumFilter(ENUM_FILTER_ID);

            const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));

            const booleanFilter = createBooleanFilter(BOOLEAN_FILTER_ID);

            const filterValues = booleanFilterValuesChecked
                .map(filterValue => createFilterValue(filterValue, BOOLEAN_FILTER_ID, filterValue.id));

            const filter = createFilter(booleanFilter, BOOLEAN_FILTER_ID);

            const radioFilterMock = createRadioFilter(RADIO_FILTER_ID);

            const radioFilterValues = booleanFilterValuesChecked
                .map(filterValue => createFilterValue(filterValue, RADIO_FILTER_ID, filterValue.id));
            const radioFilter = createFilter(radioFilterMock, RADIO_FILTER_ID);

            const rangeFilterMock = createRangeFilter(RANGE_FILTER_ID, 1);
            const rangeFilterValues = rangeFilterValuesChecked
                .map(filterValue => createFilterValue(filterValue, RANGE_FILTER_ID, filterValue.id));
            const rangeFilter = createFilter(rangeFilterMock, RANGE_FILTER_ID);


            await this.browser.setState('report', mergeState([
                reportState,
                filter,
                ...filterValues,
                enumFilter,
                ...enumFilterValues,
                rangeFilter,
                ...rangeFilterValues,
                radioFilter,
                ...radioFilterValues,
            ]));

            await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text, glfilter: '7893318%3A7701962'});
        },
        'Фильтер boolean': {
            'заголовок не отображается': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const isNameExisting = await this.quickFilters.isQuickActiveFilterNameExisting(BOOLEAN_FILTER_ID);

                    return this.browser.expect(isNameExisting)
                        .to.be.equal(
                            false
                        );
                },
            }),
            'значение отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterValue = await this.quickFilters.getQuickActiveFilterValue(BOOLEAN_FILTER_ID);
                    return this.browser.expect(filterValue)
                        .to.be.equal(
                            'Filter Name'
                        );
                },
            }),
        },
        'Фильтер enum': {
            'заголовок отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterName = await this.quickFilters.getQuickActiveFilterName(ENUM_FILTER_ID);
                    return this.browser.expect(filterName)
                        .to.be.equal(
                            'Filter Name'
                        );
                },
            }),
            'значение отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterValue = await this.quickFilters.getQuickActiveFilterValue(ENUM_FILTER_ID);
                    return this.browser.expect(filterValue)
                        .to.be.equal(
                            '1'
                        );
                },
            }),
        },
        'Фильтер range': {
            'заголовок отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterName = await this.quickFilters.getQuickActiveFilterName(RANGE_FILTER_ID);
                    return this.browser.expect(filterName)
                        .to.be.equal(
                            'Filter Name'
                        );
                },
            }),
            'значение отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterValue = await this.quickFilters.getQuickActiveFilterValue(RANGE_FILTER_ID);
                    return this.browser.expect(filterValue)
                        .to.be.equal(
                            'от 10'
                        );
                },
            }),
        },
        'Фильтер radio': {
            'заголовок не отображается': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const isNameExisting = await this.quickFilters.isQuickActiveFilterNameExisting(RADIO_FILTER_ID);

                    return this.browser.expect(isNameExisting)
                        .to.be.equal(
                            false
                        );
                },
            }),
            'значение отображается верно': makeCase({
                id: 'm-touch-3841',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const filterValue = await this.quickFilters.getQuickActiveFilterValue(RADIO_FILTER_ID);
                    return this.browser.expect(filterValue)
                        .to.be.equal(
                            'Filter Name'
                        );
                },
            }),
        },
    },
});
