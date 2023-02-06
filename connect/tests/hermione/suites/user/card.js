const { testUsers } = hermione.ctx;

describe('Отображение данных пользователя', () => {
    it('В /admin скрин должен соответствовать шаблонному', function() {
        /* alias: 1-admin */
        return this.browser
            // зайти в карточку пользователя в /portal/admin
            .login({ ...testUsers.alex, retpath: 'portal/admin/users/1130000001116414' })
            .waitForExist('.user-avatar_admin')
            .waitForExist('.unit__avatar.unit__avatar_complete')
            .pause(1000)

            // внешний вид [plain]
            .assertView('plain', '.user-section.card-section');
    });

    it('В /staff скрин должен соответствовать шаблонному', function() {
        /* alias: 2-staff */
        return this.browser

            // зайти в карточку пользователя в /portal/staff
            .login({ ...testUsers.alex, retpath: 'portal/staff' })
            .waitForExist('.unit__avatar.unit__avatar_complete')
            .moveToObject('.unit_type-user')
            .pause(1000)

            // внешний вид [plain]
            .assertView('plain', 'body');
    });
});
