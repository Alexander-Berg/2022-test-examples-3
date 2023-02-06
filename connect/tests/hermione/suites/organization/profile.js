const { assert } = require('chai');
const { testUsers: { alex, chuck } } = hermione.ctx;

describe('Внешний вид профиля организации', () => {
    it('Проверка отображения информации для админа', function() {
        /* alias: 1-profile-admin */
        return this.browser

            // зайти в раздел “Профиль организации” как администратор-владелец
            .login({ ...alex, retpath: '/portal/profile' })
            .waitForVisible('.org-profile_busy', 3000, true)

            // в шапке есть ссылка “Добавить логотип”
            .isExisting('.org-profile__logo-control_type_logo-picker')

            // в графе Название есть кнопка редактирования названия организации
            .isExisting('.org-profile__field_type_name .inline-editable__button_type_edit')

            // владелец организации совпадает с текущим пользователем
            .getText('.org-profile__field_type_owner .inline-editable__value')
            .then(text => {
                assert.equal(text, alex.login);
            })

            // есть блок “Контактная информация”
            .isExisting('.org-profile__title_type_contacts')
            .getText('.org-profile__title_type_contacts')
            .then(text => {
                assert.equal(text, 'Контактная информация');
            })

            // есть блок “Техническая информация”
            .isExisting('.org-profile__title_type_tech-details')
            .getText('.org-profile__title_type_tech-details')
            .then(text => {
                assert.equal(text, 'Техническая информация');
            })

            // внешний вид [plain]
            .assertView('plain', '.org-profile__content');
    });

    it('Проверка отображения информации для сотрудника', function() {
        /* alias: 2-profile-user */
        return this.browser
            // зайти в раздел “Профиль организации” как обычный сотрудник
            .login({ ...chuck, retpath: '/portal/profile' })
            .waitForVisible('.org-profile_busy', 3000, true)

            // в шапке нет ссылки “Добавить логотип”
            .isExisting('.org-profile__logo-control_type_logo-picker', true)

            // в графе Название есть кнопка редактирования названия организации
            .isExisting('.org-profile__field_type_name .inline-editable__button_type_edit')

            // владелец организации совпадает с текущим пользователем
            .getText('.org-profile__field_type_owner .inline-editable__value')
            .then(text => {
                assert.equal(text, alex.login);
            })

            // есть блок “Контактная информация”
            .isExisting('.org-profile__title_type_contacts')
            .getText('.org-profile__title_type_contacts')
            .then(text => {
                assert.equal(text, 'Контактная информация');
            })

            // есть блок “Техническая информация”
            .isExisting('.org-profile__title_type_tech-details')
            .getText('.org-profile__title_type_tech-details')
            .then(text => {
                assert.equal(text, 'Техническая информация');
            })

            // внешний вид [plain]
            .assertView('plain', '.org-profile__content');
    });
});
