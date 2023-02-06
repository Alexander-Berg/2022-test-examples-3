const { KimberlyMcGregor } = hermione.ctx.testUsers;

const Users = {
    JesperCooper: {
        id: '1130000002094050',
        name: 'Jesper Cooper',
    },
    HenrikEdwards: {
        id: '4028852408',
    },
};

function getAdminItemSelector(uid, nestedSelector) {
    // eslint-disable-next-line max-len
    return `.org-admin-section .section-list-item[data-id="${uid}"][data-type="user"]${nestedSelector ? ` ${nestedSelector}` : ''}`;
}

describe('Управление правами администратора', () => {
    describe('Положительные', () => {
        it('1. Админ добавляет и удаляет админские права у пользователя в разделе Администраторы', function() {
            /* alias: pos-1-admin */

            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // перейти по ссылке {{CONNECT_HOST}}/portal/admin/customization/admins
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/customization/admins' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.org-admin-section')

                // нажать на кнопку "+ Добавить" в подвале списка админов
                .click('.org-admins-footer .plus-button')

                // открылось модальное окно с формой добавления админа [add-admin-form]
                .waitForVisible('.modal_visible .add-org-admins-form')
                .assertView('add-admin-form', '.modal_visible .add-org-admins-form')

                // ввести в поле ввода имя пользователя-неадмина "Jesper Cooper"
                // выбрать "Jesper Cooper" во всплывающей подсказке
                .selectFromSuggest('.modal_visible .add-org-admins-form .suggest__control', Users.JesperCooper.name)

                // в поле ввода появилась плашка с выбранным пользователем [filled-add-admin-form]
                .waitForVisible('.modal_visible .add-org-admins-form .picker-item')
                .assertView('filled-add-admin-form', '.modal_visible .add-org-admins-form')

                // нажать на кнопку "Добавить" в модальном окне
                .setHash('add-admin')
                .click('.modal_visible .add-org-admins-form .form__buttons .button.button_view_action')

                // модальное окно закрылось
                .waitForVisible('.modal_visible .add-org-admins-form', 15000, true)

                // появилось сообщение "Состав администраторов организации обновлён" [add-success]
                .waitForVisible('.notification__success', 15000)
                .assertView('add-success', '.notification__success .notification-content')

                // нажать на крестик во всплывающем сообщении
                .click('.notification__success .ui-flat-button')

                // в списке админов появился "Jesper Cooper"
                .waitForVisible('.notification__success .notification-content', 1000, true)
                .waitForVisible(getAdminItemSelector(Users.JesperCooper.id), 3000)

                // нажать на карандаш в правом верхнем углу списка админов
                .click('.org-admin-section .edit-section-controls .ui-icon__pen')

                // нажать на чекбокс рядом с "Jesper Cooper"
                .waitForVisible(getAdminItemSelector(Users.JesperCooper.id, '.checkbox'), 3000)
                .click(getAdminItemSelector(Users.JesperCooper.id, '.checkbox'))

                // нажать на кнопку с мусорным баком в подвале списка админов
                .click('.org-admins-footer .ui-icon__trash')

                // открылось модальное окно с формой удаления админа [remove-admin-form]
                .waitForVisible('.modal_visible .remove-org-admins-form')
                .assertView('remove-admin-form', '.modal_visible .remove-org-admins-form')

                // нажать на кнопку "Удалить" в модальном окне
                .setHash('remove-admin')
                .click('.modal_visible .remove-org-admins-form .form__buttons .button.button_view_action')

                // модальное окно закрылось
                .waitForVisible('.modal_visible .remove-org-admins-form', 15000, true)

                // появилось сообщение "Состав администраторов организации обновлён" [remove-success]
                .waitForVisible('.notification__success', 15000)
                .assertView('remove-success', '.notification__success .notification-content')

                // нажать на крестик во всплывающем сообщении
                .click('.notification__success .ui-flat-button')

                // в списке админов больше нет "Jesper Cooper"
                .waitForVisible('.notification__success .notification-content', 1000, true)
                .waitForVisible(getAdminItemSelector(Users.JesperCooper.id), 3000, true);
        });

        it('2. Админ добавляет и удаляет админские права у пользователя в карточке пользователя', function() {
            /* alias: pos-2-user-card */
            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // открыть карточку сотрудника "Diaz Florence" по ссылке /portal/admin/users/1130000002100008
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/users/1130000002100008' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.user-section')

                // нажать на "..." в правом верхнем углу карточки пользователя
                .click('.user-section .user-controls__edit button')

                // открылось меню
                .waitForVisible('.popup_visible', 3000)

                // в меню есть пункт "Сделать администратором"
                .waitForExactText('.popup_visible .menu-item', 'Сделать администратором', 1000)

                // нажать на пункт меню "Сделать администратором"
                .setHash('add-admin')
                .click('.popup_visible .menu-item[data-bem*="grant_access"]')

                // появилось сообщение "Готово" [add-success]
                .waitForVisible('.notification__success', 15000)
                .assertView('add-success', '.notification__success .notification-content')

                // нажать на крестик во всплывающем сообщении
                .click('.notification__success .ui-flat-button')

                // нажать на "..." в правом верхнем углу карточки пользователя
                .click('.user-section .user-controls__edit button')

                // открылось меню
                .waitForVisible('.popup_visible', 3000)

                // в меню есть пункт "Отозвать права администратора"
                .waitForExactText('.popup_visible .menu-item', 'Отозвать права администратора', 1000)

                // нажать на пункт меню "Отозвать права администратора"
                .setHash('remove-admin')
                .click('.popup_visible .menu-item[data-bem*="revoke_access"]')

                // появилось сообщение "Готово" [remove-success]
                .waitForVisible('.notification__success', 15000)
                .assertView('remove-success', '.notification__success .notification-content');
        });
    });

    describe('Отрицательные', () => {
        // eslint-disable-next-line max-len
        it('1. Админ нажимает на крестик при добавлении админских прав пользователю в разделе Администраторы', function() {
            /* alias: neg-1-cross */
            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // перейти по ссылке {{CONNECT_HOST}}/portal/admin/customization/admins
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/customization/admins' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.org-admin-section')

                // нажать на кнопку "+ Добавить" в подвале списка админов
                .click('.org-admins-footer .plus-button')

                // открылось модальное окно с формой добавления админа
                .waitForVisible('.modal_visible .add-org-admins-form')

                // нажать на крестик в правом верхнем углу модального окна
                .click('.modal_visible .modal__close')

                // модальное окно закрылось
                .waitForVisible('.modal_visible .add-org-admins-form', 3000, true);
        });

        // eslint-disable-next-line max-len
        it('2. Админ нажимает на кнопку "Отменить" при добавлении админских прав пользователю в разделе Администраторы', function() {
            /* alias: neg-2-cancel */
            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // перейти по ссылке {{CONNECT_HOST}}/portal/admin/customization/admins
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/customization/admins' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.org-admin-section')

                // нажать на кнопку "+ Добавить" в подвале списка админов
                .click('.org-admins-footer .plus-button')

                // открылось модальное окно с формой добавления админа
                .waitForVisible('.modal_visible .add-org-admins-form')

                // нажать на кнопку "Отменить" в модальном окне
                .click('.modal_visible .button:not(.button_view_action)')

                // модальное окно закрылось
                .waitForVisible('.modal_visible .add-org-admins-form', 3000, true);
        });

        it('3. Админ не может удалить владельца организации в разделе Администраторы', function() {
            /* alias: neg-3-no-permission */
            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // перейти по ссылке {{CONNECT_HOST}}/portal/admin/customization/admins
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/customization/admins' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.org-admin-section')

                // нажать на карандаш в правом верхнем углу списка админов
                .click('.org-admin-section .edit-section-controls .ui-icon__pen')

                // нажать на чекбокс рядом с "Edwards Henrik"
                .waitForVisible(getAdminItemSelector(Users.HenrikEdwards.id, '.checkbox'), 3000)
                .click(getAdminItemSelector(Users.HenrikEdwards.id, '.checkbox'))

                // нажать на кнопку с мусорным баком в подвале списка админов
                .click('.org-admins-footer .ui-icon__trash')

                // открылось модальное окно с формой удаления админа [remove-admin-form]
                .waitForVisible('.modal_visible .remove-org-admins-form')
                .assertView('remove-admin-form', '.modal_visible .remove-org-admins-form')

                // нажать на кнопку "Удалить" в модальном окне
                .setHash('remove-admin')
                .click('.modal_visible .remove-org-admins-form .form__buttons .button.button_view_action')

                // модальное окно не закрылось
                .waitForVisible('.modal_visible .remove-org-admins-form', 3000)

                // в модальном окне появилось красное сообщение
                // "Произошла ошибка — не удалось обновить состав администраторов команды" [remove-admin-form-error]
                .waitForVisible('.modal_visible .remove-org-admins-form .loader', 15000, true)
                .waitForVisible('.modal_visible .remove-org-admins-form .form__error', 1000)
                .assertView('remove-admin-form-error', '.modal_visible .remove-org-admins-form');
        });

        it('4. Админ не может удалить владельца организации в карточке пользователя', function() {
            /* alias: neg-4-owner */
            return this.browser
                // авторизоваться под администратором организации "kimberly.mcgregor@cap.auto.connect-test.tk"
                // открыть карточку владельца организации "Henrik Edwards" по ссылке /portal/admin/users/4028852408
                .login({ ...KimberlyMcGregor, retpath: '/portal/admin/users/4028852408' })
                .disableAnimations('*')
                .hideCaret()
                .waitForVisible('.user-section')

                // нажать на "..." в правом верхнем углу карточки пользователя
                .click('.user-section .user-controls__edit button')

                // открылось меню
                .waitForVisible('.popup_visible', 3000)

                // нажать на пункт меню "Отозвать права администратора"
                .setHash('remove-admin')
                .click('.popup_visible .menu-item[data-bem*="revoke_access"]')

                // появилось сообщение "Произошла ошибка" [remove-error]
                .waitForVisible('.notification__error', 15000)
                .assertView('remove-error', '.notification__error .notification-content');
        });
    });
});
