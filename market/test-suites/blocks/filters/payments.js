import {makeSuite, makeCase} from 'ginny';

/**
 * Тест на Фильтр «Способы оплаты».
 * @param {PageObject.FilterList} filterList
 */
export default makeSuite('Фильтр «Способы оплаты»', {
    story: {
        'По умолчанию': {
            'присутствует на странице с указанными значениями': makeCase({
                test() {
                    const filters = {
                        payments_prepayment_card: 'Картой на сайте',
                        payments_delivery_card: 'Картой курьеру',
                        payments_delivery_cash: 'Наличными курьеру',
                    };

                    const checkFilterValue = filter => {
                        const expectedLabelText = filters[filter];

                        return this.filterList.getLabelTextById(filter)
                            .then(labelText => this.expect(labelText)
                                .to.equal(expectedLabelText, `Название фильтра ${filter} должно совпадать`)
                            );
                    };

                    return this.browser.allure.runStep(
                        'Сравниваем названия фильтров',
                        () => checkFilterValue('payments_prepayment_card')
                            .then(() => checkFilterValue('payments_delivery_card'))
                            .then(() => checkFilterValue('payments_delivery_cash'))
                    );
                },
            }),
        },
    },
});
