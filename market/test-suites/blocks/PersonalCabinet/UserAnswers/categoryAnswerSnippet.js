import {makeCase, makeSuite} from 'ginny';

import {pluralize} from '@self/project/src/helpers/string';

/**
 * @param {PageObject.widgets.content.UserQuestions.components.QuestionHeader} questionHeader
 * @param {PageObject.widgets.content.UserAnswers.components.CabinetAnswerSnippet} cabinetAnswerSnippet
 * @param {PageObject.widgets.content.UserAnswers.components.AnswerContentBody} answerContentBody
 * @param {PageObject.widgets.content.UserQuestions.components.QuestionFooter} questionFooter
 * @param {PageObjects.components.VoteButton} voteButton
 */

export default makeSuite('Категорийный сниппет ответа.', {
    params: {
        categoryId: 'hid категории',
        categorySlug: 'Слаг категории',
        questionId: 'Id вопроса',
        questionSlug: 'Слаг вопроса',
        commentsCount: 'Количество комментариев к ответу',
        likesCount: 'Количество лайков ответа',
    },
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                id: 'm-touch-3163',
                async test() {
                    return this.cabinetAnswerSnippet.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет ответа отображается');
                },
            }),
        },
        'Шапка.': {
            'По умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3164',
                    async test() {
                        return this.questionHeader.isVisible()
                            .should.eventually.be.equal(true, 'Шапка отображается');
                    },
                }),
                'содержит ссылку на страницу категорийных вопросов': makeCase({
                    id: 'm-touch-3165',
                    async test() {
                        const actualUrl = await this.questionHeader.getHeaderLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:category-questions', {
                            slug: this.params.categorySlug,
                            hid: this.params.categoryId,
                        });

                        return this.expect(actualUrl, 'Cсылка корректная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
        'Тело сниппета.': {
            'По умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3166',
                    async test() {
                        return this.answerContentBody.isVisible()
                            .should.eventually.be.equal(true, 'Тело сниппета отображается');
                    },
                }),
                'текст вопроса отображается и содержит ссылку на категорийный вопрос': makeCase({
                    id: 'm-touch-3167',
                    async test() {
                        const actualUrl = await this.answerContentBody.getQuestionLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:category-question', {
                            categorySlug: this.params.categorySlug,
                            hid: this.params.categoryId,
                            questionSlug: this.params.questionSlug,
                            questionId: this.params.questionId,
                        });

                        await this.answerContentBody.isQuestionVisible()
                            .should.eventually.be.equal(true, 'Текст вопроса отображается');

                        return this.expect(actualUrl, 'Cсылка корректная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
                'текст ответа отображается и содержит ссылку на страницу категорийного вопроса': makeCase({
                    id: 'm-touch-3168',
                    async test() {
                        const actualUrl = await this.answerContentBody.getAnswerLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:category-question', {
                            categorySlug: this.params.categorySlug,
                            hid: this.params.categoryId,
                            questionSlug: this.params.questionSlug,
                            questionId: this.params.questionId,
                        });

                        await this.answerContentBody.isAnswerVisible()
                            .should.eventually.be.equal(true, 'Текст ответа отображается');

                        return this.expect(actualUrl, 'Cсылка корректная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
        'Футер.': {
            'По умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3169',
                    async test() {
                        return this.questionFooter.isVisible()
                            .should.eventually.be.equal(true, 'Футер сниппета отображается');
                    },
                }),
            },
            'Ссылка с количеством комментариев.': {
                'По умолчанию': {
                    'содержит корректный текст': makeCase({
                        id: 'm-touch-3170',
                        async test() {
                            const {commentsCount} = this.params;
                            const pluralCommentsCount = pluralize(commentsCount,
                                'комментарий', 'комментария', 'комментариев');
                            const expectedText = `${commentsCount} ${pluralCommentsCount}`;

                            return this.questionFooter.getLinkText()
                                .should.eventually.be.equal(expectedText, 'Футер сниппета отображается');
                        },
                    }),
                    'ведет на страницу категорийного вопроса': makeCase({
                        id: 'm-touch-3171',
                        async test() {
                            const actualUrl = await this.questionFooter.getAnswersLink();
                            const expectedUrl = await this.browser.yaBuildURL('touch:category-question', {
                                categorySlug: this.params.categorySlug,
                                hid: this.params.categoryId,
                                questionSlug: this.params.questionSlug,
                                questionId: this.params.questionId,
                            });

                            return this.expect(actualUrl, 'Cсылка корректная')
                                .to.be.link(expectedUrl, {
                                    skipProtocol: true,
                                    skipHostname: true,
                                });
                        },
                    }),
                },
            },
        },
    },
});
