import assert from 'assert';

export const setFieldFocus = {
    name: 'Установка фокуса в поле ввода.',
    /**
     * Установка фокуса в поле ввода.
     * @param {string} [selector] - селектор поля
     * @param {boolean} [isEmpty] - флаг для проверки на пустоту
     * @returns {Promise<void>}
     */
    async func({selector, isEmpty = false}) {
        assert(this.browser, 'browser must be defined');
        assert(this.recipientFormFields, 'recipientForm must be defined');

        await this.recipientFormFields.focusField(selector);
        await this.browser.allure.runStep(
            'Поле ввода отображается активным.',
            async () => {
                await this.recipientFormFields.isFocusedField(selector);
                await this.recipientFormFields.isVisibleInputYellowOutline(selector)
                    .should.eventually.to.be.equal(
                        true,
                        'Поле ввода должно быть отображаться с желтой рамкой.'
                    );
            }
        );

        if (!isEmpty) {
            await this.recipientFormFields.clearButtonIsVisible(selector)
                .should.eventually.to.be.equal(
                    true,
                    'Кнопка "Х" должна отображаться.'
                );
        } else {
            await this.recipientFormFields.clearButtonIsVisible(selector)
                .should.eventually.to.be.equal(
                    false,
                    'Кнопка "Х" не должна отображаться.'
                );
        }
    },
};

export const setFieldText = {
    name: 'Установка текста в поле ввода.',
    /**
     * Установка текста в поле ввода.
     * @param {string} [selector] - селектор поля
     * @param {string} [text] - текст поля
     * @returns {Promise<void>}
     */
    async func({selector, text}) {
        assert(this.browser, 'browser must be defined');
        assert(this.recipientFormFields, 'recipientForm must be defined');

        await this.recipientFormFields.setFieldValue(selector, text);
        await this.browser.allure.runStep(
            'В поле ввода отображается веденный текст.',
            async () => {
                await this.recipientFormFields.getFieldValue(selector)
                    .should.eventually.to.be.equal(
                        text,
                        `В поле ввода должно быть "${text}".`
                    );
            }
        );
    },
};

export const clickByClearButton = {
    name: 'Клик по кнопке "Х".',
    /**
     * Клик по кнопке "Х".
     * @param {string} [selector] - селектор поля
     * @returns {Promise<void>}
     */
    async func({selector}) {
        assert(this.browser, 'browser must be defined');
        assert(this.recipientFormFields, 'recipientForm must be defined');

        await this.recipientFormFields.clearByField(selector);
        await this.browser.allure.runStep(
            'Поле ввода очищается.',
            async () => {
                await this.recipientFormFields.getFieldValue(selector)
                    .should.eventually.to.be.equal(
                        '',
                        'Поле ввода должно быть пустым'
                    );
            }
        );

        await this.recipientFormFields.clearButtonIsVisible(selector)
            .should.eventually.to.be.equal(
                false,
                'Кнопка "Х" не должна отображаться.'
            );

        await this.browser.allure.runStep(
            'Поле ввода отображается активным.',
            async () => {
                await this.recipientFormFields.isFocusedField(selector);
                await this.recipientFormFields.isVisibleInputYellowOutline(selector)
                    .should.eventually.to.be.equal(
                        true,
                        'Поле ввода должно быть отображаться с желтой рамкой.'
                    );
            }
        );
    },
};

export const removeFocusFromField = {
    name: 'Убрать фокус из поля ввода.',
    /**
     * Убрать фокус из поля ввода.
     * @param {string} [selector] - селектор поля
     * @param {string} [expectedError] - ожидаемая ошибка под полем ввода
     * @returns {Promise<void>}
     */
    async func({selector, expectedError}) {
        assert(this.browser, 'browser must be defined');
        assert(this.recipientFormFields, 'recipientForm must be defined');

        await this.recipientFormFields.removeFocusFromField(selector);

        if (expectedError) {
            await this.recipientFormFields.isVisibleInputRedOutline(selector)
                .should.eventually.to.be.equal(
                    true,
                    'Поле ввода должно отображаться в красной рамке.'
                );

            await this.recipientFormFields.getInputErrored(selector)
                .should.eventually.to.be.equal(
                    expectedError,
                    `Под полем ввода должна отображаться ошибка "${expectedError}".`
                );
        } else {
            await this.recipientFormFields.isInputErrored(selector)
                .should.eventually.to.be.equal(
                    false,
                    'Под полем ввода не должна отображаться ошибка.'
                );
        }
    },
};
