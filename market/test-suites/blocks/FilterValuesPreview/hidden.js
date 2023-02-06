import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент FilterValuesPreview
 *
 * @property {PageObject.FilterCompound} filterCompound
 * @property {PageObject.FilterValuesPreview} filterValuesPreview
 */
export default makeSuite('Предраскрытые фильтры. Скрытие.', {
    story: {
        'При предварительно выбранном значении': {
            'не должны отображаться': makeCase({
                async test() {
                    const isFilterPreviewVisible = this.browser.allure.runStep(
                        'Проверяем видимость предраскрытых фильтров', () => this.filterValuesPreview.isVisible()
                    );

                    await isFilterPreviewVisible
                        .should.eventually.to.be.equals(false, 'Предраскрытые фильтры не отображаются');

                    return this.filterCompound.getFilterTextValue()
                        .should.eventually.not.to.be.empty;
                },
            }),
        },
    },
});
