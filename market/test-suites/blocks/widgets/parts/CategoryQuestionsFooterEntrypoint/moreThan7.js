import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.CategoryQuestionsFooterEntrypoint} categoryQuestionsFooterEntrypoint
 */
export default makeSuite('Точка входа на категорийные вопросы в подвале. Больше 7 вопросов.', {
    id: 'marketfront-3580',
    issue: 'MARKETVERSTKA-34979',
    feature: 'Q&A Категории',
    story: {
        'По умолчанию': {
            'отображается': makeCase({
                async test() {
                    return this.categoryQuestionsFooterEntrypoint.isVisible()
                        .should.eventually.be.equal(true, 'Блок отображается');
                },
            }),
        },
        'Кнопка "Задать вопрос"': {
            'содержит правильную ссылку': makeCase({
                params: {
                    expectedUrl: 'Ожидаемый адрес ссылки',
                },
                async test() {
                    const link = await this.categoryQuestionsFooterEntrypoint.getAskUrl();
                    return this.expect(link)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'Ссылка корректная');
                },
            }),
        },
        'Кнопка "Ещё N вопросов"': {
            'содержит правильную ссылку': makeCase({
                params: {
                    expectedUrl: 'Ожидаемый адрес ссылки',
                },
                async test() {
                    const link = await this.categoryQuestionsFooterEntrypoint.getMoreUrl();
                    return this.expect(link)
                        .to.be.link(this.params.expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        }, 'Ссылка корректная');
                },
            }),
        },
    },
});
