const { URL } = require('url');
const { assert } = require('chai');
const { getContext } = require('@yandex-int/infratest-utils/lib/hermione-get-context');
const createDataPath = require('../../helpers/createDataPath');
const { testUsers: { alex, noOrgsUser } } = hermione.ctx;

describe('Лэндинг – редиректы', () => {
    it('1. Пользователя с организацими должно редиректить на дашборд', function() {
        /* alias: 1-with-orgs */
        return this.browser
            // зайти в Яндекс.Коннект под пользователем с организациями
            .login({ ...alex, retpath: '/' })

            // должно редиректить на дашборд
            .getUrl()
            .then(url => {
                assert.equal(new URL(url).pathname, '/portal/home');
            })
            .waitForVisible('.dashboard-card');
    });

    it('2. Пользователя без организаций должно редиректить в почту', function() {
        /* alias: 2-without-orgs */
        return this.browser
            // зайти в Яндекс.Коннект под пользователем без организаций
            .login({ ...noOrgsUser, retpath: '/', waitElement: 'body' })

            // должно редиректить в почту
            .getUrl()
            .then(url => {
                assert.notEqual(new URL(url).pathname, '/portal/home');
            });
    });

    it('3. Без авторизации должно редиректить в почту', function() {
        const testContext = getContext(this.browser.executionContext);
        const cachePath = createDataPath('cache', testContext);

        /* alias: 3-without-auth */
        return this.browser
            // подчищаем куки и обновляем путь для кэша
            // иначе возможна остаточная авторизация от других тестов
            .deleteCookie()
            .url('/ping')
            .waitForVisible('body', 5000)
            .setHash('')
            .setCookie({
                name: 'dumps-test-path',
                value: cachePath,
            })

            // зайти в Яндекс.Коннект без авторизации
            .url('/')
            .waitForVisible('body', 5000)

            // должно редиректить в почту
            .getUrl()
            .then(url => {
                assert.notEqual(new URL(url).pathname, '/portal/home');
            });
    });
});
