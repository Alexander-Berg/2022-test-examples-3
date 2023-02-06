const { assert } = require('chai');
const { changeAdminPasswordOrgAdmin: user } = hermione.ctx.testUsers;

function openUserCard(uid, browser) {
    return browser

        // открыть карточку сотрудника ${uid}
        .url(`/portal/admin/customization/admins/${uid}`)
        .waitForVisible('.user-section')
        .disableAnimations('*');
}

function openAndClose(selector, browser) {
    return browser

        // нажать на "..."
        .click('.user-controls__edit .ui-flat-button')
        .waitForVisible('.popup_visible .menu')

        // в меню есть пункт "Изменить пароль"
        .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
        .then(assert.isTrue)

        // выбрать пункт "Изменить пароль"
        .click('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
        .waitForVisible('.modal_visible .change-user-password-form')

        // клик по элементу
        .click('.modal_visible .modal__close')

        // модальное окно закрылось
        .isExisting('.modal_visible .change-user-password-form')
        .then(assert.isFalse);
}

function sendEditPasswordForm(password, passwordConfirmation, browser) {
    const formData = {
        password,
        password_confirmation: passwordConfirmation,
    };

    return browser

        // нажать на "..."
        .click('.user-controls__edit .ui-flat-button')
        .waitForVisible('.popup_visible .menu')

        // в меню есть пункт "Изменить пароль"
        .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
        .then(assert.isTrue)

        // выбрать пункт "Изменить пароль"
        .click('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
        // ждем, пока появится поле с паролем, т.к. оно доступно не сразу
        // подробнее: https://github.yandex-team.ru/yandex-directory/connect/blob/master/portal/client/ui/Form/index.js#L131
        .waitForVisible('.modal_visible .change-user-password-form input[name^="password__"]')

        // ввести пароль и подтверждение пароля
        .fillForm('change-user-password-form', formData)

        // нажать кнопку "Сохранить"
        .click('.change-user-password-form .button_type_submit');
}

describe('Смена пароля администратора', () => {
    beforeEach(function() {
        return this.browser
            .login({ ...user, retpath: '/portal/admin/customization/admins' })
            .hideCaret()
            .waitForVisible('.org-admin-section');
    });

    describe('Положительные', () => {
        it('1. Внешний вид меню и формы изменения пароля', function() {
            /* alias: pos-1-view */
            return openUserCard(1130000002089006, this.browser)

                // нажать на "..."
                .click('.user-controls__edit .ui-flat-button')
                .waitForVisible('.popup_visible .menu')

                // в меню есть пункт "Изменить пароль"
                .assertView('menu', '.popup_visible .menu')
                .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
                .then(assert.isTrue)

                // выбрать пункт "Изменить пароль"
                .click('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
                .waitForVisible('.modal_visible .change-user-password-form')

                // форма изменения пароля [password-form]
                .assertView('password-form', '.change-user-password-form');
        });

        it('2. Смена пароля доменной учётной записи', function() {
            /* alias: pos-2-domain-user */
            const newPassword = Date.now().toString();

            return openUserCard(1130000002089006, this.browser)
                .then(() => sendEditPasswordForm(newPassword, newPassword, this.browser))

                // появилось сообщение "Логин и пароль пользователя обновлены"
                .waitForVisible('.notification__success')
                .assertView('notification', '.notification__success')

                // модальное окно закрылось
                .isExisting('.modal_visible .change-user-password-form')
                .then(assert.isFalse);
        });
    });

    describe('Отрицательные', () => {
        it('01. Нет смены пароля портальной учётной записи', function() {
            /* alias: neg-01-portal-user */
            return openUserCard(4028864150, this.browser)

                // нажать на "..."
                .click('.user-controls__edit .ui-flat-button')
                .waitForVisible('.popup_visible .menu')

                // в меню нет пункта "Заблокировать пользователя"
                .assertView('menu', '.popup_visible .menu')
                .isExisting('.popup_visible .menu .menu-item[data-bem=\'{"menu-item":{"val":"change_password"}}\']')
                .then(assert.isFalse);
        });
        it('02. Не введён пароль', function() {
            /* alias: neg-02-empty */
            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm('', '', this.browser))

                // сообщение о пустом пароле [empty-password-error]
                .waitForVisible('.form__error')
                .assertView('empty-password-error', '.change-user-password-form');
        });
        it('03. Не введено подтверждение пароля', function() {
            /* alias: neg-03-no-confirm */
            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm('123', '', this.browser))

                // сообщение о пустом подтверждении [empty-confirmation-error]
                .waitForVisible('.form__error')
                .assertView('empty-confirmation-error', '.change-user-password-form');
        });
        it('04. Несоответствие пароля и подверждения пароля', function() {
            /* alias: neg-04-no-confirm */
            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm('123', '1234', this.browser))

                // сообщение о том, что пароли не совпадают [password-confirmation-error]
                .waitForVisible('.form__error')
                .assertView('password-confirmation-error', '.change-user-password-form');
        });
        it('05. Ввод короткого пароля', function() {
            /* alias: neg-05-short */
            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm('123', '123', this.browser))

                // сообщение о коротком пароле [short-password-error]
                .waitForVisible('.form__error')
                .assertView('short-password-error', '.change-user-password-form');
        });
        it('06. Ввод длинного пароля', function() {
            // eslint-disable-next-line max-len
            const longPassword = '1111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111';

            /* alias: neg-06-long */

            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm(longPassword, longPassword, this.browser))

                // сообщение о длинном пароле [long-password-error]
                .waitForVisible('.form__error')
                .assertView('long-password-error', '.change-user-password-form');
        });
        it('07. Ввод некорректного пароля', function() {
            /* alias: neg-07-invalid */
            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm('∞∞∞∞∞∞', '∞∞∞∞∞∞', this.browser))

                // сообщение о некорректном пароле [invalid-password-error]
                .waitForVisible('.form__error')
                .assertView('invalid-password-error', '.change-user-password-form');
        });
        it('08. Ввод старого пароля', function() {
            /* alias: neg-08-old */
            const newPassword = Date.now().toString();

            return openUserCard(1130000002089008, this.browser)
                .then(() => sendEditPasswordForm(newPassword, newPassword, this.browser))

                // появилось сообщение "Логин и пароль пользователя обновлены"
                .waitForVisible('.notification__success')
                .assertView('notification', '.notification__success')

                // модальное окно закрылось
                .isExisting('.modal_visible .change-user-password-form')
                .then(assert.isFalse)

                .setHash('used-password')
                .then(() => sendEditPasswordForm(newPassword, newPassword, this.browser))

                // сообщение о том, что новый пароль не может совпадать со старым [used-password-error]
                .waitForVisible('.form__error')
                .assertView('used-password-error', '.change-user-password-form');
        });
        it('09. Закрытие модального окна по клику на крестик', function() {
            /* alias: neg-09-cross */
            return openUserCard(1130000002089008, this.browser)
                .then(() => openAndClose('.modal_visible .modal__close', this.browser));
        });
        it('10. Закрытие модального окна по клику на кнопку "Отменить"', function() {
            /* alias: neg-10-cancel */
            return openUserCard(1130000002089008, this.browser)
                .then(() => openAndClose('.modal_visible .button__control:not(.button_type_submit)', this.browser));
        });
        it('11. Закрытие модального окна по клику за его пределами', function() {
            /* alias: neg-11-outer */
            return openUserCard(1130000002089008, this.browser)
                .then(() => openAndClose('.modal_visible .modal__close', this.browser));
        });
    });
});
