const { testUsers: { multi, noOrgsUser } } = hermione.ctx;
const { URL } = require('url');
const { assert } = require('chai');

describe('Явная смена контекста', () => {
    describe('Положительные', () => {
        it('1. Переключение контекста без переданной организации', function() {
            /* alias: pos-1-no_org */
            return this.browser
                // залогиниться пользователем, у которого больше одной организации
                // перейти на страницу "{{CONNECT_HOST}}/portal/context?silent=0&retpath=%2Fportal%2Fdownloads"
                .login({ ...multi, retpath: '/portal/context?silent=0&retpath=%2Fportal%2Fdownloads' })
                .disableAnimations('*')

                // внешний вид страницы [plain]
                .assertView('plain', '.context__container')

                // нажать на переключатель организаций
                .click('.context__select')

                // открылся выпадающий список организаций
                .waitForVisible('.popup2_visible_yes .menu', 3000)

                // выпадающий список организаций [dropdown]
                .assertView('dropdown', '.popup2_visible_yes')

                // выбрать из списка не текущую организацию
                .click('.popup2_visible_yes .menu .menu__item:nth-child(2)')
                .getAttribute('.context__select .control', 'id')
                .then(text => {
                    this.browser.setMeta('orgId', text);
                })
                // нажать на кнопку "Перейти"
                .waitForHidden('.context__select-spin')
                .click('.context__accept')

                // выполнился переход на страницу "Скачайте приложения" {{CONNECT_HOST}}/portal/downloads
                // организация сменилась на выбранную
                .waitForVisible('.context', 10000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');
                    const orgId = parsedUrl.searchParams.get('org_id');

                    assert.equal(parsedUrl.pathname, '/portal/set/any/');
                    assert.equal(new URL(retpath).pathname, '/portal/downloads');

                    return this.browser.getMeta('orgId').then(text => {
                        assert.equal(orgId, text);
                    });
                });
        });

        it('2. Отображение страницы c сервисом Трекер', function() {
            /* alias: pos-2-tracker */
            return this.browser

                // залогиниться пользователем, у которого больше одной организации
                // перейти на страницу "{{CONNECT_HOST}}/portal/context?silent=0&source=tracker"
                .login({ ...multi, retpath: '/portal/context?silent=0&source=tracker' })

                // внешний вид страницы [plain]
                .assertView('plain', '.context__container');
        });

        it('3. Переключение контекста с переданной организацией', function() {
            /* alias: pos-3-org */
            return this.browser

                // залогиниться внешним админом, у которого больше одной организации
                // перейти на страницу "{{CONNECT_HOST}}/portal/context?silent=0&org_id={id организации пользователя}"
                .login({ ...multi, retpath: '/portal/context?silent=0&org_id=104252' })
                .disableAnimations('*')

                // внешний вид страницы [plain]
                .assertView('plain', '.context__container')

                // нажать на кнопку "Перейти"
                .click('.context__accept')

                // выполнился переход на страницу "Дашборда" {{CONNECT_HOST}}/portal/home
                // организация сменилась на выбранную
                .waitForVisible('.context', 10000, true)
                .getUrl()
                .then(url => {
                    const parsedUrl = new URL(url);
                    const retpath = parsedUrl.searchParams.get('retpath');
                    const orgId = parsedUrl.searchParams.get('org_id');

                    assert.equal(parsedUrl.pathname, '/portal/set/any/');
                    assert.equal(new URL(retpath).pathname, '/portal/home');
                    assert.equal(104252, orgId);
                });
        });

        it('4. Отображение страницы для пользователя без организаций', function() {
            /* alias: pos-4-no-orgs */
            return this.browser

                // залогиниться пользователем, у которого нет организаций
                // перейти на страницу "{{CONNECT_HOST}}/portal/context?silent=0"
                .login({ ...noOrgsUser, retpath: '/portal/context?silent=0' })

                // внешний вид страницы [plain]
                .assertView('plain', '.context__container');
        });
    });
});
