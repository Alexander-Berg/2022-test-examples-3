const { assert } = require('chai');
const { userEditTestAdmin: admin } = hermione.ctx.testUsers;

describe('Редактирование пользователя', () => {
    const PO = {};

    PO.userSection = '.user-section.section';
    PO.editDots = '.user-section.section .user-controls__edit';
    PO.updateUserSection = '.update-user-section.section';
    PO.closeEditBtn = '.update-user-section.section .section__close';
    PO.updateUserForm = '.update-user-form';
    PO.firstNameInput = '.update-user-form .input__control[name="name[first][ru]"]';
    PO.lastNameInput = '.update-user-form .input__control[name="name[last][ru]"]';
    PO.firstNameInputClear = '.update-user-form .input__control[name="name[first][ru]"] + .input__clear_visible';
    PO.lastNameInputClear = '.update-user-form .input__control[name="name[last][ru]"] + .input__clear_visible';
    PO.birthdayDayInput = '.update-user-form .date-picker__date .input__control';
    PO.birthdayMonthSelect = '.update-user-form .date-picker__month .select';
    PO.birthdayYearInput = '.update-user-form .date-picker__year .input__control';
    PO.submitBtn = '.update-user-form .button_type_submit';
    PO.formLoader = '.update-user-form .loader';
    PO.pickerItemClose = '.update-user-form .picker .picker-item__close';
    PO.pickerSuggest = '.update-user-form .picker .suggest__control';
    PO.pickerSuggestMenu = '.popup_visible .suggest-menu';
    PO.pickerSuggestItem = title => `.popup_visible .suggest-menu .suggest-item [title="${title}"]`;
    PO.addContact = '.update-user-form .add-contact';
    PO.userContacts = '.user-section .user-contacts';
    PO.userDepartment = '.user-section .user-department';

    function assertText(browser, selector, text) {
        return browser
            .getValue(selector)
            .then(val => {
                assert.equal(val, text);
            });
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

    function login(browser, retpath) {
        return browser
            .login({ ...admin, retpath })
            .disableAnimations('*');
    }

    function openEditForm(browser) {
        return login(browser, '/portal/admin/users/1130000002675430')
            .setViewportSize({ width: 1280, height: 2000 })
            .waitForVisible('.user-section')
            .click(PO.editDots)
            .waitForVisible('.popup_visible')
            .elements('.popup_visible .menu .menu-item')
            .then(rows => {
                const rowsLength = rows.value.length;

                return assert.strictEqual(rowsLength, 6,
                    `Неверное количество пунктов в меню редактирования доменного сотрудника: ${rowsLength}`);
            })
            .click('.popup_visible .menu .menu-item[data-bem*="edit_info"]')
            .waitForVisible(PO.updateUserSection);
    }

    function successEdit(browser) {
        return browser
            .click(PO.submitBtn)
            .waitForVisible('.user-section', 10000)
            .waitForVisible('.notification__success', 10000);
    }

    describe('Положительные', () => {
        beforeEach(function() {
            return openEditForm(this.browser);
        });

        it('1. Форма редактирования доменного сотрудника', function() {
            // alias: pos-1

            return this.browser
                .assertView('edit-form', '.update-user-form');
        });

        it('2. Редактирование обязательных полей в карточке доменного сотрудника', function() {
            // alias: pos-2

            const firstName = 'Фёдор';
            const lastName = 'Фёдоров';

            return this.browser
                .click(PO.lastNameInputClear)
                .click(PO.firstNameInputClear)
                .then(() => assertText(this.browser, PO.lastNameInput, ''))
                .then(() => assertText(this.browser, PO.firstNameInput, ''))
                .setValue(PO.lastNameInput, lastName)
                .setValue(PO.firstNameInput, firstName)
                .then(() => successEdit(this.browser))
                .assertView('updated-user-name', '.user-identity__name');
        });

        it('3. Редактирование необязательных полей в карточке доменного сотрудника', function() {
            // alias: pos-3

            const middleName = 'Фёдорович';
            const position = 'неРуководитель';

            return this.browser

                // в поле "Отчество" редактируем или заполняем если было пусто
                // в поле "Должность" редактируем или заполняем если было пусто
                .clearInput('.update-user-form .input__control[name="name[middle][ru]"]')
                .clearInput('.update-user-form .input__control[name="position[ru]"]')
                .setValue('.update-user-form .input__control[name="name[middle][ru]"]', middleName)
                .setValue('.update-user-form .input__control[name="position[ru]"]', position)

                // в поле "Язык" выбираем любой язык из списка
                .then(() => select(this.browser, { name: 'language' }, 'tr'))

                // в поле "Часовой пояс" выбираем любой часовой пояс из списка
                .then(() => select(this.browser, { name: 'timezone' }, 'Europe/Moscow'))

                // в поле "Дата рождения" редактируем дату, год и выбираем из списка месяц
                .setValue(PO.birthdayDayInput, 3)
                .then(() => select(this.browser, { selector: PO.birthdayMonthSelect }, '11'))
                .setValue(PO.birthdayYearInput, 2001)

                // в поле "Пол" выбираем пол (в данном случае – женский)
                .click('input[name="gender"] ~ .ui-button-group__label input[value="female"] + span')

                .then(() => successEdit(this.browser))

                // в карточке сотрудника отображаются новые значения в ранее заполненных полях
                .assertView('3-edited-user', '.user-section .section__children-wrapper');
        });

        it('4. Редактирование поля "Отдел" в карточке доменного сотрудника', function() {
            // alias: pos-4

            return this.browser

                // в поле отдел нажать на крестик рядом с "Все сотрудники"
                .click(PO.pickerItemClose)

                // ввести в поле значение "Тест"
                .setValue(PO.pickerSuggest, 'Тест')

                // появился саджест с отделом "Тест"
                .waitForVisible(PO.pickerSuggestMenu)

                // выбрать отдел "Тест" из саджеста
                .click(PO.pickerSuggestItem('Тест'))
                .waitForHidden(PO.pickerSuggestMenu)
                .waitForVisible(PO.pickerItemClose)

                .then(() => successEdit(this.browser))

                // в карточке сотрудника отображается новый отдел
                .assertView('new-department', PO.userDepartment)

                .click(PO.editDots)
                .waitForVisible('.popup_visible')
                .click('.popup_visible .menu .menu-item[data-bem*="edit_info"]')
                .waitForVisible(PO.updateUserSection)

                // в поле отдел нажать на крестик рядом с "Тест"
                .click(PO.pickerItemClose)

                // ввести в поле значение "Все сотрудники"
                .setValue(PO.pickerSuggest, 'Все сотрудники')

                // появился саджест с отделом "Все сотрудники"
                .waitForVisible(PO.pickerSuggestMenu)

                //  выбрать отдел "Все сотрудники" из саджеста
                .click(PO.pickerSuggestItem('Все сотрудники'))
                .waitForHidden(PO.pickerSuggestMenu)
                .waitForVisible(PO.pickerItemClose)

                .then(() => successEdit(this.browser))

                // в карточке сотрудника отображается новый отдел
                .assertView('new-department-2', PO.userDepartment);
        });

        it('5. Добавление и редактирование дополнительных полей по кнопке "+ Добавить" в карточке доменного сотрудника', function() {
            // alias: pos-5

            return this.browser

                // нажать на "+ Добавить" под полем "Staff"
                .click(PO.addContact)

                // выбрать значение из выпадающего списка "Телефон"
                .then(() => select(this.browser, { name: 'contacts[3][type]' }, 'phone'))

                // ввести в поле ввода номер телефона, проверки на формат ввода телефона нет
                .setValue('.update-user-form input[name="contacts[3][value]"]', '12345phone')

                .then(() => successEdit(this.browser))

                // в карточке сотрудника появилось новое поле
                .assertView('new-contacts', PO.userContacts)

                .click(PO.editDots)
                .waitForVisible('.popup_visible')
                .click('.popup_visible .menu .menu-item[data-bem*="edit_info"]')
                .waitForVisible(PO.updateUserSection)

                // ввести в поле "Телефон" новое значение
                .click('.update-user-form input[name="contacts[2][value]"] + .input__clear_visible')
                .setValue('.update-user-form input[name="contacts[2][value]"]', 'phone12345')

                .then(() => successEdit(this.browser))

                // в карточке сотрудника отображается новое значение
                .assertView('new-contacts-2', PO.userContacts);
        });
    });

    describe('Отрицательные', () => {
        it('1. Закрытие формы редактирования по нажатию на крестик', function() {
            // alias: neg-1

            return openEditForm(this.browser)
                // нажать на "крестик" в правом верхнем углу формы
                .click(PO.closeEditBtn)
                // карточка сотрудника вернулась в состояние просмотра
                .waitForVisible(PO.userSection, 5000)
                .assertView('user-section', '.user-section .section__children-wrapper');
        });

        it('2. Проверка валидации при незаполненных обязательных полях', function() {
            // alias: neg-2

            return openEditForm(this.browser)

                // нажать на элементы для очистки полей "крестик" в обязательных полях "Фамилия" и "Имя"
                .click(PO.lastNameInputClear)
                .click(PO.firstNameInputClear)

                // поля "Фамилия" и "Имя" стали пустыми
                .then(() => assertText(this.browser, PO.lastNameInput, ''))
                .then(() => assertText(this.browser, PO.firstNameInput, ''))

                // нажать кнопку "Сохранить"
                .click(PO.submitBtn)

                // скриншот формы, где отображаются сообщения около незаполненных обязательных полей
                .assertView('validation', PO.updateUserForm);
        });

        it('3. Проверка валидации даты рождения', function() {
            // alias: neg-3

            return openEditForm(this.browser)

                // изменить дату рождения на 30 февраля 1999
                .clearInput(PO.birthdayDayInput)
                .clearInput(PO.birthdayYearInput)
                .setValue(PO.birthdayDayInput, 30)
                .setValue(PO.birthdayYearInput, 1999)

                // нажать кнопку "Сохранить"
                .click(PO.submitBtn)
                .waitForHidden(PO.formLoader)

                // скриншот формы с ошибкой
                .assertView('errors', PO.updateUserForm)

                // изменить дату рождения на 15 февраля 2021
                .clearInput(PO.birthdayDayInput)
                .clearInput(PO.birthdayYearInput)
                .setValue(PO.birthdayDayInput, 15)
                .setValue(PO.birthdayYearInput, 2021)

                // нажать кнопку "Сохранить"
                .click(PO.submitBtn)

                // над кнопкой "Сохранить" появилась красная надпись "Некорректная дата."
                .waitForHidden(PO.formLoader)
                .isVisible('.update-user-form .form__error_standalone')
                .then(isVisible => assert.strictEqual(isVisible, true, 'Сообщение об ошибке не появилось'));
        });

        it('4. Отсутсвие возможности редактирования карточки портального сотрудника', function() {
            // alias: neg-4

            // открываем карточку портального сотрудника
            return login(this.browser, '/portal/admin/users/4035500652')
                // нажимаем три точки в верхнем правом углу карточки
                .click(PO.editDots)
                // появилось меню с пунктами "Сделать администратором" и "Удалить"
                .waitForVisible('.popup_visible')
                .assertView('portal-user-edit-menu', '.popup_visible .menu')
                .elements('.popup_visible .menu .menu-item')
                .then(rows => {
                    const rowsLength = rows.value.length;

                    return assert.strictEqual(rowsLength, 2,
                        `Неверное количество пунктов в меню редактирования портального пользователя: ${rowsLength}`);
                });
        });
    });
});
