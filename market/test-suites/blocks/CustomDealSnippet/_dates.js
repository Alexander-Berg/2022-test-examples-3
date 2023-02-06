import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на даты проведения акции блока CustomDealSnippet
 * @param {PageObject.CustomDealSnippet} customDealSnippet
 */
export default makeSuite('Даты проведения акции на сниппете кастомной акции.', {
    params: {
        startDate: 'Дата начала акции',
        endDate: 'Дата конца акции',
        expectedDateText: 'Ожидаемый текст даты проведения акции',
    },
    story: {
        'по умолчанию': {
            'корректно отображаются': makeCase({
                issue: 'MARKETVERSTKA-34507',
                id: 'marketfront-2515',
                async test() {
                    const dateText = await this.customDealSnippet.getDateText();

                    return this.expect(dateText).to.equal(this.params.expectedDateText, 'Дата корректна');
                },
            }),
        },
    },
});
