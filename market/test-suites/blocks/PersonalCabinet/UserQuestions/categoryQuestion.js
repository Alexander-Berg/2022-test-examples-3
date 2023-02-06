import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.CabinetQuestionSnippet} questionSnippet
 * @param {PageObject.QuestionHeader} questionHeader
 * @param {PageObject.QuestionFooter} questionFooter
 */
export default makeSuite('Сниппет категорийного вопроса.', {
    environment: 'kadavr',
    params: {
        categorySlug: 'слаг продукта',
        categoryId: 'id продукта',
        questionSlug: 'слаг вопроса',
        questionId: 'id вопроса',
    },
    story: {
        'Ссылка на категорию': {
            'по умолчанию': {
                'отображается и ведет на страницу вопросов о категории': makeCase({
                    id: 'm-touch-3126',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        const currentHref = await this.questionHeader.getHeaderLink();
                        const expectedPath = await this.browser.yaBuildURL('touch:category-questions', {
                            slug: this.params.categorySlug,
                            hid: this.params.categoryId,
                        });

                        await this.questionHeader.headerLink.isVisible()
                            .should.eventually.to.be.equal(true,
                                'Ссылка на страницу категорийных вопросов отображается');
                        await this.expect(currentHref, 'ссылка корректная')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
        'Текст вопроса': {
            'по умолчанию': {
                'содержит ссылку на вопрос.': makeCase({
                    id: 'm-touch-3120',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        const currentHref = await this.questionSnippet.getQuestionLink();

                        const expectedPath = await this.browser.yaBuildURL('touch:category-question', {
                            categorySlug: this.params.categorySlug,
                            hid: this.params.categoryId,
                            questionSlug: this.params.questionSlug,
                            questionId: this.params.questionId,
                        });

                        await this.expect(currentHref, 'ссылка корректная')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
        'Ссылка количества ответов на вопрос': {
            'по умолчанию': {
                'отображается и ведет на страницу вопроса': makeCase({
                    id: 'm-touch-3121',
                    issue: 'MARKETFRONT-6439',
                    async test() {
                        const currentHref = await this.questionFooter.getAnswersLink();

                        const expectedPath = await this.browser.yaBuildURL('touch:category-question', {
                            categorySlug: this.params.categorySlug,
                            hid: this.params.categoryId,
                            questionSlug: this.params.questionSlug,
                            questionId: this.params.questionId,
                        });

                        await this.questionFooter.answersLink.isVisible()
                            .should.eventually.to.be.equal(true, 'Ссылка количества ответов отображается');
                        await this.expect(currentHref, 'ссылка корректная')
                            .to.be.link(expectedPath, {
                                skipProtocol: true,
                                skipHostname: true,
                            });
                    },
                }),
            },
        },
    },
});
