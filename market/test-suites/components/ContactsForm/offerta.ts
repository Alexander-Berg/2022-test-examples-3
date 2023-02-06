'use strict';

import {mergeSuites, makeSuite, makeCase} from 'ginny';

/**
 * Тест на недоступность чекбокса принятия оферты менеджерам
 * @param {PageObject.ContactsForm} contactsForm - форма с контактными данными
 * @param {PageObject.ContactsForm} form - форма
 */
export default makeSuite('Форма "Контактные данные". Оферта. ', {
    environment: 'kadavr',
    feature: 'Настройки',
    params: {
        user: 'Пользователь',
    },
    story: mergeSuites(
        {
            async beforeEach() {
                await this.browser.allure.runStep('Ожидаем появления формы контактных данных', () =>
                    this.contactsForm.waitForExist(),
                );

                await this.form
                    .getReadonlyFieldValue('offer')
                    .should.eventually.be.equal('не принята', 'У поля "Оферта" корректное изначальное значение');

                await this.form.clickEditButton();

                await this.browser.allure.runStep('Форма перешла в режим редактирования', async () => {
                    /*
                     * Используем yaSafeAction, чтобы не делать дополнительные проверки с isExisting.
                     * Если элемент не будет найден, тест упадет,
                     * но ошибка будет корректно обработана как false.
                     * Если использовать waitUntil, то тест в отчете будет broken,а не failed
                     */

                    await this.browser
                        .yaSafeAction(this.form.cancelButton.isEnabled(), false)
                        .should.eventually.equal(true, 'Кнопка [Отмена] отображается и активна');

                    await this.browser
                        .yaSafeAction(this.form.submitButton.isEnabled(), false)
                        .should.eventually.equal(true, 'Кнопка [Сохранить] отображается и активна');
                });
            },
        },
        {
            'Чекбокс принятия условий оферты': {
                задизейблен: makeCase({
                    issue: 'VNDFRONT-2132',
                    id: 'vendor_auto-53',
                    test() {
                        return this.contactsForm.disabledCheckbox
                            .vndIsExisting()
                            .should.eventually.be.equal(true, 'Чекбокс принятия условий оферты задизейблен');
                    },
                }),
            },
        },
    ),
});
