import {makeCase, makeSuite} from 'ginny';

import {
    focusFromRecipientFormField,
    removeFocusFromRecipientFormField,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';

import {CONTACTS} from '../../constants';

export default makeSuite('Валидация поля "Имя и фамилия" с последующим сохранением внесенных изменений', {
    id: 'marketfront-5013',
    issue: 'MARKETFRONT-54580',
    feature: 'Валидация поля "Имя и фамилия" с последующим сохранением внесенных изменений',
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

                const nameField = RecipientFormFields.name;
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
                    'Установить фокус в поле "Имя и фамилия"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {
                                selector: nameField,
                                isActive: true,
                            }
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода к имеющемуся значению ввести значение "test"',
                            async () => {
                                await this.recipientFormFields.addNameInputValue('test');
                                await this.recipientFormFields.getNameInputValue()
                                    .should.eventually.to.be.equal(
                                        CONTACTS.DEFAULT_CONTACT.recipient + 'test',
                                        `Должна отображаться надпись ${CONTACTS.DEFAULT_CONTACT.recipient}test`
                                    );
                            }
                        );
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: nameField,
                                selectorForFocus: RecipientFormFields.email,
                                errorText: 'Фамилия и имя должны быть написаны на кириллице',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Имя и фамилия"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: nameField}
                        );

                        await this.browser.allure.runStep(
                            'Нажать на кнопку "Х"',
                            async () => {
                                await this.recipientFormFields.clearField();
                                await this.recipientFormFields.getNameInputValue().should.eventually.to.be.equal(
                                    '',
                                    'Поле ввода очищается'
                                );
                                await this.recipientFormFields.clearButtonIsVisible(nameField).should.eventually.to.be.equal(
                                    false,
                                    'В поле отсутствует отображение кнопки "Х"'
                                );
                                await this.recipientFormFields.isFocusedField(nameField)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Фокус остается в поле ввода'
                                    );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: nameField,
                                selectorForFocus: RecipientFormFields.email,
                                errorText: 'Напишите имя и фамилию как в паспорте',
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Имя и фамилия"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: nameField}
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода ввести значение "Имя"',
                            async () => {
                                await this.recipientFormFields.setNameInputValue('Имя');
                                await this.recipientFormFields.getNameInputValue().should.eventually.to.be.equal(
                                    'Имя',
                                    'В поле ввода отображается только Имя получателя'
                                );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: nameField,
                                selectorForFocus: RecipientFormFields.email,
                                errorText: 'Напишите имя и фамилию как в паспорте',
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Имя и фамилия"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: nameField}
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода ввести валидное значение',
                            async () => {
                                await this.recipientFormFields.setNameInputValue('Имя Фамилия');
                                await this.recipientFormFields.getNameInputValue().should.eventually.to.be.equal(
                                    'Имя Фамилия',
                                    'В поле ввода отображается "Имя Фамилия"'
                                );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: nameField,
                                selectorForFocus: RecipientFormFields.email,
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
                                    ['Имя Фамилия\n'] +
                                    ['pupochek@yandex.ru, 89876543210'],
                                    'Пресет редактируемого получателя отображается активным с внесенными изменениями'
                                );
                            }
                        );
                    });
            },
        }),
    },
});
