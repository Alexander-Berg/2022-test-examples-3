import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 */
export default makeSuite('Сниппет вопроса, аватар пользователя', {
    feature: 'Контент страницы',
    story: {
        'По умолчанию': {
            'не отображается аватар пользователя': makeCase({
                id: 'marketfront-2821',
                issue: 'MARKETVERSTKA-31050',
                async test() {
                    await this.questionSnippet
                        .isUserAvatarImageVisible()
                        .should.eventually.be.equal(
                            false,
                            'Аватар пользователя не отображается'
                        );
                },
            }),
        },
    },
});
