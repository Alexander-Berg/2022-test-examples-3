import {makeSuite, makeCase} from 'ginny';

export default makeSuite('Блок "Часто спрашивают"', {
    feature: 'Ответы на частые вопросы',
    story: {
        'По умолчанию': {
            'отображается блок “Часто спрашивают”': makeCase({
                id: 'bluemarket-2833',
                issue: 'BLUEMARKET-6972',
                async test() {
                    await this.frequentQuestions.isVisible()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что блок “Часто спрашивают” виден на странице'
                        );
                    const title = 'Часто спрашивают';
                    await this.expect(this.frequentQuestions.getTitle())
                        .to.be.equal(title, `Текст заголовка должен быть “${title}”`);
                },
            }),

            'ответы на вопросы скрыты': makeCase({
                id: 'bluemarket-2833',
                issue: 'BLUEMARKET-6972',
                async test() {
                    await this.frequentQuestions.questionAnswerByIndex(0).isVisible()
                        .should.eventually.to.equal(
                            false,
                            'Проверяем, что ответы на вопросы не видны на странице'
                        );
                },
            }),
        },

        'При клике на вопрос': {
            'отображается ответ на него': makeCase({
                id: 'bluemarket-2833',
                issue: 'BLUEMARKET-6972',
                async test() {
                    await this.frequentQuestions.clickOnQuestion(0);

                    await this.frequentQuestions.questionAnswerByIndex(0).isVisible()
                        .should.eventually.to.equal(
                            true,
                            'Проверяем, что ответ на 1ый вопрос появился на странице'
                        );
                },
            }),
        },
    },
});
