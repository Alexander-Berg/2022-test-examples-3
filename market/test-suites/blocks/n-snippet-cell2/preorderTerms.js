import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на n-snippet-cell2 при наличии скидки
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Текст даты предзаказа на сниппете.', {
    feature: 'Сниппет КМ',
    story: {
        'По умолчанию': {
            'Текст соответствует ожидаемому': makeCase({
                id: 'marketfront-4059',
                issue: 'MARKETFRONT-10965',
                test() {
                    return this.snippetCell2.getPreorderTermsText()
                        .should
                        .be
                        .eventually
                        .equal('Официальный старт продаж 1 декабря');
                },
            }),
        },
    },
});
