import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления вопроса, счётчик кол-ва символов', {
    feature: 'Добавление вопроса',
    story: {
        'Если введен текст вопроса': {
            'в счётчике символов отображается длина введённого текста': makeCase({
                params: {
                    charactersCount: 'количество введенных символов',
                    maxCharactersCount: 'максимальное количество символов в форме',
                },
                async test() {
                    await this.form.clickTextarea();

                    await this.form.setText('Y'.repeat(this.params.charactersCount));
                    await this.form
                        .getCharacterLimitCounterText()
                        .should.eventually.be.equal(
                            `${this.params.charactersCount} / ${this.params.maxCharactersCount}`,
                            `Счётчик символов показывает ${this.params.charactersCount}`
                        );
                },
            }),
        },
    },
});
