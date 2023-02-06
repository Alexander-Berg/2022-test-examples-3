const { URL } = require('url');
const { assert } = require('chai');
const { jane: admin } = hermione.ctx.testUsers;

function openInviteForm(browser) {
    return browser
        // нажимаем кнопку "+ Добавить" в футере корневого отдела
        .waitForVisible('.app__body .department-footer', 30000)
        .click('.app__body .department-footer .plus-button')
        // выбираем пункт "Пригласить сотрудников" в выпадающем меню
        .waitForVisible('.popup_visible .menu')
        .click('.popup_visible .menu-item:nth-child(2)')
        // открылся попап "Пригласить сотрудников в организацию"
        .waitForVisible('.modal_visible .invite-form')
        .waitForVisible('.add-invite-link .spin', 15000, true);
}

const ignoreInviteValue = {
    ignoreElements: ['.invite-link__input'],
};

describe('Приглашение пользователя', () => {
    describe('Положительные', () => {
        it('1. Доступность формы "Пригласить сотрудников в организацию" с дашборда', function() {
            // alias: pos-1-dashboard

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим на дашборд по ссылке "{{CONNECT_HOST}}/portal/home"
                .login({ ...admin, retpath: '/portal/home' })
                .disableAnimations('*')
                // нажимаем "+ Добавить сотрудника" на плитке "Админка"
                .click('.dashboard-card[data-slug="portal"] .dashboard-card__action')
                // выполнился переход в админку
                .waitForVisible('.app__body .department-layout', 30000)
                .getUrl()
                .then(url => {
                    assert.equal(new URL(url).pathname, '/portal/admin');
                })
                // открылся попап "Пригласить сотрудников в организацию"
                .waitForVisible('.modal_visible .invite-form')
                .waitForVisible('.add-invite-link .spin', 15000, true)
                // скриншот попапа "Пригласить сотрудников в организацию" [invite-form]
                .assertView('invite-form', '.modal_visible .modal__content', ignoreInviteValue);
        });

        it('2. Доступность формы "Пригласить сотрудников в организацию" в админке', function() {
            // alias: pos-2-admin

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openInviteForm(this.browser));
        });

        it('3. Приглашение по единому инвайту', function() {
            // alias: pos-3-invite-link

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .hideCaret()
                .then(() => openInviteForm(this.browser))
                .waitForVisible('.add-invite-link .lego-toggle')
                .isSelected('.add-invite-link .lego-toggle__checkbox')
                .then(selected => {
                    // если переключатель "Включить доступ по ссылке" серого цвета, нажать на переключатель
                    if (!selected) {
                        return this.browser.click('.add-invite-link .lego-toggle');
                    }
                })
                // переключатель желтого цвета
                .waitForExist('.add-invite-link .lego-toggle__checkbox:checked')
                // рядом с переключателем ссылка инвайта, пример ссылки "https://connect-portal-test.crowdtest.yandex.ru/invites/?code=a0e04cf8456c4e348fffadd472285c7b"
                .waitForExist('.invite-link__input')
                // скриншот попапа "Пригласить сотрудников в организацию" с включенным инвайтом [active-invite-form]
                .assertView('active-invite-form', '.modal_visible .modal__content', ignoreInviteValue)
                // перейти по ссылке инвайта
                .getValue('.invite-link__input')
                .then(val => this.browser.url(val))
                // открылась страница "Выберите аккаунт, чтобы присоединиться к организации <название организации>"
                .waitForVisible('.invite-page .invite_loading_no')
                // скриншот страницы "Выберите аккаунт, чтобы присоединиться к организации" [select-account]
                .assertView('select-account', '.invite-page')
                // выбираем из списка логин текущей учётки админа
                .click(`.session-select__session-name[title="${admin.nickname}"]`)
                // нажимаем кнопку "Подключить"
                .click('.invite__controls .lego-button_action')
                // появилось сообщение "Пользователь уже состоит в организации" [already-in-org-error]
                .waitForVisible('.invite .form__error')
                .assertView('already-in-org-error', 'form.invite')
                // выбираем пункт "+ Другой аккаунт"
                .click('.session-select__session_add')
                // попадаем на регистрацию в паспорте
                .waitForVisible('.passp-auth')
                .getUrl()
                .then(url => {
                    assert.isTrue(/^passport(\-\S+)?\.yandex\.ru$/.test(new URL(url).host));
                });
        });

        it('4. Приглашение по email', function() {
            // alias: pos-4-email

            const emails = [
                'qby7u1g3m0gt8lzhj2rxjs@yandex.ru',
                'plzy2cbrpf8q76zcq6k9jm@yandex.ru',
                'h5evswp00q4omxbtkyqjnn@yandex.ru',
            ];

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .hideCaret()
                .disableBorderRadius('.notification')
                .then(() => openInviteForm(this.browser))
                // в поле "Электронные адреса сотрудников" ввести несколько email вида login@yandex.ru
                .setValue('.invite-form__email-list .textinput__control', emails.join(', '))
                // нажимаем кнопку "Готово"
                .click('.button2_theme_action.invite-form__button')
                // попап закрылся
                .waitForVisible('.modal_visible .invite-form', 5000, true)
                // появилась нотификация "Приглашения отправлены."
                .waitForVisible('.notification__success', 30000);
        });
    });

    describe('Отрицательные', () => {
        it('1. Закрытие формы по нажатию на крестик', function() {
            // alias: neg-1-cross

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openInviteForm(this.browser))
                // нажимаем на крестик в верхнем правом углу формы
                .click('.modal_visible .modal__close')
                // попап закрылся
                .waitForVisible('.modal_visible .invite-form', 3000, true);
        });

        it('2. Ввод некорректного электронного адреса', function() {
            // alias: neg-2-invalid-email

            return this.browser
                // авторизоваться под учеткой администратора
                // переходим в админку по ссылке "{{CONNECT_HOST}}/portal/admin"
                .login({ ...admin, retpath: '/portal/admin' })
                .disableAnimations('*')
                .then(() => openInviteForm(this.browser))
                // в поле "Электронные адреса сотрудников" вводим некорректный email, пример login.ru
                .setValue('.invite-form__email-list .textinput__control', 'login.ru')
                // нажимаем кнопку "Готово"
                .click('.button2_theme_action.invite-form__button')
                // попап открыт, появилось сообщение "Укажите корректный адрес почты." [invalid-email-error]
                .assertView('invalid-email-error', '.modal_visible .modal__content', ignoreInviteValue);
        });

        it('3. Ссылка недействительного инвайта', function() {
            // alias: neg-3-invalid-link

            return this.browser
                // авторизоваться под учеткой администратора
                // перейти по ссылке инвайта "{{CONNECT_HOST}}/portal/invites/?code=InvalidInviteCode"
                .login({
                    ...admin,
                    retpath: '/portal/invites/?code=InvalidInviteCode',
                    waitElement: '.invite-page .invite_invalid',
                })
                // открылась страница с текстом ошибки "Ссылка недействительна." [invalid-code-error]
                .assertView('invalid-code-error', '.invite-page');
        });
    });
});
