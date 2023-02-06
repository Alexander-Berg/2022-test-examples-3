import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CategoryQuestionsTopEntrypoint} categoryQuestionsTopEntrypoint
 */
export default makeSuite('Точка входа на категорийные вопросы в шапке.', {
    id: 'marketfront-3567',
    issue: 'MARKETVERSTKA-34977',
    feature: 'Q&A Категории',
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                async test() {
                    return this.categoryQuestionsTopEntrypoint.isVisible()
                        .should.eventually.be.equal(true, 'Точка входа отображается');
                },
            }),
            'содержит правильную ссылку': makeCase({
                params: {
                    expectedUrl: 'Ожидаемый адрес ссылки',
                },
                async test() {
                    const link = await this.categoryQuestionsTopEntrypoint.getLinkUrl();
                    return this.expect(link)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'Ссылка кнопки всех вопросов корректная');
                },
            }),
            'содержит правильный текст': makeCase({
                params: {
                    expectedQuestionCount: 'Ожидаемое количество вопросов',
                },
                async test() {
                    const text = await this.categoryQuestionsTopEntrypoint.getLinkText();
                    return this.expect(text)
                        .to.be.equal(
                            `Вопросы о товарах ${this.params.expectedQuestionCount}`,
                            'Текст кнопки всех вопросов корректный'
                        );
                },
            }),
        },
    },
});
