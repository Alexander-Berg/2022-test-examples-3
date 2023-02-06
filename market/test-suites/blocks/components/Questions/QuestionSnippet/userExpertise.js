import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 * @param {PageObject.AuthorExpertise} authorExpertise
 */
export default makeSuite('Экспертиза автора ответа на вопрос', {
    feature: 'Структура страницы',
    story: {
        'по умолчанию': {
            'отображается корректно': makeCase({
                id: 'marketfront-4170',
                issue: 'MARKETFRONT-16499',
                test() {
                    return this.authorExpertise.isVisible()
                        .should.eventually.to.be.equal(true, 'Ответ на вопрос должен показывать экпертность');
                },
            }),
        },
    },
});
