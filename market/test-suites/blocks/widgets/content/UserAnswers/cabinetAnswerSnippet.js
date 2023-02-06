import {makeCase, makeSuite, mergeSuites} from 'ginny';

import CabinetAnswerSnippet from '@self/platform/widgets/content/UserAnswers/components/CabinetAnswerSnippet/__pageObject';

export default makeSuite('Сниппет ответа пользователя.', {
    params: {
        questionText: 'Текст вопроса',
        questionContentLink: 'Ссылка, куда ведет текст вопроса',
        answerText: 'Текст ответа',
        answerContentLink: 'Ссылка, куда ведет текст ответа',
    },
    story: mergeSuites({
        async beforeEach() {
            await this.setPageObjects({
                cabinetAnswerSnippet: () => this.createPageObject(CabinetAnswerSnippet),
            });
        },
        'По умолчанию': {
            'отображается': makeCase({
                id: 'marketfront-3866',
                async test() {
                    return this.cabinetAnswerSnippet.isVisible()
                        .should.eventually.be.equal(true, 'Блок с вопросом отображается');
                },
            }),
            'содержит корректный текст вопроса': makeCase({
                id: 'marketfront-3867',
                async test() {
                    const expectedText = this.params.questionText;

                    return this.cabinetAnswerSnippet.getQuestionText()
                        .should.eventually.be.equal(expectedText, 'Текст вопроса корректный');
                },
            }),
            'содержит корректную ссылку, на которую ведет текст вопроса': makeCase({
                id: 'marketfront-3868',
                async test() {
                    const expectedUrl = this.params.questionContentLink;
                    const actualUrl = this.cabinetAnswerSnippet.getQuestionContentLink();

                    return this.expect(actualUrl, 'Cсылка корректная')
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
            'содержит корректный текст ответа': makeCase({
                id: 'marketfront-3869',
                async test() {
                    const expectedText = this.params.answerText;

                    return this.cabinetAnswerSnippet.getAnswerText()
                        .should.eventually.be.equal(expectedText, 'Текст ответа корректный');
                },
            }),
            'содержит корректную ссылку, на которую ведет текст ответа': makeCase({
                id: 'marketfront-3870',
                async test() {
                    const expectedUrl = this.params.answerContentLink;
                    const actualUrl = this.cabinetAnswerSnippet.getAnswerContentLink();

                    return this.expect(actualUrl, 'Cсылка корректная')
                        .to.be.link(expectedUrl, {
                            skipProtocol: true,
                            skipHostname: true,
                        });
                },
            }),
        },
    }),
});
