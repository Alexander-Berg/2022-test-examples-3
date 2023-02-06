import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.QuestionSnippet} questionSnippet
 * @param {PageObject.InlineNotification} inlineNotification
 */
export default makeSuite('Форма добавления вопроса, сохранение, переход и «зелеблок»', {
    feature: 'Добавление вопроса',
    story: {
        'При добавлении вопроса': {
            'происходит переход на страницу вопроса с введённым в форме текстом и зелеблоком': makeCase({
                async test() {
                    await this.form.clickTextarea();
                    await this.form.setText('Мой новый вопрос');
                    await this.browser.yaWaitForPageReloadedExtended(
                        () => this.form.clickSubmitButton(),
                        5000
                    );

                    const isGreenBlockVisible = await this.inlineNotification.isVisible();
                    await this.expect(isGreenBlockVisible).to.be.equal(true, 'Зелёный блок виден');

                    const questionText = await this.questionSnippet.getQuestionText();
                    await this.expect(questionText).to.be.equal(
                        'Мой новый вопрос',
                        'Текст нового вопроса совпадает с введённым'
                    );
                },
            }),
        },
    },
});
