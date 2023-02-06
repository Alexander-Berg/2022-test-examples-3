import {makeSuite, makeCase} from 'ginny';
import Filters from '@self/platform/components/Filters/__pageObject';
import SelectFilter from '@self/platform/components/SelectFilter/__pageObject';
import FilterPopup from '@self/platform/containers/FilterPopup/__pageObject';

/**
 * Тесты на фильтр способов оплаты
 * @param {PageObject.SearchOptions} searchOptions
 */
export default makeSuite('Список фильтров.', {
    params: {
        filterName: 'Название фильтра',
        filterId: 'Идентификатор фильтра',
    },
    story: {
        beforeEach() {
            this.setPageObjects({
                filters: () => this.createPageObject(Filters),
                selectFilter: () => this.createPageObject(SelectFilter),
                filterPopup: () => this.createPageObject(FilterPopup),
            });
        },
        'При открытии фильтров': {
            beforeEach() {
                return this.searchOptions.clickOnFiltersButton();
            },
            'должен присутствовать фильтр «Способы оплаты»': makeCase({
                async test() {
                    const rawText = await this.filters
                        .getFilterById(this.params.filterId)
                        .getText();

                    const [text] = rawText.split('\n');

                    return text.should.equal(
                        this.params.filterName,
                        `фильтр должен быть равен - ${this.params.filterName}`
                    );
                },
            }),

            'и нажатии на фильтр «Способы оплаты»': {
                'должна происходить фильтрация': makeCase({
                    async test() {
                        await this.filters.clickFilterById(this.params.filterId);
                        const expectedFilterVal = await this.selectFilter.getValueIdByIndex(1);

                        return this.selectFilter.getItemByIndex(1).click()
                            .then(() => this.filterPopup.apply())
                            .then(() => this.filters.waitForApplyButtonActive())
                            .then(() => this.filters.apply())
                            .then(() => this.browser.yaDelay(300))
                            .then(() => this.browser.yaParseUrl())
                            .then(url => {
                                const appliedFilterVal = url.query[this.params.filterId];

                                return this.expect(appliedFilterVal)
                                    .to.be.equal(expectedFilterVal, 'Применился нужный фильтр');
                            });
                    },
                }),
            },

            'при сбросе выбранного фильтра': {
                'не должно быть фильтра в query-параметрах': makeCase({
                    test() {
                        return this.filters.clickFilterById(this.params.filterId)
                            .then(() => this.filterPopup.waitForSpinnerHidden())
                            .then(() => this.selectFilter.getItemByIndex(1).click())
                            .then(() => this.filterPopup.apply())
                            .then(() => this.filters.waitForApplyButtonActive())
                            .then(() => this.filters.clickFilterById(this.params.filterId))
                            .then(() => this.selectFilter.reset())
                            .then(() => this.filterPopup.apply())
                            .then(() => this.filters.waitForApplyButtonActive())
                            .then(() => this.filters.apply())
                            .then(() => this.browser.yaDelay(300))
                            .then(() => this.browser.yaParseUrl())
                            .then(url => {
                                const paymentsFilterVal = url.query[this.params.filterId];

                                return this.expect(paymentsFilterVal)
                                    .to.be.equal(undefined, 'Фильтр отсутствует');
                            });
                    },
                }),
            },
        },
    },
});
