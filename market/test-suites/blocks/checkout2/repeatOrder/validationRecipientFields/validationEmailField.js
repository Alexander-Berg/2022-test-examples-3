import {makeCase, makeSuite} from 'ginny';

import {
    focusFromRecipientFormField,
    removeFocusFromRecipientFormField,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';

import {CONTACTS} from '../../constants';

export default makeSuite('Валидация поля "Электронная почта" с последующим сохранением внесенных изменений', {
    id: 'marketfront-5035',
    issue: 'MARKETFRONT-54580',
    feature: 'Валидация поля "Электронная почта" с последующим сохранением внесенных изменений',
    params: {
        region: 'Регион',
        isAuthWithPlugin: 'Авторизован ли пользователь',
    },
    defaultParams: {
        region: region['Москва'],
        isAuth: false,
    },
    environment: 'kadavr',
    story: {
        'Открыть страницу чекаута.': makeCase({
            async test() {
                const recipient =
                    `${CONTACTS.DEFAULT_CONTACT.recipient}\n${CONTACTS.DEFAULT_CONTACT.email}, ${CONTACTS.DEFAULT_CONTACT.phone}`;
                const emailField = RecipientFormFields.email;

                await this.browser.allure.runStep(
                    'Отображается главный экран чекаута.',
                    async () => {
                        await this.confirmationPage.waitForVisible();
                        await this.deliveryInfo.waitForVisible();

                        await this.browser.allure.runStep(
                            'В блоке "Получатель" отображается информация о получателе.',
                            async () => {
                                await this.recipientBlock.getContactText()
                                    .should.eventually.to.be.equal(
                                        recipient,
                                        'На карточке получателя должны быть данные'
                                    );
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать кнопку "Изменить" в блоке "Получатель".',
                    async () => {
                        await this.recipientEditableCard.changeButtonClick();
                        await this.browser.allure.runStep(
                            'Открывается попап "Получатели" со списком получателей.',
                            async () => {
                                await this.editPopup.waitForVisibleRoot();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Карандаш".',
                    async () => {
                        await this.recipientList.clickOnEditButtonByRecipient(recipient);
                        await this.browser.allure.runStep(
                            'Открывается форма редактирования данных пользователя "Изменить получателя".',
                            async () => {
                                await this.recepientForm.waitForVisible();
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Электронная почта"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {
                                selector: emailField,
                                isActive: true,
                            }
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода вводим значение "тест"',
                            async () => {
                                await this.recipientFormFields.setEmailInputValue('test');
                                await this.recipientFormFields.getEmailInputValue()
                                    .should.eventually.to.be.equal(
                                        'test',
                                        'Должна отображаться надпись test'
                                    );
                            }
                        );
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: emailField,
                                selectorForFocus: RecipientFormFields.name,
                                errorText: 'Неверный формат почты',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Электронная почта"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: emailField}
                        );

                        await this.browser.allure.runStep(
                            'Нажать на кнопку "Х"',
                            async () => {
                                await this.recipientFormFields.clearField();
                                await this.recipientFormFields.getEmailInputValue().should.eventually.to.be.equal(
                                    '',
                                    'Поле ввода очищается'
                                );
                                await this.recipientFormFields.clearButtonIsVisible(emailField).should.eventually.to.be.equal(
                                    false,
                                    'В поле отсутствует отображение кнопки "Х"'
                                );
                                await this.recipientFormFields.isFocusedField(emailField)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Фокус остается в поле ввода'
                                    );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: emailField,
                                selectorForFocus: RecipientFormFields.name,
                                errorText: 'Напишите электронную почту',
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Электронная почта"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: emailField}
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода ввести значение "test.ru"',
                            async () => {
                                await this.recipientFormFields.setEmailInputValue('test.ru');
                                await this.recipientFormFields.getEmailInputValue().should.eventually.to.be.equal(
                                    'test.ru',
                                    'В поле ввода отображается "test.ru"'
                                );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: emailField,
                                selectorForFocus: RecipientFormFields.name,
                                errorText: 'Неверный формат почты',
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Электронная почта"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: emailField}
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода ввести валидное значение',
                            async () => {
                                await this.recipientFormFields.setEmailInputValue('test@test.tu');
                                await this.recipientFormFields.getEmailInputValue().should.eventually.to.be.equal(
                                    'test@test.tu',
                                    'В поле ввода отображается "test@test.tu"'
                                );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: emailField,
                                selectorForFocus: RecipientFormFields.name,
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Нажать на кнопку "Сохранить"',
                    async () => {
                        await this.recepientForm.saveButtonClick();

                        await this.browser.allure.runStep(
                            'Открывается попап "Получатели" со списком получателей.',
                            async () => {
                                await this.editPopup.waitForVisibleRoot();
                                await this.recipientList.getActiveItemText().should.eventually.include(
                                    ['Вася Пупкин\n'] +
                                    ['test@test.tu, 89876543210'],
                                    'Пресет редактируемого получателя отображается активным с внесенными изменениями'
                                );
                            }
                        );
                    });
            },
        }),
    },
});
