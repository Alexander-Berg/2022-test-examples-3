import {makeCase, makeSuite} from 'ginny';

import {pluralize} from '@self/project/src/helpers/string';

/**
 * @param {PageObject.widgets.content.UserQuestions.components.QuestionHeader} questionHeader
 * @param {PageObject.widgets.content.UserAnswers.components.CabinetAnswerSnippet} cabinetAnswerSnippet
 * @param {PageObject.widgets.content.UserAnswers.components.AnswerContentBody} answerContentBody
 * @param {PageObject.widgets.content.UserQuestions.components.QuestionFooter} questionFooter
 * @param {PageObjects.components.VoteButton} voteButton
 */

export default makeSuite('Продуктовый сниппет ответа.', {
    params: {
        productId: 'Id продукта',
        productSlug: 'Слаг продукта',
        answerId: 'Id ответа',
        questionId: 'Id вопроса',
        questionSlug: 'Слаг вопроса',
        commentsCount: 'Количество комментариев к ответу',
        likesCount: 'Количество лайков ответа',
    },
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                id: 'm-touch-3147',
                async test() {
                    return this.cabinetAnswerSnippet.isVisible()
                        .should.eventually.be.equal(true, 'Сниппет ответа отображается');
                },
            }),
        },
        'Шапка.': {
            'По умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3148',
                    async test() {
                        return this.questionHeader.isVisible()
                            .should.eventually.be.equal(true, 'Шапка отображается');
                    },
                }),
                'содержит ссылку на продукт': makeCase({
                    id: 'm-touch-3149',
                    async test() {
                        const actualUrl = await this.questionHeader.getHeaderLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:product', {
                            slug: this.params.productSlug,
                            productId: this.params.productId,
                        });

                        return this.expect(actualUrl, 'Cсылка корректная')
                            .to.be.link(expectedUrl, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
                'содержит картинку/заглушку продукта': makeCase({
                    id: 'm-touch-3150',
                    async test() {
                        return this.questionHeader.isProductImageVisible()
                            .should.eventually.be.equal(true, 'Картинка товара отображается');
                    },
                }),
                'не содержит крестик удаления товара': makeCase({
                    id: 'm-touch-3151',
                    async test() {
                        return this.cabinetAnswerSnippet.isRemoveButtonVisible()
                            .should.eventually.be.equal(false, 'Крестик для удаления ответа отображается');
                    },
                }),
            },
        },
        'Тело сниппета.': {
            'По умолчанию': {
                'отображается': makeCase({
                    id: 'm-touch-3152',
                    async test() {
                        return this.answerContentBody.isVisible()
                            .should.eventually.be.equal(true, 'Тело сниппета отображается');
                    },
                }),
                'содержит аватарку пользователя': makeCase({
                    id: 'm-touch-3153',
                    async test() {
                        return this.answerContentBody.isAvatarVisible()
                            .should.eventually.be.equal(true, 'Аватарка пользователя отображается');
                    },
                }),
                'содержит дату создания': makeCase({
                    id: 'm-touch-3154',
                    async test() {
                        return this.answerContentBody.isCreationDateVisible()
                            .should.eventually.be.equal(true, 'Дата создания ответа отображается');
                    },
                }),
                'текст вопроса отображается и содержит ссылку на вопрос': makeCase({
                    id: 'm-touch-3155',
                    async test() {
                        const actualUrl = await this.answerContentBody.getQuestionLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:product-question', {
                            productSlug: this.params.productSlug,
                            productId: this.params.productId,
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
                'текст ответа отображается и содержит ссылку на ответ': makeCase({
                    id: 'm-touch-3156',
                    async test() {
                        const actualUrl = await this.answerContentBody.getAnswerLink();
                        const expectedUrl = await this.browser.yaBuildURL('touch:product-question-answer', {
                            answerId: this.params.answerId,
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
                    id: 'm-touch-3157',
                    async test() {
                        return this.questionFooter.isVisible()
                            .should.eventually.be.equal(true, 'Футер сниппета отображается');
                    },
                }),
            },
            'Ссылка с количеством комментариев.': {
                'По умолчанию': {
                    'содержит корректный текст': makeCase({
                        id: 'm-touch-3158',
                        async test() {
                            const {commentsCount} = this.params;
                            const pluralCommentsCount = pluralize(commentsCount,
                                'комментарий', 'комментария', 'комментариев');
                            const expectedText = `${commentsCount} ${pluralCommentsCount}`;

                            return this.questionFooter.getLinkText()
                                .should.eventually.be.equal(expectedText, 'Футер сниппета отображается');
                        },
                    }),
                    'ведет на страницу ответа': makeCase({
                        id: 'm-touch-3159',
                        async test() {
                            const actualUrl = await this.questionFooter.getAnswersLink();
                            const expectedUrl = await this.browser.yaBuildURL('touch:product-question-answer', {
                                answerId: this.params.answerId,
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
            'Голосовалка.': {
                'По умолчанию': {
                    'отображает верное количество лайков': makeCase({
                        id: 'm-touch-3160',
                        async test() {
                            return this.voteButton.getVotesCount()
                                .should.eventually.be.equal(this.params.likesCount, 'Количество лайков верное');
                        },
                    }),
                },
                'При проставлении лайка': {
                    'количество лайков увеличивается': makeCase({
                        id: 'm-touch-3161',
                        async test() {
                            const {likesCount} = this.params;

                            await this.browser.yaWaitForChangeValue({
                                action: () => this.voteButton.clickVote(),
                                valueGetter: () => this.voteButton.getVotesCount(),
                            });

                            await this.voteButton.getVotesCount()
                                .should.eventually.be.greaterThan(likesCount, 'Количество лайков увеличилось');

                            await this.browser.refresh();

                            return this.voteButton.getVotesCount()
                                .should.eventually.be.equal(likesCount + 1, 'Количество лайков увеличилось на 1');
                        },
                    }),
                },
                'При удвоенном клике': {
                    'количество лайков не изменяется': makeCase({
                        id: 'm-touch-3162',
                        async test() {
                            const {likesCount} = this.params;

                            await this.browser.allure.runStep(
                                'Ставим лайк своему ответу',
                                () => this.browser.yaWaitForChangeValue({
                                    action: () => this.voteButton.clickVote(),
                                    valueGetter: () => this.voteButton.getVotesCount(),
                                })
                            );

                            const afterClickLikesCount = await this.voteButton.getVotesCount();
                            await this.expect(afterClickLikesCount)
                                .to.be.greaterThan(likesCount, 'Количество лайков увеличилось');


                            await this.browser.allure.runStep(
                                'Убираем свой только что проставленный лайк',
                                () => this.browser.yaWaitForChangeValue({
                                    action: () => this.voteButton.clickVote(),
                                    valueGetter: () => this.voteButton.getVotesCount(),
                                })
                            );

                            await this.voteButton.getVotesCount()
                                .should.eventually.be.lessThan(afterClickLikesCount, 'Количество лайков уменьшилось');

                            await this.browser.refresh();

                            return this.voteButton.getVotesCount()
                                .should.eventually.be.equal(likesCount + 1, 'Количество лайков не изменилось');
                        },

                    }),
                },
            },
        },
    },
});
