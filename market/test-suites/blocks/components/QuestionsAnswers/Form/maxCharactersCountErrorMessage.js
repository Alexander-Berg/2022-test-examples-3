import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления, кнопка "Отправить", максимальное кол-во символов', {
    story: {
        'Если введено максимальное количество символов и ввести ещё один': {
            'кнопка "Отправить" активна': makeCase({
                params: {
                    charactersCount: 'количество введенных символов',
                    maxCharactersCount: 'максимальное количество символов в форме',
                },
                async test() {
                    await this.form.clickTextarea();

                    await this.form.setText(`${'Y'.repeat(this.params.maxCharactersCount)}A`);

                    return this.expect(this.form.isSubmitButtonActive()).eventually.to.be.equal(true);
                },
            }),
            'появляется сообщение об ошибке': makeCase({
                params: {
                    charactersCount: 'количество введенных символов',
                    maxCharactersCount: 'максимальное количество символов в форме',
                },
                async test() {
                    await this.form.clickTextarea();

                    await this.form.setText(`${'Y'.repeat(this.params.maxCharactersCount)}A`);

                    return this.expect(this.form.getErrorText())
                        .eventually.to.be.equal('Это слишком длинный ответ, сформулируйте покороче');
                },
            }),
        },
    },
});
