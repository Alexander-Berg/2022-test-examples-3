import {makeSuite, makeCase} from 'ginny';

const VENDOR_FILTER_ID = '7893318';

/**
 * @param {PageObject.SearchOptions} SearchOptions
 */
export default makeSuite('Выбор фильтра.', {
    environment: 'testing',
    params: {
        pageRoot: 'Ожидаемый slug страницы после применения фильтров',
    },
    story: {
        'При применении фильтра': {
            'slug должен сохраняться': makeCase({
                id: 'm-touch-2440',
                issue: 'MOBMARKET-10282',
                async test() {
                    await this.searchOptions.clickOnFiltersButton();
                    await this.filters.clickFilterById(VENDOR_FILTER_ID);

                    await this.selectFilter.waitForVisible();

                    await this.selectFilter.valueItems.click();

                    await this.filterPopup.apply();

                    await this.filterPopup.waitForSpinnerHidden();

                    await this.filters.waitForApplyButtonActive();

                    await this.filters.apply();

                    return this.browser
                        .yaParseUrl()
                        .should.eventually.be.link({
                            pathname: `catalog--${this.params.pageRoot}/\\d+/list`,
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
