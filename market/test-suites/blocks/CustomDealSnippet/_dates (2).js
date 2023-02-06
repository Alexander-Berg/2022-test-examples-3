import {makeSuite, makeCase} from 'ginny';

/**
 * Тесты на даты проведения акции блока CustomDealSnippet
 * @property {PageObject.CustomDealSnippet} customDealSnippet
 */
export default makeSuite('Даты проведения акции на сниппете кастомной акции.', {
    params: {
        expectedDateText: 'Ожидаемый текст даты проведения акции',
    },
    story: {
        'по умолчанию': {
            'корректно отображаются': makeCase({
                issue: 'MOBMARKET-13217',
                id: 'm-touch-2970',
                async test() {
                    const dateText = await this.customDealSnippet.getDateText();

                    return this.expect(dateText).to.equal(this.params.expectedDateText, 'Дата корректна');
                },
            }),
        },
    },
});
