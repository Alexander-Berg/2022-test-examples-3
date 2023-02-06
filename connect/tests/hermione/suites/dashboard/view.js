const assert = require('chai').assert;
const { testUsers } = hermione.ctx;

describe('Дашборд', () => {
    it('Ссылка на админку должна отображаться для внутреннего админа', function() {
        /* alias: 1-inner-admin */
        return this.browser
            // зайти в Яндекс.Коннект под админом
            .login({ ...testUsers.alex, retpath: '/portal/home' })

            .waitForVisible('.dashboard-card')
            .isExisting('.dashboard-card[data-slug="portal"]')

            // ссылка на админку существует
            .then(isAdminCardExisting => {
                assert.equal(isAdminCardExisting, true);
            })

            // внешний вид [dashboard-admin]
            .assertView('dashboard-admin', 'body');
    });

    it('Ссылка на админку не должна отображаться для обычного пользователя', function() {
        /* alias: 2-simple-user */
        return this.browser
            // зайти в Яндекс.Коннект под обычным пользователем организации
            .login({ ...testUsers.chuck, retpath: '/portal/home' })

            .waitForVisible('.dashboard-card')
            .isExisting('.dashboard-card[data-slug="portal"]')

            // ссылка на админку не существует
            .then(isAdminCardExisting => {
                assert.equal(isAdminCardExisting, false);
            })

            // внешний вид [dashboard-user]
            .assertView('dashboard-user', 'body');
    });

    it('Ссылка на админку должна отображаться для внешнего админа', function() {
        /* alias: 3-external-admin */
        return this.browser
            // зайти в Яндекс.Коннект под внешним админом
            .login({ ...testUsers.externalAdmin, retpath: '/portal/home' })

            .waitForVisible('.dashboard-card')
            .isExisting('.dashboard-card[data-slug="portal"]')

            // ссылка на админку существует
            .then(isAdminCardExisting => {
                assert.equal(isAdminCardExisting, true);
            })

            // внешний вид [dashboard-admin]
            .assertView('dashboard-admin', 'body');
    });

    it('Ссылка на админку должна отображаться для внешнего заместителя админа', function() {
        /* alias: 4-deputy-admin */
        return this.browser
            // зайти в Яндекс.Коннект под внешним заместителем админа
            .login({ ...testUsers.externalDeputyAdmin, retpath: '/portal/home' })

            .waitForVisible('.dashboard-card')
            .isExisting('.dashboard-card[data-slug="portal"]')

            // ссылка на админку не существует
            .then(isAdminCardExisting => {
                assert.equal(isAdminCardExisting, true);
            })

            // внешний вид [dashboard-user]
            .assertView('dashboard-user', 'body');
    });

    it('Должна показываться выпадушка с переключением языка', function() {
        /* alias: 5-lang-switch */
        return this.browser
            // зайти в Яндекс.Коннект под админом
            .login({ ...testUsers.alex, retpath: '/portal/home' })

            .waitForVisible('.lang-switcher')

            // нажать на переключалку языков
            .click('.lang-switcher')

            // открывается выпадушка
            .waitForVisible('.lang-switcher__lang')

            // внешний вид [dashboard-change-language]
            .assertView('dashboard-change-language', 'body');
    });
});
