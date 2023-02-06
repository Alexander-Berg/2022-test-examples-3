import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления вопроса, очистка поля ввода вопроса', {
    feature: 'Добавление вопроса',
    story: {
        'При введённом тексте вопроса': {
            'если нажать на кнопку очистки, поле очистится': makeCase({
                async test() {
                    await this.form.clickTextarea();
                    await this.form.setText('Мой новый вопрос');
                    await this.form
                        .getText()
                        .should.eventually.be.equal('Мой новый вопрос', 'Вопрос напечатан в поле ввода');
                    await this.form.clickClearTextFieldButton();
                    await this.form
                        .getText()
                        .should.eventually.be.equal('', 'Поле ввода очищено');
                    await this.browser.refresh();
                    await this.browser.yaWaitForPageReady();
                    await this.form.clickTextarea();
                    await this.form
                        .getText()
                        .should.eventually.be.equal('', 'После перезагрузки поле ввода пустое');
                },
            }),
        },
    },
});
