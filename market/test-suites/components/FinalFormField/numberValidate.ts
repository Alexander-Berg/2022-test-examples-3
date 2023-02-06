'use strict';

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на валидацию числового поля
 * @param {PageObject.FinalForm} form - форма
 * @param {Object} params
 * @param {string} params.label – текстовое название поля
 * @param {string} params.errorText– текст сообщения об ошибке
 */
export default makeSuite('Валидация числового поля формы.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        'При попытке ввода букв или других символов': {
            'значение поля не изменяется': makeCase({
                async test() {
                    const {label, errorText} = this.params;

                    await this.browser.allure.runStep(`Вводим в поле "${label}" текстовое значение`, () =>
                        this.form.setInputValue(label, 'Твоя мамка'),
                    );

                    await this.form
                        .getInputValue(label)
                        .should.eventually.be.equal('', `Значение поля "${label}" осталось пустым`);

                    await this.browser.allure.runStep(`Вводим в поле "${label}" различные символы`, () =>
                        this.form.setInputValue(label, '!?@#$%^&*()_-+=`~;,."\'[]{}\\|/<>'),
                    );

                    await this.form
                        .getInputValue(label)
                        .should.eventually.be.equal('', `Значение поля "${label}" осталось пустым`);

                    await this.form
                        .getFieldErrorMessageByLabelText(label)
                        .should.eventually.be.equal(errorText, `Текст ошибки у поля "${label}" корректный`);
                },
            }),
        },
    },
});
