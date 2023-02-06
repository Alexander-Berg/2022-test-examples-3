'use strict';

import {makeCase, makeSuite} from 'ginny';

import {generateBigText} from 'spec/lib/helpers/generateBigText';

/**
 * Тест на валидацию текстового поля
 * @param {PageObject.Form} form - форма
 * @param {Object} params
 * @param {string} params.name – ключ поля в форме
 * @param {string} params.errorName – ключ попапа с ошибкой
 * @param {string} params.label – текстовое название поля
 * @param {string} params.initialValue – изначальное значение поля
 * @param {string} params.maxLength – максимально допустимая длина вводимого значения
 * @param {string} params.errorText– итоговый текст ошибки в попапе, если отличается от текста по умолчанию
 * @param {string} params.selector – селектор основного элемента FormField. По умолчанию input[type="text"],
 * но встречается и textarea, и другие элементы.
 */
export default makeSuite('Валидация текстового поля формы.', {
    feature: 'Форма',
    environment: 'kadavr',
    params: {
        user: 'Пользователь',
    },
    story: {
        'При вводе слишком длинного текста': {
            'отображается хинт о необходимости ввода корректного значения': makeCase({
                async test() {
                    const {
                        name,
                        label,
                        initialValue,
                        maxLength,
                        selector,
                        errorName = name,
                        errorText = `Должно содержать не более ${maxLength} символов`,
                    } = this.params;

                    await this.form
                        .getFieldValue(name, selector)
                        .should.eventually.be.equal(initialValue, `У поля "${label}" корректное изначальное значение`);

                    const bigText = generateBigText(maxLength + 1);

                    await this.browser.allure.runStep(
                        `Вводим в поле "${label}" значение длиной ${maxLength + 1} символ`,
                        () => this.form.getFieldByName(name, selector).vndSetValue(bigText),
                    );

                    await this.form
                        .getFieldValidationErrorPopup(errorName)
                        .isVisible()
                        .should.eventually.be.equal(true, `Сообщение об ошибке у поля "${label}" отображается`);

                    await this.form
                        .getFieldValidationErrorPopup(errorName)
                        .getText()
                        .should.eventually.be.equal(errorText, 'Текст ошибки корректный');
                },
            }),
        },
    },
});
