import {makeCase, makeSuite} from 'ginny';

/**
 * Тесты на n-snippet-cell2 при наличии скидки
 * @param {PageObject.SnippetCell2} snippetCell2
 */
export default makeSuite('Текст "предзаказ" на сниппете.', {
    feature: 'Сниппет КО',
    story: {
        'По умолчанию': {
            'Текст соответствует ожидаемому': makeCase({
                id: 'marketfront-4058',
                issue: 'MARKETFRONT-10965',
                test() {
                    return this.snippetCell2.getDeliveryPartText()
                        .should
                        .be
                        .eventually
                        .equal('Предзаказ');
                },
            }),
        },
    },
});
