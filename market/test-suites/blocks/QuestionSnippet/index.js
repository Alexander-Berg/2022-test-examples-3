import {makeCase, makeSuite} from 'ginny';

function makeState(ctx, {
    dataQuestionId,
    slug = 'any',
    canDelete = false,
    text = 'lol',
    questionType = 'product',
}) {
    const question = {
        id: dataQuestionId,
        canDelete,
        user: {
            uid: '666',
            entity: 'user',
        },
        author: {
            id: '666',
            entity: 'user',
        },
        slug,
        text,
    };

    if (questionType === 'product') {
        question.product = {
            id: 1722193751,
            entity: 'product',
        };
    } else {
        question.category = {
            id: 91491,
            entity: 'category',
        };
        question.product = null;
    }

    const schema = {
        users: [{
            id: '666',
            public_id: 'lolpop112233',
            uid: {
                value: '666',
            },
            login: 'lol',
            display_name: {
                name: 'lop pop',
                public_name: 'Lol P.',
            },
        }],
        modelQuestions: [question],
    };

    return ctx.browser
        .setState('schema', schema)
        .then(() => ctx.browser.refresh());
}

/**
 * @param {PageObject.QuestionSnippet} questionSnippet
 * @param {PageObject.QuestionList} questionList
 * @param {PageObject.PromptDialog} promptDialog
 */
export default makeSuite('Блок вопроса.', {
    environment: 'kadavr',
    params: {
        expectedContentLink: 'Ожидаемая ссылка на вопрос',
        slug: 'Представление вопроса в url',
        dataQuestionId: 'Id текущего вопроса',
    },
    story: {
        'Текст вопроса': {
            'по умолчанию': {
                'содержит ссылку на вопрос.': makeCase({
                    id: 'm-touch-2186',
                    issue: 'MOBMARKET-8803',
                    test() {
                        const {dataQuestionId, slug, questionType} = this.params;
                        return makeState(this, {dataQuestionId, slug, questionType})
                            .then(() => this.questionSnippet.contentHref)
                            .then(currentHref =>
                                this.expect(currentHref, 'ссылка корректная')
                                    .to.be.link({pathname: this.params.expectedContentLink}, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                            );
                    },
                }),
                'содержит "Показать еще", если вопрос длинный': makeCase({
                    id: 'm-touch-2210',
                    issue: 'MOBMARKET-8957',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType, text: 'lol '.repeat(10)})
                            .then(() => this.questionSnippet.isTextWithReadMoreVisible())
                            .should.eventually.to.be.equal(true, '"Показать еще" присутствует');
                    },
                }),
                'содержит корректное имя пользователя': makeCase({
                    id: 'm-touch-2211',
                    issue: 'MOBMARKET-8998',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType})
                            .then(() => this.questionSnippet.getAuthorNameText())
                            .should.eventually.to.be.equal('Lol P.', 'Имя пользователя корректное');
                    },
                }),
                'ответить содержит корректную ссылку': makeCase({
                    id: 'm-touch-2188',
                    issue: 'MOBMARKET-9004',
                    test() {
                        const {dataQuestionId, slug, questionType} = this.params;
                        return makeState(this, {dataQuestionId, slug, questionType})
                            .then(() => this.questionSnippet.answerHref)
                            .then(currentHref =>
                                this.expect(currentHref, 'Ожидаемая ссылка на вопрос')
                                    .to.be.link({pathname: this.params.expectedContentLink}, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                            );
                    },
                }),
                'аватар пользователя не отображается': makeCase({
                    id: 'm-touch-2212',
                    issue: 'MOBMARKET-8999',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType})
                            .then(() => this.questionSnippet.isAvatarVisible())
                            .should.eventually.to.be.equal(false, 'Аватар пользователя не отображается');
                    },
                }),
                'отображается блок даты создания': makeCase({
                    id: 'm-touch-2980',
                    issue: 'MARKETFRONT-3451',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType})
                            .then(() => this.questionSnippet.isDateInfoVisible())
                            .should.eventually.to.be.equal(true, 'Дата создания комментария отображается');
                    },
                }),
            },

        },
        'Количество ответов': {
            'по умолчанию': {
                'содержит ссылку на вопрос': makeCase({
                    id: 'm-touch-2187',
                    issue: 'MOBMARKET-8955',
                    test() {
                        const {dataQuestionId, slug, questionType} = this.params;
                        return makeState(this, {dataQuestionId, slug, questionType})
                            .then(() => this.questionSnippet.answersCountHref)
                            .then(currentHref =>
                                this.expect(currentHref, 'ссылка корректная')
                                    .to.be.link({pathname: this.params.expectedContentLink}, {
                                        skipProtocol: true,
                                        skipHostname: true,
                                    })
                            );
                    },
                }),
            },
        },
        'Кнопка удалить': {
            'если сущность нельзя удалить': {
                'не отображается': makeCase({
                    id: 'm-touch-2194',
                    issue: 'MOBMARKET-8878',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType, canDelete: false})
                            .then(() => this.questionSnippet.isRemoveButtonVisible())
                            .should.eventually.to.be.equal(false, 'Кнопка удалить не отображается');
                    },
                }),
            },
            'если сущность можно удалить': {
                отображается: makeCase({
                    id: 'm-touch-2236',
                    issue: 'MOBMARKET-8894',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType, canDelete: true})
                            .then(() => this.questionSnippet.isRemoveButtonVisible())
                            .should.eventually.to.be.equal(true, 'Кнопка удалить отображается');
                    },
                }),
            },
            'если отменить удаление': {
                'не удаляет вопрос': makeCase({
                    id: 'm-touch-2193',
                    issue: 'MOBMARKET-8917',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType, canDelete: true})
                            .then(() => this.questionSnippet.clickRemoveButton())
                            .then(() => this.promptDialog.clickCancel())
                            .then(() => this.questionList.isQuestionVisible(dataQuestionId))
                            .should.eventually.to.be.equal(true, 'Вопрос все еще отображается');
                    },
                }),
            },
            'если подтвердить удаление': {
                'удаляет вопрос': makeCase({
                    id: 'm-touch-2190',
                    issue: 'MOBMARKET-8983',
                    test() {
                        const {dataQuestionId, questionType} = this.params;
                        return makeState(this, {dataQuestionId, questionType, canDelete: true})
                            .then(() => this.questionSnippet.clickRemoveButton())
                            .then(() => this.promptDialog.clickSubmitButton())
                            .then(() => this.questionList.isQuestionVisible(dataQuestionId))
                            .should.eventually.to.be.equal(false, 'Вопрос больше не отображается');
                    },
                }),
            },
        },
    },
});
