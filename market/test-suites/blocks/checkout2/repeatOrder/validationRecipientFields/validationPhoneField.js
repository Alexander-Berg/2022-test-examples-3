import {makeCase, makeSuite} from 'ginny';

import {
    focusFromRecipientFormField,
    removeFocusFromRecipientFormField,
} from '@self/root/src/spec/hermione/scenarios/checkout';

import {region} from '@self/root/src/spec/hermione/configs/geo';

import RecipientFormFields from '@self/root/src/components/RecipientForm/__pageObject';

import {CONTACTS} from '../../constants';

const VALID_PHONE = '+7 411 222-33-44';

export default makeSuite('Валидация поля "Телефон" с последующим сохранением внесенных изменений', {
    id: 'marketfront-5037',
    issue: 'MARKETFRONT-54580',
    feature: 'Валидация поля "Телефон" с последующим сохранением внесенных изменений',
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

                const phoneField = RecipientFormFields.phone;
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
                    'Установить фокус в поле "Телефон"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {
                                selector: phoneField,
                                isActive: true,
                            }
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода к имеющемуся значению ввести значение "9879879"',
                            async () => {
                                await this.recipientFormFields.addPhoneInputValue('9879879');
                                await this.recipientFormFields.getPhoneInputValue()
                                    .should.eventually.to.be.equal(
                                        `${CONTACTS.DEFAULT_CONTACT.phone}9879879`,
                                        `Должна отображаться надпись ${CONTACTS.DEFAULT_CONTACT.phone}тест`
                                    );
                            }
                        );
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: phoneField,
                                selectorForFocus: RecipientFormFields.name,
                                errorText: 'Пример: +7 495 414-30-00',
                            }
                        );
                    }
                );

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Телефон"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: phoneField}
                        );

                        await this.browser.allure.runStep(
                            'Нажать на кнопку "Х"',
                            async () => {
                                await this.recipientFormFields.clearField();
                                await this.recipientFormFields.getPhoneInputValue().should.eventually.to.be.equal(
                                    '',
                                    'Поле ввода очищается'
                                );
                                await this.recipientFormFields.clearButtonIsVisible().should.eventually.to.be.equal(
                                    false,
                                    'В поле отсутствует отображение кнопки "Х"'
                                );
                                await this.recipientFormFields.isFocusedField(phoneField)
                                    .should.eventually.to.be.equal(
                                        true,
                                        'Фокус остается в поле ввода'
                                    );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: phoneField,
                                selectorForFocus: RecipientFormFields.name,
                                errorText: 'Напишите номер телефона',
                            }
                        );
                    });

                await this.browser.allure.runStep(
                    'Установить фокус в поле "Телефон"',
                    async () => {
                        await this.browser.yaScenario(
                            this,
                            focusFromRecipientFormField,
                            {selector: phoneField}
                        );

                        await this.browser.allure.runStep(
                            'В поле ввода ввести валидное значение',
                            async () => {
                                await this.recipientFormFields.setPhoneInputValue(VALID_PHONE);
                                await this.recipientFormFields.getPhoneInputValue().should.eventually.to.be.equal(
                                    VALID_PHONE,
                                    `В поле ввода отображается "${VALID_PHONE}"`
                                );
                            });
                        await this.browser.yaScenario(
                            this,
                            removeFocusFromRecipientFormField,
                            {
                                focusedSelector: phoneField,
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
                                    [`pupochek@yandex.ru, ${VALID_PHONE}`],
                                    'Пресет редактируемого получателя отображается активным с внесенными изменениями'
                                );
                            }
                        );
                    });
            },
        }),
    },
});
