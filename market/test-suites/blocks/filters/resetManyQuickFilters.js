import {makeSuite, makeCase} from '@yandex-market/ginny';

import {
    mergeState,
    createFilter,
    createFilterValue,

} from '@yandex-market/kadavr/mocks/Report/helpers';
import {
    enumFilterValuesChecked,
    createEnumFilter,
    booleanFilterValuesChecked,
    createRadioFilter,
} from '@self/platform/spec/hermione2/fixtures/filters/all-filters';
import {
    reportState,
} from '@self/platform/spec/hermione2/test-suites/blocks/filters/fixtures/productWithOffers';
import {routes} from '@self/platform/spec/hermione/configs/routes';

const ENUM_FILTER_ID = '15464320';
const RADIO_FILTER_ID = '1231232';
const enumFilterIds = [
    '1', '2',
];

export default makeSuite('Cброс быстрых фильтров (c несколькими выбранными фильтрами).', {
    environment: 'kadavr',
    issue: 'MARKETFRONT-64247',
    story: {
        async beforeEach() {
            const enumFilterValues = enumFilterValuesChecked.map((enumFilterValue, i) =>
                createFilterValue({...enumFilterValue, checked: true}, ENUM_FILTER_ID, enumFilterIds[i])
            );

            const enumFilterMock = createEnumFilter(ENUM_FILTER_ID);
            const enumFilter = createFilter(enumFilterMock, String(ENUM_FILTER_ID));

            const radioFilterMock = createRadioFilter(RADIO_FILTER_ID);

            const radioFilterValues = booleanFilterValuesChecked
                .map(filterValue => createFilterValue(filterValue, RADIO_FILTER_ID, filterValue.id));
            const radioFilter = createFilter(radioFilterMock, RADIO_FILTER_ID);


            const reportState2 = mergeState([
                reportState,
                enumFilter,
                ...enumFilterValues,
                radioFilter,
                ...radioFilterValues,
            ]);


            await this.browser.setState('report', reportState2);
            await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text, glfilter: '7893318%3A7701962'});
        },
        'На выдаче по поисковому запросу "Мобильные телефоны"': {
            'перед всеми фильтрами присутствует иконка сброса': makeCase({
                id: 'm-touch-3840',
                async test() {
                    await this.browser.yaWaitForPageReady();
                    const isResetLinkExisting = await this.resetLink.isResetLinkExisting();
                    return this.browser.expect(isResetLinkExisting)
                        .to.be.equal(
                            true
                        );
                },
            }),
            'ссылка сброса всех фильтров формируется правильно': makeCase({
                id: 'm-touch-3840',
                async test() {
                    await this.browser.yaOpenPage('touch:list', {...routes.catalog.phones, was_redir: 1, text: this.params.text, glfilter: '7893318%3A7701962', cpa: 1});
                    await this.browser.yaWaitForPageReady();
                    const resetLinkHref = await this.resetLink.getHref();
                    const isResetLinkValid = !resetLinkHref.includes('glfilter') && !resetLinkHref.includes('cpa');
                    return this.browser.expect(isResetLinkValid)
                        .to.be.equal(
                            true
                        );
                },
            }),
        },
    },
});
