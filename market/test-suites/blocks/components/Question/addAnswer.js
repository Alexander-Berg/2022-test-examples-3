import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 * @param {PageObject.Answer} answerSnippet
 * @param {PageObject.InlineNotification} inlineNotification
 */
export default makeSuite('Форма добавления ответа, сохранение, «зелеблок»', {
    feature: 'Добавление ответа',
    story: {
        'При добавлении ответа': {
            'происходит добавление в список ответов с введённым в форме текстом и зелеблоком': makeCase({
                feature: 'Контент страницы',
                async test() {
                    await this.form.clickTextarea();
                    await this.form.setText('В них можно слушать русский рэп');
                    const isButtonActive = this.form.isSubmitButtonActive();
                    await this.expect(isButtonActive).to.be.equal(true, 'Кнопка Отправить активна');
                    await this.form.clickSubmitButton();

                    const isGreenBlockVisible = await this.inlineNotification.isVisible();
                    await this.expect(isGreenBlockVisible).to.be.equal(true, 'Зелёный блок виден');

                    await this.answerSnippet.isExisting()
                        .should.eventually.to.be.equal(true, 'Присутствует сниппет с ответом на вопрос');

                    const answerText = await this.answerSnippet.getText();

                    await this.expect(answerText).to.be.equal(
                        'В них можно слушать русский рэп',
                        'Текст нового ответа совпадает с введённым'
                    );
                },
            }),
        },
    },
});
