import {makeCase, makeSuite} from 'ginny';

import {
    clickByClearButton,
    removeFocusFromField,
    setFieldFocus,
    setFieldText,
} from './helpers';

export default makeSuite('Валидация поля.', {
    params: {
        selector: 'Селектор поля ввода',
        recipientBeforeEdit: 'Текст поля по умолчанию',
        validText: 'Валидный текст',
        additionalText: 'Добавляемый текст с ожидаемой ошибкой',
        emptyText: 'Текст ошибки при пустом поле ввода',
        invalidText: 'Невалидный текст с ожидаемой ошибкой',
    },
    environment: 'kadavr',
    story: {
        'С последующим сохранением внесенных изменений.': makeCase({
            async test() {
                const {
                    selector,
                    fieldName,
                    validText,
                    additionalText,
                    emptyText,
                    invalidText,
                    editedRecipient,
                } = this.params;
                const {recipientBeforeEdit} = this.params;

                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                    }
                );

                await this.allure.runStep(
                    'В блоке "Получатель" отображается информация о получателе.', async () => {
                        await this.recipientBlock.getContactText()
                            .should.eventually.to.be.equal(
                                recipientBeforeEdit,
                                'На карточке получателя должна отображаться информация о получателе.'
                            );
                    }
                );

                await this.allure.runStep(
                    'В блоке "Получатель" нажать на кнопку "Карандаш".', async () => {
                        await this.recipientBlock.onClick();

                        await this.recipientList.waitForVisible();
                        await this.recipientList.clickEditButtonByRecipient(recipientBeforeEdit);

                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recipientForm.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    `Тапнуть по полю "${fieldName}".`, () =>
                        this.browser.yaScenario(
                            this,
                            setFieldFocus,
                            {selector}
                        )
                );

                if (additionalText) {
                    await this.browser.allure.runStep(
                        `В поле ввода ввести текст "${additionalText.text}".`,
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                setFieldText,
                                {
                                    selector,
                                    text: additionalText.text,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Убрать фокус, проверить наличие ошибки валидации.',
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                removeFocusFromField,
                                {
                                    selector,
                                    expectedError: additionalText.expectedError,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        `Тапнуть по полю "${fieldName}".`, () =>
                            this.browser.yaScenario(
                                this,
                                setFieldFocus,
                                {selector}
                            )
                    );
                }

                if (emptyText) {
                    await this.browser.yaScenario(
                        this,
                        clickByClearButton,
                        {selector}
                    );

                    await this.browser.allure.runStep(
                        'Убрать фокус, проверить наличие ошибки валидации.',
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                removeFocusFromField,
                                {
                                    selector,
                                    expectedError: emptyText.expectedError,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        `Тапнуть по полю "${fieldName}".`, () =>
                            this.browser.yaScenario(
                                this,
                                setFieldFocus,
                                {
                                    selector,
                                    isEmpty: true,
                                }
                            )
                    );
                }

                if (invalidText) {
                    await this.browser.allure.runStep(
                        `В поле ввода ввести текст "${invalidText.text}".`,
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                setFieldText,
                                {
                                    selector,
                                    text: invalidText.text,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Убрать фокус, проверить наличие ошибки валидации.',
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                removeFocusFromField,
                                {
                                    selector,
                                    expectedError: invalidText.expectedError,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        `Тапнуть по полю "${fieldName}".`, () =>
                            this.browser.yaScenario(
                                this,
                                setFieldFocus,
                                {selector}
                            )
                    );
                }

                if (validText) {
                    await this.browser.allure.runStep(
                        `В поле ввода ввести текст "${validText.text}".`,
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                setFieldText,
                                {
                                    selector,
                                    text: validText.text,
                                }
                            );
                        }
                    );

                    await this.browser.allure.runStep(
                        'Убрать фокус, проверить наличие ошибки валидации.',
                        async () => {
                            await this.browser.yaScenario(
                                this,
                                removeFocusFromField,
                                {selector}
                            );
                        }
                    );
                }

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Сохранить".',
                    async () => {
                        await this.recipientPopup.submitButtonClick();

                        await this.browser.allure.runStep(
                            'На экране появляется попап "Получатель".',
                            async () => {
                                await this.recipientList.waitForVisible();
                            }
                        );

                        await this.browser.allure.runStep(
                            'Пресет редактируемого получателя с внесенными изменениями отображается активным.',
                            async () => {
                                await this.recipientList.isItemWithRecipientChecked(editedRecipient)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'В пресете должны отображаться введенные данные.'
                                    );
                            }
                        );
                    }
                );
            },
        }),
    },
});
