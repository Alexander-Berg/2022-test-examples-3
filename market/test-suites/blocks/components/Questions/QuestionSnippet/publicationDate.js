import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, дата публикации', {
    feature: 'Контент страницы',
    story: {
        'По умолчанию': {
            'отображается дата публикации': makeCase({
                id: 'marketfront-2823',
                issue: 'MARKETVERSTKA-31053',
                async test() {
                    await this.questionSnippet
                        .getPublicationDateText()
                        .should.eventually.not.to.be.empty;
                },
            }),
        },
    },
});
