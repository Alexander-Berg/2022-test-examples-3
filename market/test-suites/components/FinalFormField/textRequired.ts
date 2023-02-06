'use strict';

import {makeCase, makeSuite} from 'ginny';

/**
 * Тест на валидацию текстового поля
 * @param {PageObject.FinalForm} form - форма
 * @param {Object} params
 * @param {string} params.label – текстовое название поля
 * @param {string} params.errorText– текст сообщения об ошибке
 */
export default makeSuite('Валидация обязательного для заполнения поля формы.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        'При удалении значения из поля': {
            'появляется сообщение о необходимости его заполнить': makeCase({
                async test() {
                    const {label, errorText} = this.params;

                    await this.browser.allure.runStep(`Вводим в поле "${label}" значение`, () =>
                        this.form.setInputValue(label, 'Твоя мамка'),
                    );

                    await this.browser.allure.runStep(`Стираем значение из поля "${label}"`, () =>
                        this.form.setInputValue(label, ''),
                    );

                    await this.form
                        .getFieldErrorMessageByLabelText(label)
                        .should.eventually.be.equal(errorText, `Текст ошибки у поля "${label}" корректный`);
                },
            }),
        },
    },
});
