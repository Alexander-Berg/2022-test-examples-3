'use strict';

import {makeCase, makeSuite} from 'ginny';

import {generateBigText} from 'spec/lib/helpers/generateBigText';

/**
 * Тест на валидацию текстового поля
 * @param {PageObject.FinalForm} form - форма
 * @param {Object} params
 * @param {string} params.label – текстовое название поля
 * @param {string} params.initialValue – изначальное значение поля, если не пустое
 * @param {string} params.maxLength – максимально допустимая длина вводимого значения
 * @param {string} params.errorText– текст сообщения об ошибке, если отличается от текста по умолчанию
 */
export default makeSuite('Валидация текстового поля формы.', {
    params: {
        user: 'Пользователь',
    },
    story: {
        'При вводе слишком длинного текста': {
            'отображается подсказка о необходимости ввода корректного значения': makeCase({
                async test() {
                    const {
                        label,
                        maxLength,
                        initialValue = '',
                        errorText = `Должно содержать не более ${maxLength} символов`,
                    } = this.params;

                    await this.form
                        .getInputValue(label)
                        .should.eventually.be.equal(initialValue, `У поля "${label}" корректное изначальное значение`);

                    const bigText = generateBigText(maxLength + 1);

                    await this.browser.allure.runStep(
                        `Вводим в поле "${label}" значение длиной ${maxLength + 1} символ`,
                        () => this.form.setInputValue(label, bigText),
                    );

                    await this.form
                        .getFieldErrorMessageByLabelText(label)
                        .should.eventually.be.equal(errorText, `Текст ошибки у поля "${label}" корректный`);
                },
            }),
        },
    },
});
