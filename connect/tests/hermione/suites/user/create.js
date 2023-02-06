/* eslint-disable max-len */
const { URL } = require('url');
const { assert } = require('chai');
const { dismissUser } = require('../../helpers/directoryApi');
const { alex: admin } = hermione.ctx.testUsers;
const { CONNECT_TOKENS__USER_PASS: PASSWORD } = process.env;

function openUserForm(browser) {
    return browser
        .disableBorderRadius('.modal__content')
        // нажимаем кнопку "+ Добавить" в футере корневого отдела
        .waitForVisible('.app__body .department-footer', 15000)
        .click('.app__body .department-footer .plus-button')
        // выбираем первый пункт "Добавить сотрудника" в выпадающем меню
        .waitForVisible('.popup_visible .menu')
        .click('.popup_visible .menu-item:first-child')
        // открылся попап "Новый сотрудник"
        .waitForVisible('.modal_visible .create-user-form');
}

function select(browser, { name, selector }, value) {
    if (name) {
        selector = `[data-bem="{\\"select\\":{\\"name\\":\\"${name}\\"}}"]`;
    }

    return browser
        .click(`${selector} .button`)
        .waitForVisible('.popup_visible')
        .click(`.popup_visible [data-bem*="\\"val\\":\\"${value}\\""]`);
}

function contains(browser, selector, value) {
    return browser
        .getText(selector)
        .then(text => {
            assert.equal(text, value);
        });
}

describe('Добавление пользователя', () => {
    afterEach(function() {
        if (hermione.ctx.cacheMode !== 'read') {
            return this.browser.getMeta('uid').then(uid => uid ? dismissUser(Number(uid), admin) : undefined);
        }
    });

    describe('Положительные', () => {
        it('1. Доступность формы "Добавление сотрудника" с дашборда', function() {
            /* alias: pos-1-dashboard */

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим на дашборд по ссылке "{{CONNECT_HOST}}/portal/home"
                .login({ ...admin, retpath: '/portal/home' })
                .disableAnimations('*')
                // нажимаем на экшен "+ Добавить сотрудника" на плитке "Админка"
                .click('.dashboard-card[data-slug="portal"] .dashboard-card__action')
                // выполнился переход в админку
                .waitForVisible('.app__body .department-layout', 15000)
                .getUrl()
                .then(url => {
                    assert.equal(new URL(url).pathname, '/portal/admin');
                })
                // открылся попап "Новый сотрудник"
                .waitForVisible('.modal_visible .create-user-form')
                // скриншот попапа "Новый сотрудник" [new-user-form]
                .assertView('new-user-form', '.modal_visible .modal__content');
        });

        it('2. Доступность формы "Добавление сотрудника" в админке', function() {
            /* alias: pos-2-admin */

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openUserForm(this.browser));
        });

        it('3. Пользователь создается, заполнены обязательные поля', function() {
            /* alias: pos-3-req-fields */

            const fields = {
                name: {
                    first: 'Ivan',
                    last: 'Smirnov',
                },
                nickname: 'ivan-smirnov-007',
            };
            const { name } = fields;

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .hideCaret()
                .disableBorderRadius('.notification')
                .then(() => openUserForm(this.browser))
                // заполняем обязательные поля Фамилия, Имя, Логин, Пароль, Еще раз
                .setValue('input[name="name[last][ru]"]', name.last)
                .setValue('input[name="name[first][ru]"]', name.first)
                .setValue('input[name^="nickname__"]', fields.nickname)
                .setValue('input[name^="password__"]', PASSWORD)
                .setValue('input[name^="password_confirmation__"]', PASSWORD)
                // форма создания сотрудника с заполненными обязательными полями [required-filled-form]
                .assertView('required-filled-form', '.modal_visible .modal__content')
                // нажать на кнопку "Добавить"
                .click('.modal_visible .button2_type_submit')
                // появилась нотификация "Пользователь создан", попап закрылся
                .waitForVisible('.notification__success', 30000)
                .waitForVisible('.modal_visible .create-user-form', 1000, true)
                // отображается сообщение "Пользователь создан" [notification]
                .assertView('notification', '.notification__success .notification-content')
                // нажать на крестик в нотификации
                .click('.notification__success .ui-flat-button')
                // открыта карточка сотрудника "{{CONNECT_HOST}}/portal/admin/users/{{uid}}"
                .waitForVisible('.user-section')
                .getUrl()
                .then(url => {
                    let { pathname } = new URL(url);
                    let uid = pathname.split('/').pop();

                    assert.equal(pathname, `/portal/admin/users/${uid}`);

                    return this.browser.setMeta('uid', uid);
                })
                // новый сотрудник отображается в составе отдела
                .waitForVisible(`.department-section .department-body .unit[title="${name.last} ${name.first}"]`)
                // проверяем что имя и фамилия в карточке пользователя совпадают с теми что заполнили ранее в попапе "Новый сотрудник"
                .then(() => contains(this.browser, '.user-section .user-identity__name', `${name.first} ${name.last}`));
        });

        it('4. Пользователь создается, заполнены все поля', function() {
            /* alias: pos-4-all-fields */

            const b = this.browser;
            const fields = {
                name: {
                    first: 'Мария',
                    middle: 'Ивановна',
                    last: 'Петрова',
                },
                position: 'Художник',
                language: 'ru',
                timezone: 'Europe/Istanbul',
                date: 21,
                month: 12,
                year: 1990,
                gender: 'female',
                nickname: 'm-petrova-007',
                email: 'm.petrova@email.not',
            };
            const { name } = fields;

            return b
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .hideCaret()
                .disableBorderRadius('.notification')
                .then(() => openUserForm(b))
                // в поле "Фамилия*" ввести произвольное корректное значение
                .setValue('input[name="name[last][ru]"]', name.last)
                // в поле "Имя*" ввести произвольное корректное значение
                .setValue('input[name="name[first][ru]"]', name.first)
                // в поле "Отчество" ввести произвольное корректное значение
                .setValue('input[name="name[middle][ru]"]', name.middle)
                // в поле "Должность" ввести произвольное корректное значение
                .setValue('input[name="position[ru]"]', fields.position)
                // в поле "Язык" выбрать из выпадающего списка любой язык
                .then(() => select(b, { name: 'language' }, fields.language))
                // в поле "Часовой пояс" выбрать из выпадающего списка любой часовой пояс
                .then(() => select(b, { name: 'timezone' }, fields.timezone))
                // в поле "Дата рождения" ввести корректную дату рождения, месяц выбрать из выпадающего списка
                .setValue('.date-picker__date input', fields.date)
                .then(() => select(b, { selector: '.date-picker__month .select' }, fields.month))
                .setValue('.date-picker__year input', fields.year)
                // в поле "Пол" выбрать через переключатель пол
                .click(`.ui-button-group__caption=${fields.gender === 'female' ? 'Женский' : 'Мужской'}`)
                // в поле "Логин*" ввести произвольное корректное значение
                .setValue('input[name^="nickname__"]', fields.nickname)
                // в поле "Пароль*" ввести произвольное корректное значение
                .setValue('input[name^="password__"]', PASSWORD)
                // в поле "Еще раз*" ввести произвольное корректное значение
                .setValue('input[name^="password_confirmation__"]', PASSWORD)
                // в поле "Почта" ввести произвольное корректное значение
                .click('.add-contact')
                .then(() => select(b, { selector: '.user-form__contacts .form__item:nth-child(1) .select' }, 'email'))
                .setValue('input[name="contacts[0][value]"]', fields.email)
                // нажать кнопку "+ Добавить"
                .click('.add-contact')
                // в выпадающем списке выбрать "Skype"
                .then(() => select(b, { selector: '.user-form__contacts .form__item:nth-child(2) .select' }, 'skype'))
                // в поле "Skype" ввести произвольное корректное значение
                .setValue('input[name="contacts[1][value]"]', fields.nickname)
                // в поле "Отдел" нажать на крестик возле "Все сотрудники"
                .click('.picker-item_type_department .picker-item__close')
                // в поле "Отдел" ввести отдел "Тест"
                // выбрать отдел "Тест" из саджеста
                .waitForVisible('.create-user-form .form__item:nth-child(7) .suggest__control')
                .selectFromSuggest('.create-user-form .form__item:nth-child(7) .suggest__control', 'Best')
                // форма создания сотрудника с заполненными полями [filled-form]
                .assertView('filled-form', '.modal_visible .modal__content')
                // нажать на кнопку "Добавить"
                .click('.modal_visible .button2_type_submit')
                // появилась нотификация "Пользователь создан", попап закрылся
                .waitForVisible('.notification__success', 30000)
                .waitForVisible('.modal_visible .create-user-form', 1000, true)
                // нажать на крестик в нотификации
                .click('.notification__success .ui-flat-button')
                // открыта карточка сотрудника "{{CONNECT_HOST}}/portal/admin/users/{{uid}}"
                .waitForVisible('.user-section')
                .getUrl()
                .then(url => {
                    let { pathname } = new URL(url);
                    let uid = pathname.split('/').pop();

                    assert.equal(pathname, `/portal/admin/users/${uid}`);

                    return b.setMeta('uid', uid);
                })
                // новый сотрудник отображается в составе отдела
                .waitForVisible(`.department-section .department-body .unit[title^="${name.last} ${name.first} ${name.middle}"]`)
                // проверяем что данные в карточке пользователя совпадают с теми что заполнили ранее в попапе "Новый сотрудник"
                .then(() => contains(b, '.user-section .user-identity__name', `${name.first} ${name.middle} ${name.last}`))
                // данные заполнены как на скриншоте [user-card]
                .assertView('user-card', '.user-section .section-body');
        });
    });

    describe('Отрицательные', () => {
        it('1. Закрытие формы по нажатию на крестик', function() {
            /* alias: neg-1-cross */

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openUserForm(this.browser))
                // нажимаем на крестик в верхнем правом углу формы
                .click('.modal_visible .modal__close')
                // попап закрылся
                .waitForVisible('.modal_visible .create-user-form', 3000, true);
        });

        it('2. Закрытие формы по нажатию на кнопку "Отменить"', function() {
            /* alias: neg-2-cancel */

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openUserForm(this.browser))
                // нажимаем на крестик в верхнем правом углу формы
                .click('.modal_visible .form__buttons .button2_theme_normal')
                // попап закрылся
                .waitForVisible('.modal_visible .create-user-form', 3000, true);
        });

        it('3. Проверка валидации при незаполненных обязательных полях', function() {
            /* alias: neg-3-invalid */

            const b = this.browser;

            return b
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openUserForm(b))
                // нажать на кнопку "Добавить"
                .click('.modal_visible .button2_type_submit')
                // под полем "Фамилия*" отображается сообщение "Нужно указать фамилию сотрудника."
                .then(() => contains(b, '.create-user-form .form__subsection:nth-child(2) .form__item:nth-child(1) .form__error', 'Нужно указать фамилию сотрудника.'))
                // под полем "Имя*" отображается сообщение "Вы забыли указать имя."
                .then(() => contains(b, '.create-user-form .form__subsection:nth-child(2) .form__item:nth-child(2) .form__error', 'Вы забыли указать имя.'))
                // под полем "Логин*" отображается сообщение "Сотруднику обязательно нужен логин."
                .then(() => contains(b, '.create-user-form .form__item.user-nickname .form__error', 'Сотруднику обязательно нужен логин.'))
                // под полем "Пароль*" отображается сообщение "Вы забыли про пароль."
                .then(() => contains(b, '.create-user-form .form__subsection:nth-child(5) .form__item:nth-child(1) .form__error', 'Вы забыли про пароль.'))
                // под полем "Еще раз*" отображается сообщение "Повторите пароль."
                .then(() => contains(b, '.create-user-form .form__subsection:nth-child(5) .form__item:nth-child(2) .form__error', 'Повторите пароль.'))
                // отображаются сообщения около незаполненных обязательных полей [errors]
                .assertView('errors', '.modal_visible .modal__content');
        });
    });
});
