import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления вопроса, бэкап поля ввода вопроса', {
    feature: 'Добавление вопроса',
    story: {
        'При введённом тексте вопроса': {
            'если перезагрузить страницу и кликнуть по полю, текст восстановится': makeCase({
                async test() {
                    await this.form.clickTextarea();

                    await this.browser.yaWaitUntilLocalStorageSet({
                        action: async () => {
                            await this.form.setText('Мой новый вопрос');
                            await this.form
                                .getText()
                                .should.eventually.be.equal('Мой новый вопрос', 'Вопрос напечатан в поле ввода');
                        },
                        key: this.params.localStorageKey,
                        value: 'Мой новый вопрос',
                    });

                    await this.browser.refresh();
                    await this.browser.yaWaitForPageReady();
                    await this.form.clickTextarea();
                    await this.form
                        .getText()
                        .should.eventually.be.equal(
                            'Мой новый вопрос',
                            'После перезагрузки поле ввода содержит ранее введённый вопрос'
                        );
                },
            }),
        },
    },
});
