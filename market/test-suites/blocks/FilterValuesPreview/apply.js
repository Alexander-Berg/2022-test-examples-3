import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент FilterValuesPreview
 * @property {PageObject.FilterCompound} filterCompound
 * @property {PageObject.FilterValuesPreview} filterValuesPreview
 *
 * @param {Object} params
 * @param {string} params.filterValueId Идентификатор выбираемого значения фильтра.
 * @param {string} params.filterValueText Текст выбираемого значения фильтра.
 */
export default makeSuite('Предраскрытые фильтры. Применение', {
    story: {
        'При клике по значению': {
            'должно устанавливаться новое значение фильтра': makeCase({
                async test() {
                    await this.filterValuesPreview.clickOnValue(this.params.filterValueId);

                    const isFilterValueActive = this.filterValuesPreview.isValueActive(
                        this.params.filterValueId
                    );

                    await isFilterValueActive
                        .should.eventually.to.be.equals(true, 'Значение должно быть активно');

                    const textValue = this.filterCompound.getFilterTextValue();

                    return textValue.should.eventually.to.be.equals(
                        this.params.filterValueText,
                        'Выбранное значение должно быть активно'
                    );
                },
            }),
        },
    },
});
