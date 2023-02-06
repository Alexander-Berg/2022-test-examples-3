import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CategoryQuestionsFooterEntrypoint} categoryQuestionsFooterEntrypoint
 */
export default makeSuite('Точка входа на категорийные вопросы в подвале. Меньше 7 вопросов.', {
    id: 'marketfront-3589',
    issue: 'MARKETVERSTKA-34996',
    feature: 'Q&A Категории',
    story: {
        'Кнопка "Ещё N вопросов"': {
            'не отображается': makeCase({
                async test() {
                    return this.categoryQuestionsFooterEntrypoint.hasMoreButton()
                        .should.eventually.be.equal(false, 'Кнопка входа не отображается');
                },
            }),
        },
    },
});
