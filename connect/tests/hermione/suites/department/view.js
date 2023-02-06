const assert = require('chai').assert;
const { testUsers } = hermione.ctx;
const secondDepartmentSelector = '.department-section:not(.root-department-section)';
const departmentEmailLink = '.department-contacts .list-item__value .link';

describe('Содержимое отдела', () => {
    it('В /admin скрин должен соответствовать шаблонному', function() {
        /* alias: 1-admin */
        return this.browser

            // зайти в раздел "Оргструктура" в /portal/admin
            .login({ ...testUsers.alex, retpath: 'portal/admin/departments/2' })
            .waitForExist('.unit__avatar.unit__avatar_complete')
            .getAttribute(`${secondDepartmentSelector} ${departmentEmailLink}`, 'href')
            .then(link => {
                assert.equal(link, 'https://mail.yandex.ru/compose?to=best%40ui-test.yaconnect.com');
            })
            .pause(1000)

            // внешний вид [plain]
            .assertView('plain', '.app__content');
    });

    it('В /staff скрин должен соответствовать шаблонному', function() {
        /* alias: 2-staff */
        return this.browser

            // зайти в раздел "Оргструктура" в /portal/staff
            .login({ ...testUsers.alex, retpath: 'portal/staff/departments/2' })
            .waitForExist('.unit__avatar.unit__avatar_complete')
            .getAttribute(`${secondDepartmentSelector} ${departmentEmailLink}`, 'href')
            .then(link => {
                assert.equal(link, 'https://mail.yandex.ru/compose?to=best%40ui-test.yaconnect.com');
            })
            .pause(1000)

            // внешний вид [plain]
            .assertView('plain', 'body');
    });
});
