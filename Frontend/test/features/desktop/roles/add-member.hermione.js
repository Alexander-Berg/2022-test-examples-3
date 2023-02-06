const assert = require('assert');
const PO = require('./PO');

describe('Добавление в команду', function() {
    describe('Положительные', function() {
        it('1. Добавление нового сотрудника с предложенной ролью (бессрочное)', function() {
            return this.browser
                // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
                .openIntranetPage({ pathname: '/services/zanartestservice007' })
                .disableAnimations('*')

                .waitForVisible('.service-team .service-team-scope', 10000) // Так понимаем что команда отрисовалась

                // нажать кнопку добавления в команду
                .click('.service-team__button_role_add-users')

                // Н.У: в команде сервиса нет пользователя "user3371"
                .isExisting('.service-team .username .link[href$="/user3371"]').then(isExisting =>
                    assert.strictEqual(isExisting, false, 'В команде уже есть пользователь user3371'))

                // появилась форма добавления новых участников [add-member-form]
                .assertView('add-member-form', '.service-team-editor')

                // на форме добавления новых участников в поле добавления сотрудника
                // выбрать пользователя "user3371"
                .setValue('.service-team-editor__input_role_add .input__control', 'user3371')
                .waitForVisible('.input__popup.popup_visibility_visible .b-autocomplete-item_type_staff', 10000)
                .click('.input__popup .b-autocomplete-item_type_staff:first-child')
                .waitForVisible('.service-team-editor .service-team-editor-member')

                // рубильник срока роли находится в положении "Бессрочно"
                .getAttribute('.service-team-editor__expiration .radio-button__radio_checked_yes .radio-button__control', 'value')
                .then(value => {
                    assert.strictEqual(value, 'permanent', 'Режим запроса роли должен находить в состоянии "Бессрочно"');
                })

                // установлена роль по умолчанию "DevOps"
                .getAttribute('.service-team-editor__members .service-team-editor-member__role .input__control', 'value')
                .then(value => {
                    assert.strictEqual(value, 'DevOps', 'Роль по-умолчанию должна быть "DevOps"');
                })

                // Н.У: в команде нет блока "Запрошены роли"
                .waitForVisible('.service-team__head-spin', 10000, true) // Ждем, пока загрузятся team-statuses
                .waitForExist('.service-team__unapproved:empty', 10000)

                .setHash('add-member-form-filled')

                // нажать кнопку "Добавить"
                .click('.service-team-editor__button_role_add')

                // попап исчез
                .waitForVisible('.service-team__popup', 10000, true)

                // появилась плашка "Ожидает подтверждения" с добавленным пользователем [unapproved-user]
                .waitForExist('.service-team__unapproved .username .link[href$="/user3371"]', 10000)
                .assertView('unapproved-user', '.service-team__unapproved', {
                    ignoreElements: [
                        '.m-userpic',
                    ],
                });
        });

        it('2. Добавление нового сотрудника с предложенной ролью (до даты)', function() {
            return this.browser
                // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
                .openIntranetPage({ pathname: '/services/zanartestservice007' }, { seq: true })
                .disableAnimations('*')

                .waitForVisible('.service-team .service-team-scope', 10000) // Так понимаем что команда отрисовалась

                // нажать кнопку добавления в команду
                .click('.service-team__button_role_add-users')

                // Н.У: в команде сервиса нет пользователя "user3371"
                .isExisting('.service-team .username .link[href$="/user3371"]').then(isExisting =>
                    assert.strictEqual(isExisting, false, 'В команде уже есть пользователь user3371'))

                // на форме добавления новых участников в поле добавления сотрудника
                // выбрать пользователя "user3371"
                .setValue('.service-team-editor__input_role_add .input__control', 'user3371')
                .waitForVisible('.input__popup.popup_visibility_visible .b-autocomplete-item_type_staff', 10000)
                .click('.input__popup .b-autocomplete-item_type_staff:first-child')
                .waitForVisible('.service-team-editor .service-team-editor-member')

                // рубильник срока роли перевести в положение "до даты"
                .click('.service-team-editor__expiration .radio-button__radio .radio-button__control[value="after-date"]')

                // выбрать дату 01.07.2021
                .click('.service-team-editor__input-after-date')
                .click('.m-calendar__popup .m-datepicker__title-click')
                .setValue('.m-calendar__popup .m-datepicker__choose-year .input__control', '2021')
                .click('.m-calendar__popup .m-datepicker__choose-month .radio-button__control[value="6"]')
                .click('.m-calendar__popup .m-datepicker__chooser-submit .button')
                .click('.m-calendar__popup .m-datepicker__day[data-content="1"]')

                // установлена роль по умолчанию "DevOps"
                .getAttribute('.service-team-editor__members .service-team-editor-member__role .input__control', 'value')
                .then(value => {
                    assert.strictEqual(value, 'DevOps', 'Роль по-умолчанию должна быть "DevOps"');
                })

                // Н.У: в команде нет блока "Запрошены роли"
                .waitForVisible('.service-team__head-spin', 10000, true) // Ждем, пока загрузятся team-statuses
                .waitForExist('.service-team__unapproved:empty', 10000)

                .setHash('add-member-form-filled')

                // нажать кнопку "Добавить"
                .click('.service-team-editor__button_role_add')

                // попап исчез
                .waitForVisible('.service-team__popup', 10000, true)

                // пплашка "Ожидает подтверждения" с добавленным пользователем
                .waitForExist('.service-team__unapproved .username .link[href$="/user3371"]', 10000);
        });

        it('3. Добавление нового сотрудника с предложенной ролью (на срок)', function() {
            return this.browser
                // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
                .openIntranetPage({ pathname: '/services/zanartestservice007' }, { seq: true })
                .disableAnimations('*')

                .waitForVisible('.service-team .service-team-scope', 10000) // Так понимаем что команда отрисовалась

                // нажать кнопку добавления в команду
                .click('.service-team__button_role_add-users')

                // Н.У: в команде сервиса нет пользователя "user3371"
                .isExisting('.service-team .username .link[href$="/user3371"]').then(isExisting =>
                    assert.strictEqual(isExisting, false, 'В команде уже есть пользователь user3371'))

                // на форме добавления новых участников в поле добавления сотрудника
                // выбрать пользователя "user3371"
                .setValue('.service-team-editor__input_role_add .input__control', 'user3371')
                .waitForVisible('.input__popup.popup_visibility_visible .b-autocomplete-item_type_staff', 10000)
                .click('.input__popup .b-autocomplete-item_type_staff:first-child')
                .waitForVisible('.service-team-editor .service-team-editor-member')

                // рубильник срока роли перевести в положение "до даты"
                .click('.service-team-editor__expiration .radio-button__radio .radio-button__control[value="after-days"]')

                // в поле ввода дней вписать "7"
                .setValue('.service-team-editor__input-after-days .input__control', '7')

                // установлена роль по умолчанию "DevOps"
                .getAttribute('.service-team-editor__members .service-team-editor-member__role .input__control', 'value')
                .then(value => {
                    assert.strictEqual(value, 'DevOps', 'Роль по-умолчанию должна быть "DevOps"');
                })

                // Н.У: в команде нет блока "Запрошены роли"
                .waitForVisible('.service-team__head-spin', 10000, true) // Ждем, пока загрузятся team-statuses
                .waitForExist('.service-team__unapproved:empty', 10000)

                .setHash('add-member-form-filled')

                // нажать кнопку "Добавить"
                .click('.service-team-editor__button_role_add')

                // попап исчез
                .waitForVisible('.service-team__popup', 10000, true)

                // плашка "Ожидает подтверждения" с добавленным пользователем
                .waitForExist('.service-team__unapproved .username .link[href$="/user3371"]', 10000);
        });

        it('4. Добавление сотрудника с ограниченной ролью', async function() {
            const userName = 'Matroskin Cat';

            // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
            await this.browser
                .openIntranetPage({ pathname: '/services/zanartestservice007' })
                .waitForVisible('.service-team .service-team-scope', 10000)
                // нажать кнопку добавления в команду
                .click(PO.addTeamMember());

            // на форме добавления новых участников в поле добавления сотрудника ввести "Matroskin Cat"
            await this.browser.setValue(PO.teamEditor.addMemberField.control(), userName);

            // появился выпадающий список вариантов, среди которых есть сотрудник "Matroskin Cat"
            await this.browser.waitForVisible(PO.visiblePopup.firstStaffItem(), 10000);
            const text = await this.browser.getText(PO.visiblePopup.firstStaffItem.username());
            assert.strictEqual(text, userName);

            // кликнуть на сотрудника "Matroskin Cat"
            await this.browser.click(PO.visiblePopup.firstStaffItem());
            await this.browser.waitForVisible(PO.teamEditor.firstMemberEditor());

            // сотрудник выбран, появился в блоке "Роль"
            const addedMember = await this.browser.getText(PO.teamEditor.firstMemberEditor.person.username());
            assert.strictEqual(addedMember, userName);

            // в блоке "Роли" появилась заметка с ворнингом про ограниченные роли
            await this.browser.waitForVisible(PO.teamEditor.rolesWarning(), 1000);
            // рядом с пользователем есть оранжевая иконка "Р2"
            await this.browser.waitForVisible(PO.teamEditor.accessRoleIcon(), 500);

            // рубильник срока роли находится в положении "Бессрочно",
            const checkedExpiration = await this.browser.getValue(PO.teamEditor.expiration.checkedOption.control());
            assert.strictEqual(checkedExpiration, 'permanent');

            // по умолчанию в поле "Роль" указано "DevOps"
            const preselectedRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(preselectedRole, 'DevOps');

            // удалить значение из поля "Роль"
            await this.browser.click(PO.teamEditor.firstMemberEditor.role.clear());

            // поле очистилось
            const clearedRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(clearedRole, '');

            // в нём появился плейсхолдер "Выберите роль"
            const placeholder = await this.browser.getAttribute(PO.teamEditor.firstMemberEditor.role.control(), 'placeholder');
            assert.strictEqual(placeholder, 'Выберите роль');

            // под полем появился выпадающий список с ролями
            await this.browser.waitForVisible(PO.visiblePopup.roleItem(), 10000);

            // выбрать из выпадающего списка ролей роль "Аналитик"
            await this.browser.click(PO.visiblePopup.analystRole());

            // в поле с названием роли написано "Аналитик"
            const createdRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(createdRole, 'Аналитик');

            // форма добавления в команду сотрудника с ограниченной ролью
            await this.browser.assertView('add-user-P2', PO.teamEditor());

            // навести курсор на иконку "Р2"
            await this.browser.moveToObject(PO.teamEditor.accessRoleIcon());
            // появилась подсказка
            await this.browser.waitForVisible(PO.visiblePopup2(), 1000);
            const popupContent = await this.browser.getText(PO.visiblePopup2.content());
            assert.strictEqual(popupContent, 'У сотрудника «Ограниченная роль». Если его добавить, он увидит описание, состав' +
                ' команд и графики дежурств ВСЕХ сервисов.');

            // чтобы список участников обновился (запрос по тому же урлу, меняем хеш дампа)
            await this.browser.setHash('user-added-P2');

            // нажать кнопку "Добавить"
            await this.browser.click(PO.teamEditor.submit());

            // попап исчез
            await this.browser.waitForVisible(PO.team.popup(), 10000, true);

            // появилась плашка "Запрошены роли" с добавленным пользователем "Кот Матроскин"
            await this.browser.waitForExist(PO.team.unapproved.matroskinUser(), 10000);
        });

        it('5. Добавление сотрудника с сильно ограниченной ролью', async function() {
            const userName = 'Bayun Cat';

            // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
            await this.browser
                .openIntranetPage({ pathname: '/services/zanartestservice007' })
                .waitForVisible('.service-team .service-team-scope', 10000)
                // нажать кнопку добавления в команду
                .click(PO.addTeamMember());

            // на форме добавления новых участников в поле добавления сотрудника ввести "Bayun Cat"
            await this.browser.setValue(PO.teamEditor.addMemberField.control(), userName);

            // появился выпадающий список вариантов, среди которых есть сотрудник "Bayun Cat"
            await this.browser.waitForVisible(PO.visiblePopup.firstStaffItem(), 10000);
            const text = await this.browser.getText(PO.visiblePopup.firstStaffItem.username());
            assert.strictEqual(text, userName);

            // кликнуть на сотрудника "Bayun Cat"
            await this.browser.click(PO.visiblePopup.firstStaffItem());
            await this.browser.waitForVisible(PO.teamEditor.firstMemberEditor());

            // сотрудник выбран, появился в блоке "Роль"
            const addedMember = await this.browser.getText(PO.teamEditor.firstMemberEditor.person.username());
            assert.strictEqual(addedMember, userName);

            // в блоке "Роли" появилась заметка с ворнингом про ограниченные роли
            await this.browser.waitForVisible(PO.teamEditor.rolesWarning(), 1000);
            // рядом с пользователем есть оранжевая иконка "Р3"
            await this.browser.waitForVisible(PO.teamEditor.accessRoleIcon(), 500);

            // рубильник срока роли находится в положении "Бессрочно",
            const checkedExpiration = await this.browser.getValue(PO.teamEditor.expiration.checkedOption.control());
            assert.strictEqual(checkedExpiration, 'permanent');

            // по умолчанию в поле "Роль" указано "DevOps"
            const preselectedRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(preselectedRole, 'DevOps');

            // форма добавления в команду сотрудника с ограниченной ролью
            await this.browser.assertView('add-user-P3', PO.teamEditor());

            // кликнуть на иконку "Р3"
            await this.browser.click(PO.teamEditor.accessRoleIcon());
            // появилась подсказка
            await this.browser.waitForVisible(PO.visiblePopup2(), 1000);
            const popupContent = await this.browser.getText(PO.visiblePopup2.content());
            assert.strictEqual(popupContent, 'У сотрудника «Сильно ограниченная роль». ' +
                'Если его добавить, он увидит описание и команду этого сервиса.');

            // чтобы список участников обновился (запрос по тому же урлу, меняем хеш дампа)
            await this.browser.setHash('user-added-P3');

            // нажать кнопку "Добавить"
            await this.browser.click(PO.teamEditor.submit());

            // попап исчез
            await this.browser.waitForVisible(PO.team.popup(), 10000, true);

            // появилась плашка "Запрошены роли" с добавленным пользователем "Кот Баюн"
            await this.browser.waitForExist(PO.team.unapproved.bayunUser(), 10000);
        });

        it('6. Добавление сотрудника с заведением новой роли', async function() {
            const userName = 'Оксана Мидори';
            const currentTimestamp = '17-02-2021 03:30';
            const customRoleName = `кастомная роль - ${currentTimestamp}`;
            const customRoleNameEn = `custom role - ${currentTimestamp}`;

            // открыть страницу сервиса Мой тестовый сервис 007 (/services/zanartestservice007)
            await this.browser
                .openIntranetPage({ pathname: '/services/zanartestservice007' })
                .waitForVisible('.service-team .service-team-scope', 10000) // Так понимаем что команда отрисовалась
                // нажать кнопку добавления в команду
                .click(PO.addTeamMember());

            // - do: на форме добавления новых участников в поле добавления сотрудника ввести "Оксана Мидори"
            await this.browser.setValue(PO.teamEditor.addMemberField.control(), userName);

            // - assert: появился выпадающий список вариантов, среди которых есть сотрудник "Оксана Мидори"
            await this.browser.waitForVisible(PO.visiblePopup.firstStaffItem(), 10000);
            const text = await this.browser.getText(PO.visiblePopup.firstStaffItem.username());
            assert.strictEqual(text, userName);

            // - do: кликнуть на сотрудника "Оксана Мидори"
            await this.browser.click(PO.visiblePopup.firstStaffItem());
            await this.browser.waitForVisible(PO.teamEditor.firstMemberEditor());

            // сотрудник выбран, появился в блоке "Роль"
            const addedMember = await this.browser.getText(PO.teamEditor.firstMemberEditor.person());
            assert.strictEqual(addedMember, userName);

            // рубильник срока роли находится в положении "Бессрочно",
            const checkedExpiration = await this.browser.getValue(PO.teamEditor.expiration.checkedOption.control());
            assert.strictEqual(checkedExpiration, 'permanent');

            // установлена роль по умолчанию "Заместитель руководителя сервиса"
            const preselectedRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(preselectedRole, 'Заместитель руководителя сервиса');

            // - do: кликнуть на крестик в поле с ролью "Заместитель руководителя сервиса"
            await this.browser.click(PO.teamEditor.firstMemberEditor.role.clear());

            // поле очистилось
            const clearedRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(clearedRole, '');

            // в нём появился плейсхолдер "Выберите роль"
            const placeholder = await this.browser.getAttribute(PO.teamEditor.firstMemberEditor.role.control(), 'placeholder');
            assert.strictEqual(placeholder, 'Выберите роль');

            // под полем появился выпадающий список с ролями
            await this.browser.waitForVisible(PO.visiblePopup.roleItem(), 10000);

            // - assert: внизу списка есть пункт "+ Добавить новую роль"
            // - do: кликнуть на пункт "+ Добавить новую роль"
            await this.browser.click(PO.visiblePopup.newRoleButton());

            // - screenshot: появилась форма добавления новой роли [new-role-form]
            await this.browser.assertPopupView(PO.newRolePopup(), 'new-role-form', PO.newRolePopup());

            // - do: кликнуть в форме на кнопку "Выберите функциональную группу"
            // - assert: появился выпадающий список с функциональными группами
            // - do: кликнуть на вариант "Аналитика"
            await this.browser.setSelect2Val(PO.newRoleEditor.scope(), 'Аналитика');

            // - assert: выпадающий список скрылся
            await this.browser.waitForVisible(PO.select2Popup(), 10000, true);

            // выбран вариант "Аналитика"
            const selectText = await this.browser.getText(PO.newRoleEditor.scope.selectButton());
            assert.strictEqual(selectText, 'Аналитика');

            // - do: в поле "Русское название роли" ввести
            // "кастомная роль - (сегодняшние дата и время в формате дд-мм-гггг чч:мм)"
            await this.browser.setValue(PO.newRoleEditor.nameRu.control(), customRoleName);

            // - do: в поле "Английское название роли" ввести
            // "custom role - (сегодняшние дата и время в формате дд-мм-гггг чч:мм)"
            await this.browser.setValue(PO.newRoleEditor.nameEn.control(), customRoleNameEn);

            // - do: в поле "Код" ввести сегодняшнюю дату и время в формате "дд-мм-гггг-чч:мм"
            await this.browser.setValue(PO.newRoleEditor.code.control(), currentTimestamp);

            // - do: кликнуть на кнопку "Создать"
            await this.browser.click(PO.newRoleEditor.submit());

            // - assert: форма создания новой роли закрылась
            await this.browser.waitForVisible(PO.newRolePopup(), 10000, true);
            // в поле с названием роли написано "кастомная роль - (сегодняшние дата и время в формате дд-мм-гггг чч:мм)"
            const createdRole = await this.browser.getValue(PO.teamEditor.firstMemberEditor.role.control());
            assert.strictEqual(createdRole, customRoleName);

            // чтобы список участников обновился (запрос по тому же урлу, меняем хеш дампа)
            await this.browser.setHash('user-added');

            // - do: нажать кнопку "Добавить"
            await this.browser.click(PO.teamEditor.submit());

            // попап исчез
            await this.browser.waitForVisible(PO.team.popup(), 10000, true);

            // появилась плашка "Запрошены роли" с добавленным пользователем "Оксана Мидори"
            await this.browser.waitForExist(PO.team.unapproved.user3370(), 10000);

            // у пользователя "Оксана Мидори" запрошена роль
            // "кастомная роль - (сегодняшние дата и время в формате дд-мм-гггг чч:мм)"
            const roleText = await this.browser.getText(PO.team.unapproved.role());
            assert.strictEqual(roleText, customRoleName);
        });
    });
});
