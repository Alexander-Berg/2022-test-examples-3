import {makeSuite, makeCase} from 'ginny';
import {FILTER_ID, FILTER_OPTIONS} from '@self/platform/spec/hermione/fixtures/credit';

/**
 * Тест на Фильтр «Покупка в кредит».
 * @param {PageObject.FilterRadio} filterList
 */
export default makeSuite('Фильтр "Покупка в кредит"', {
    feature: 'Кредиты на Маркете',
    story: {
        'По умолчанию': {
            'присутствует на странице с указанными значениями': makeCase({
                test() {
                    const checkFilterValue = filterOptionId => {
                        const expectedLabelText = FILTER_OPTIONS[filterOptionId];

                        return this.filterList.getLabelTextById(`${FILTER_ID}_${filterOptionId}`)
                            .then(labelText => this.expect(labelText)
                                .to.equal(expectedLabelText, `Название фильтра ${expectedLabelText} правильное`)
                            );
                    };

                    return this.browser.allure.runStep(
                        'Фильтр "Покупка в кредит" присутствует на странице c ожидаемыми опциями',
                        () => Object.keys(FILTER_OPTIONS).map(checkFilterValue)
                    );
                },
            }),
        },
    },
});
