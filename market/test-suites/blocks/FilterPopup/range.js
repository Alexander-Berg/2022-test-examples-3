import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент FilterPopup
 *
 * @property {PageObject.FilterCompound} filterCompound
 * @property {PageObject.FilterPopup} filterPopup
 * @property {PageObject.RangeFilter} rangeFilter
 *
 * @param {Object} params
 * @param {string} params.valueFrom
 * @param {string} params.valueTo
 * @param {string} params.expectedValue
 */
export default makeSuite('Фильтр выбора диапазона', {
    story: {
        'При установке диапазона': {
            'должно устанавливаться новое значение фильтра': makeCase({
                async test() {
                    await this.filterCompound.clickOnCard();

                    const {valueFrom, valueTo} = this.params;

                    await this.rangeFilter.waitForVisible();
                    await this.rangeFilter.setTextToField('from', valueFrom);
                    await this.rangeFilter.setTextToField('to', valueTo);

                    await this.filterPopup.waitForSpinnerHidden();
                    await this.filterPopup.apply();

                    const value = this.filterCompound.getFilterTextValue();

                    return value.should.eventually.to.be.equals(
                        this.params.expectedValue, `Значение фильтра должно быть равно "${this.params.expectedValue}"`
                    );
                },
            }),
        },
    },
});
