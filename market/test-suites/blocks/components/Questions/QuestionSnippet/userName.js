import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, имя пользователя', {
    feature: 'Контент страницы',
    story: {
        'По умолчанию': {
            'отображается имя пользователя': makeCase({
                id: 'marketfront-2822',
                issue: 'MARKETVERSTKA-31051',
                async test() {
                    await this.questionSnippet
                        .getUserName()
                        .should.eventually.be.equal(
                            'Willy W.',
                            'Отображается имя пользователя'
                        );
                },
            }),
        },
    },
});
