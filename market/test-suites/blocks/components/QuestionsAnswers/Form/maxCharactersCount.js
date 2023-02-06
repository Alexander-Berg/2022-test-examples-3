import {makeSuite, makeCase} from 'ginny';

/**
 * @param {PageObject.Form} form
 */
export default makeSuite('Форма добавления, максимальное кол-во символов', {
    story: {
        'Если введено максимальное количество символов и ввести ещё один': {
            'цвет счётчика сменится с серого на красный, а к количеству добавится 1': makeCase({
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
                    await this.form
                        .getCharacterLimitCounterColor()
                        .should.eventually.be.equal(
                            '#999999',
                            `Счётчик символов при ${this.params.charactersCount} имеет серый цвет`
                        );

                    await this.form.setText(`${'Y'.repeat(this.params.maxCharactersCount)}A`);

                    const overMaxCharacterCount = this.params.maxCharactersCount + 1;

                    await this.form
                        .getCharacterLimitCounterText()
                        .should.eventually.be.equal(
                            `${overMaxCharacterCount} / ${this.params.maxCharactersCount}`,
                            `Счётчик символов показывает ${overMaxCharacterCount}`
                        );
                    await this.form
                        .getCharacterLimitCounterColor()
                        .should.eventually.be.equal(
                            '#ec0000',
                            `Счётчик символов при ${overMaxCharacterCount} имеет красный цвет`
                        );
                },
            }),
        },
    },
});
