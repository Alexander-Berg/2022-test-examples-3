import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    mergeState,
    createFilter,
    createFilterValue,

} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    reportState,
} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/productWithOffers';
import {routes} from '@self/platform/spec/hermione/configs/routes';
import {
    enumFilterValuesChecked,
    createEnumFilter,
} from '@self/platform/spec/hermione2/fixtures/filters/all-filters';


const ENUM_FILTER_ID = '15464320';

export default makeSuite('Cброс быстрых фильтров (c одним выбранными фильтром)', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64247',
    story: {
        async beforeEach() {
            const enumFilterValues = enumFilterValuesChecked.map(enumFilterValue =>
                createFilterValue(enumFilterValue, ENUM_FILTER_ID, enumFilterValue.id)
            );
            const enumFilterMock = createEnumFilter(ENUM_FILTER_ID);
            const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));

            const reportState2 = mergeState([
                reportState,
                enumFilter,
                ...enumFilterValues,
            ]);

            await this.browser.setState('report', reportState2);
            await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text, glfilter: '7893318%3A7701962'});
        },
        'Перед всеми фильтрами отсутсвует иконка сброса': makeCase({
            id: 'm-touch-3829',
            async test() {
                await this.browser.yaWaitForPageReady();
                const isResetLinkExisting = await this.resetLink.isResetLinkExisting();
                return this.browser.expect(isResetLinkExisting)
                    .to.be.equal(
                        false
                    );
            },
        }),
    },
});
