import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на появления даты начала продаж.
 * @param {PageObject.PreorderTerms} preorderTerms
 */
export default makeSuite('Дата старта продаж', {
    story: {
        'По умолчанию': {
            'должна быть равна ожидаемой': makeCase({
                test() {
                    return this.snippet.getPreorderTermsText()
                        .should
                        .be
                        .eventually
                        .equal('Официальный старт продаж 1 декабря');
                },
            }),
        },
    },
});
