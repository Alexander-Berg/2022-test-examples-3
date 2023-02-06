import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на компонент FilterValuesPreview
 * @property {PageObject.FilterValuesPreview} filterValuesPreview
 */
export default makeSuite('Предраскрытые фильтры. Отображение.', {
    story: {
        'По умолчанию': {
            'должны отображаться': makeCase({
                test() {
                    const isFilterPreviewVisible = this.browser.allure.runStep(
                        'Проверяем видимость предраскрытых фильтров', () => this.filterValuesPreview.isVisible()
                    );

                    return isFilterPreviewVisible
                        .should.eventually.to.be.equals(true, 'Предраскрытые фильтры отображаются');
                },
            }),
        },
    },
});
