const PO = require('./PO');

describe('Страница сервиса', function() {
    describe('Отрицательные', function() {
        it('1. Пользователь с ограниченной ролью не может редактировать описание сервиса', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, { user: 'robot-abc-003' });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // в правой части есть блок "Описание", под ним написано "пусто"
            await this.browser.waitForVisible(PO.serviceDescription(), 100);
            await this.browser.waitForVisible(PO.serviceDescription.emptyDescription(), 100);
            // навести курсор на заголовок "Описание"
            await this.browser.moveToObject(PO.serviceDescription.header());
            // справа от заголовка не появилась иконка карандашика
            await this.browser.assertView('service-desription-by-robot-003', PO.serviceDescription());
        });
        it('2. Пользователь с сильно ограниченной ролью не может редактировать описание сервиса', async function() {
            // открыть страницу сервиса "Автотестовый сервис"
            await this.browser.openIntranetPage({
                pathname: '/services/autotestservice2309/',
            }, { user: 'robot-abc-004' });
            // страница загрузилась
            await this.browser.waitForVisible(PO.serviceTeamScopes(), 7000);
            // в правой части есть блок "Описание", под ним написано "пусто"
            await this.browser.waitForVisible(PO.serviceDescription(), 100);
            await this.browser.waitForVisible(PO.serviceDescription.emptyDescription(), 100);
            // навести курсор на заголовок "Описание"
            await this.browser.moveToObject(PO.serviceDescription.header());
            // справа от заголовка не появилась иконка карандашика
            await this.browser.assertView('service-desription-by-robot-004', PO.serviceDescription());
        });
    });
});
