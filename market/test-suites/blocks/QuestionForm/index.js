import {makeCase, makeSuite} from 'ginny';

/**
 * @param {PageObject.QuestionForm} questionForm
 */
export default makeSuite('Элементы управления блока формы вопроса', {
    story: {
        'при клике на крестик': {
            'форма безвозвратно очищается': makeCase({
                id: 'm-touch-2216',
                issue: 'MOBMARKET-8997',
                feature: 'Добавление контента',
                async test() {
                    const textFieldInput = 'test text';
                    await this.questionForm.clickTextField();
                    await this.browser.yaWaitUntilLocalStorageSet({
                        action: () => this.questionForm.setTextFieldInput(textFieldInput),
                        key: this.params.localStorageKey,
                        value: textFieldInput,
                    });
                    await this.browser.yaWaitUntilLocalStorageDelete({
                        action: () => this.questionForm.clickClearButton(),
                        key: this.params.localStorageKey,
                    });
                    const input = await this.questionForm.getTextFieldInput();
                    await this.expect(input).to.equal('', 'Поле ввода очищено');
                    await this.browser.refresh();
                    await this.questionForm.clickTextField();
                    await this.questionForm.getTextFieldInput()
                        .should.eventually.to.be.equal('', 'Поле ввода очищено');
                },
            }),
        },
        'при клике "отмена" вопрос можно восстановить': {
            'форма содержит введенный текст': makeCase({
                id: 'm-touch-2215',
                issue: 'MOBMARKET-8996',
                feature: 'Добавление контента',
                async test() {
                    const textFieldInput = 'test text';
                    await this.questionForm.clickTextField();
                    await this.browser.yaWaitUntilLocalStorageSet({
                        action: () => this.questionForm.setTextFieldInput(textFieldInput),
                        key: this.params.localStorageKey,
                        value: textFieldInput,
                    });
                    await this.questionForm.clickCancel();
                    await this.browser.refresh();
                    await this.questionForm.clickTextField();
                    await this.questionForm.getTextFieldInput()
                        .should.eventually.to.be.equal(textFieldInput, 'Текст в поле ввода восстановлен');
                    await this.browser.yaWaitUntilLocalStorageDelete({
                        action: () => this.questionForm.clickClearButton(),
                        key: this.params.localStorageKey,
                    });
                    await this.questionForm.clickTextField();
                    await this.questionForm.getTextFieldInput()
                        .should.eventually.to.be.equal('', 'Поле ввода очищено');
                },
            }),
        },
    },
});
