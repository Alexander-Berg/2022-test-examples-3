function preparePage(browser) {
    return browser
        // открываем страницу
        .openIntranetPage({ pathname: '/services/robotinternal003service' })
        .waitForVisible('.service-team__loader', 10000, true)

        // жмём "Добавить сотрудника или департамент"
        .waitForVisible('.service-team__button_role_add-users')
        .click('.service-team__button_role_add-users')

        // вызываем саджест сотрудников
        .waitForVisible('.service-team-editor', 10000)
        .setValue('.service-team-editor__input_role_add .input__control', ' ')
        .waitForVisible('.input__popup.popup_visibility_visible', 20000)

        // кликаем на первого
        .click('.b-autocomplete-item:first-child')
        .waitForVisible('.service-team-editor-member');
}

describe('Очистка формы добавления участников команды', () => {
    describe('Положительные', () => {
        it('1. Удаление из формы без выбора роли', function() {
            return preparePage(this.browser)
                // удаляем сотрудника
                .click('.service-team-editor-member') // Потому что moveToObject — deprecated
                .waitForVisible('.service-team-editor-member__remove')
                .click('.service-team-editor-member__remove')

                // пользователь успешно удалился из формы
                .waitForVisible('.service-team-editor-member', null, true);
        });

        it('2. Удаление после создания новой роли', function() {
            return preparePage(this.browser)
                // вызываем саджест роли
                // очищаем подсказки, чтобы последний элемент был виден
                .setValue('.service-team-editor-member__role .input__control', 'we_dont_have_such_role')
                .waitForVisible('.input__popup.popup_visibility_visible', 20000)

                // нажимаем на "Добавить новую роль"
                .waitForVisible('.input_role_role-suggest.input_js_inited')
                .waitForVisible('.b-autocomplete-item_type_new-role.b-autocomplete-item_js_inited')
                .click('.b-autocomplete-item__new-role-button')
                .waitForVisible('.abc-new-role', 5000)

                // удаляем сотрудника
                .click('.service-team-editor-member') // Потому что moveToObject — deprecated
                .waitForVisible('.service-team-editor-member__remove')
                .click('.service-team-editor-member__remove')

                // пользователь успешно удалился из формы
                .waitForVisible('.service-team-editor-member', null, true);
        });

        it('3. Удаление после закрытия формы создания роли', function() {
            return preparePage(this.browser)
                // вызываем саджест роли
                // очищаем подсказки, чтобы последний элемент был виден
                .setValue('.service-team-editor-member__role .input__control', 'we_dont_have_such_role')
                .waitForVisible('.input__popup.popup_visibility_visible', 20000)

                // нажимаем на "Добавить новую роль"
                .waitForVisible('.input_role_role-suggest.input_js_inited')
                .waitForVisible('.b-autocomplete-item_type_new-role.b-autocomplete-item_js_inited')
                .click('.b-autocomplete-item__new-role-button')
                .waitForVisible('.abc-new-role', 5000)

                // ждём, когда форма прогрузится
                .waitForVisible('.abc-new-role__spin', null, true)

                // закрываем форму создания роли
                .waitForVisible('.abc-new-role__discard')
                .click('.abc-new-role__discard')
                .waitForVisible('.abc-new-role', null, true)

                // удаляем сотрудника
                .click('.service-team-editor-member') // Потому что moveToObject — deprecated
                .waitForVisible('.service-team-editor-member__remove')
                .click('.service-team-editor-member__remove')

                // пользователь успешно удалился из формы
                .waitForVisible('.service-team-editor-member', null, true);
        });
    });
});
